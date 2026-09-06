package com.vanja.hotellobbydisplay.playback;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.media3.ui.PlayerView;

import com.vanja.hotellobbydisplay.data.MediaCacheManager;
import com.vanja.hotellobbydisplay.data.PlaybackLogger;
import com.vanja.hotellobbydisplay.data.local.PlaylistItemEntity;
import com.vanja.hotellobbydisplay.player.ImageRenderer;
import com.vanja.hotellobbydisplay.player.LayoutRenderer;
import com.vanja.hotellobbydisplay.player.TextRenderer;
import com.vanja.hotellobbydisplay.player.VideoRenderer;
import com.vanja.hotellobbydisplay.player.WebRenderer;
import com.vanja.hotellobbydisplay.util.NetworkMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the four renderers and asks {@link TimelineScheduler} what to show next.
 * The Activity just builds this and calls {@link #setItems}, {@link #start},
 * {@link #stop}. Each cycle: pick an item, stop every renderer, show exactly
 * one, log it, and schedule the next.
 */
public class PlaybackController {

    private static final String TAG = "PlaybackController";

    private static final String TYPE_VIDEO = "VIDEO";
    private static final String TYPE_IMAGE = "IMAGE";
    private static final String TYPE_TEXT = "TEXT";
    private static final String TYPE_BANNER = "BANNER";
    private static final String TYPE_WEB_PAGE = "WEB_PAGE";
    private static final String TYPE_LAYOUT = "LAYOUT";

    private static final long RETRY_DELAY_MS = 3_000L;
    private static final long NOTHING_ELIGIBLE_RETRY_MS = 5_000L;
    /** Gap after an offline skip, so an all-un-cached playlist does not spin. */
    private static final long OFFLINE_SKIP_DELAY_MS = 1_000L;
    /** Fade-in duration when a new item's view appears. */
    private static final long TRANSITION_MS = 400L;

    private final VideoRenderer videoRenderer;
    private final ImageRenderer imageRenderer;
    private final TextRenderer textRenderer;
    private final WebRenderer webRenderer;
    private final LayoutRenderer layoutRenderer;

    private final View videoView;
    private final View imageView;
    private final View textView;
    private final View webContainer;
    private final View layoutContainer;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final PlaybackLogger playbackLogger;
    private final MediaCacheManager mediaCacheManager;
    private final DebugOverlay debugOverlay;
    private final Context appContext;

    private TimelineScheduler scheduler;
    private boolean running;
    private String playlistSource = "-";
    /** Last network state, so a change is logged once instead of every cycle. */
    private Boolean wasOnline;

    public PlaybackController(Context context, PlayerView videoView, ImageView imageView,
            TextView textView, ViewGroup webContainer, ViewGroup layoutContainer,
            TextView debugOverlayView) {
        this.appContext = context.getApplicationContext();
        this.videoView = videoView;
        this.imageView = imageView;
        this.textView = textView;
        this.webContainer = webContainer;
        this.layoutContainer = layoutContainer;

        this.videoRenderer = new VideoRenderer(context, videoView);
        this.imageRenderer = new ImageRenderer(context, imageView);
        this.textRenderer = new TextRenderer(textView);
        this.webRenderer = new WebRenderer(context, webContainer);
        this.layoutRenderer = new LayoutRenderer(context, layoutContainer);

        this.playbackLogger = new PlaybackLogger(context);
        this.mediaCacheManager = new MediaCacheManager(context);
        this.debugOverlay = new DebugOverlay(debugOverlayView);
    }

    /** Builds a fresh scheduler from the playlist. {@code start()} waits for this. */
    public void setItems(List<PlaylistItemEntity> items, String playlistSource) {
        this.playlistSource = playlistSource;
        List<PlaylistItemEntity> usable = new ArrayList<>();
        for (PlaylistItemEntity item : items) {
            if (hasRequiredContent(item)) {
                usable.add(item);
            } else {
                Log.w(TAG, "Dropping item " + item.getId() + " (" + item.getType()
                        + "): missing required content");
            }
        }
        scheduler = new TimelineScheduler(usable);
        Log.i(TAG, "Scheduler ready with " + usable.size() + " item(s)");
    }

    public void start() {
        running = true;
        if (scheduler != null) {
            playNext(0L);
        }
    }

    /** Stops playback and releases every renderer. */
    public void stop() {
        running = false;
        uiHandler.removeCallbacksAndMessages(null);
        videoRenderer.release();
        imageRenderer.cancelPending();
        textRenderer.cancelPending();
        webRenderer.release();
        layoutRenderer.release();
    }

    private void playNext(long delayMs) {
        uiHandler.postDelayed(this::playCurrent, delayMs);
    }

    private void playCurrent() {
        if (!running || scheduler == null) {
            return;
        }

        PlaylistItemEntity item = scheduler.getNextItem();
        if (item == null) {
            Log.i(TAG, "Nothing eligible to play right now");
            playNext(NOTHING_ELIGIBLE_RETRY_MS);
            return;
        }

        stopAllRenderers();
        hideAllViews();

        String type = item.getType();
        boolean online = isOnlineLogged();

        // Prefer a downloaded local copy for VIDEO/IMAGE.
        String localPath = null;
        if (TYPE_VIDEO.equals(type) || TYPE_IMAGE.equals(type)) {
            localPath = mediaCacheManager.localPathIfAvailable(item.getUrl());
        }

        // Offline and this item needs the network -> skip it.
        if (!online && !canPlayOffline(type, localPath)) {
            Log.i(TAG, "OFFLINE - skipping item=" + item.getId() + " type=" + type);
            playNext(OFFLINE_SKIP_DELAY_MS);
            return;
        }

        String playSource = (localPath != null) ? localPath : item.getUrl();
        String sourceLabel = sourceLabelFor(type, localPath);

        Log.i(TAG, "START item=" + item.getId() + " type=" + type
                + (sourceLabel != null ? " source=" + sourceLabel : ""));
        debugOverlay.update(item.getId(), type, sourceLabel, online, playlistSource);

        switch (type) {
            case TYPE_VIDEO:
                playbackLogger.logStart(item.getId(), sourceLabel);
                fadeIn(videoView);
                videoRenderer.play(playSource, new VideoRenderer.Listener() {
                    @Override
                    public void onFinished() {
                        logFinished(item);
                        playNext(0L);
                    }

                    @Override
                    public void onError(String message) {
                        logError(item, message);
                        playNext(RETRY_DELAY_MS);
                    }
                });
                break;

            case TYPE_IMAGE:
                playbackLogger.logStart(item.getId(), sourceLabel);
                fadeIn(imageView);
                imageRenderer.play(playSource, item.getDurationSec(), item.getMetadataScaleType(),
                        new ImageRenderer.Listener() {
                            @Override
                            public void onFinished() {
                                logFinished(item);
                                playNext(0L);
                            }

                            @Override
                            public void onError(String message) {
                                logError(item, message);
                                playNext(RETRY_DELAY_MS);
                            }
                        });
                break;

            case TYPE_TEXT:
            case TYPE_BANNER:
                playbackLogger.logStart(item.getId(), null);
                fadeIn(textView);
                textRenderer.play(item.getText(), item.getDurationSec(),
                        item.getMetadataBannerPosition(), () -> {
                            logFinished(item);
                            playNext(0L);
                        });
                break;

            case TYPE_WEB_PAGE:
                playbackLogger.logStart(item.getId(), sourceLabel);
                fadeIn(webContainer);
                webRenderer.play(item.getUrl(), item.getDurationSec(),
                        item.isMetadataJavascriptEnabled(), new WebRenderer.Listener() {
                            @Override
                            public void onFinished() {
                                logFinished(item);
                                playNext(0L);
                            }

                            @Override
                            public void onError(String message) {
                                logError(item, message);
                                playNext(RETRY_DELAY_MS);
                            }
                        });
                break;

            case TYPE_LAYOUT:
                playbackLogger.logStart(item.getId(), null);
                fadeIn(layoutContainer);
                layoutRenderer.play(item.getLayoutJson(), item.getDurationSec(),
                        new LayoutRenderer.Listener() {
                            @Override
                            public void onFinished() {
                                logFinished(item);
                                playNext(0L);
                            }

                            @Override
                            public void onError(String message) {
                                logError(item, message);
                                playNext(RETRY_DELAY_MS);
                            }
                        });
                break;

            default:
                Log.i(TAG, "Skipping unsupported type: " + type);
                playNext(0L);
        }
    }

    private boolean isOnlineLogged() {
        boolean online = NetworkMonitor.isOnline(appContext);
        if (wasOnline == null || wasOnline != online) {
            Log.i(TAG, "Network is now " + (online ? "ONLINE" : "OFFLINE"));
            wasOnline = online;
        }
        return online;
    }

    private boolean canPlayOffline(String type, String localPath) {
        if (TYPE_TEXT.equals(type) || TYPE_BANNER.equals(type)) {
            return true;
        }
        if (TYPE_VIDEO.equals(type) || TYPE_IMAGE.equals(type)) {
            return localPath != null;
        }
        return false;
    }

    /** "LOCAL"/"REMOTE" for video/image, "REMOTE" for web, null for text/banner. */
    private String sourceLabelFor(String type, String localPath) {
        if (TYPE_VIDEO.equals(type) || TYPE_IMAGE.equals(type)) {
            return (localPath != null) ? "LOCAL" : "REMOTE";
        }
        if (TYPE_WEB_PAGE.equals(type)) {
            return "REMOTE";
        }
        return null;
    }

    private void logFinished(PlaylistItemEntity item) {
        Log.i(TAG, "FINISH item=" + item.getId());
        playbackLogger.logFinished(item.getId());
    }

    private void logError(PlaylistItemEntity item, String message) {
        Log.e(TAG, "ERROR item=" + item.getId() + ": " + message);
        playbackLogger.logError(item.getId(), message);
    }

    private void stopAllRenderers() {
        videoRenderer.release();
        imageRenderer.cancelPending();
        textRenderer.cancelPending();
        webRenderer.release();
        layoutRenderer.release();
    }

    private void hideAllViews() {
        videoView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        textView.setVisibility(View.GONE);
        webContainer.setVisibility(View.GONE);
        layoutContainer.setVisibility(View.GONE);
    }

    private void fadeIn(View view) {
        view.animate().cancel();
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate().alpha(1f).setDuration(TRANSITION_MS).start();
    }

    /** VIDEO/IMAGE/WEB_PAGE need a url; TEXT/BANNER need text; LAYOUT needs a layout. */
    private boolean hasRequiredContent(PlaylistItemEntity item) {
        String type = item.getType();
        if (TYPE_VIDEO.equals(type) || TYPE_IMAGE.equals(type) || TYPE_WEB_PAGE.equals(type)) {
            return item.getUrl() != null;
        }
        if (TYPE_TEXT.equals(type) || TYPE_BANNER.equals(type)) {
            return item.getText() != null;
        }
        if (TYPE_LAYOUT.equals(type)) {
            return item.getLayoutJson() != null;
        }
        return true;
    }
}
