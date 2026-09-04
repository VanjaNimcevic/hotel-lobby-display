package com.vanja.hotellobbydisplay;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.media3.ui.PlayerView;

import com.vanja.hotellobbydisplay.data.PlaylistRepository;
import com.vanja.hotellobbydisplay.data.local.PlaylistItemEntity;
import com.vanja.hotellobbydisplay.player.ImageRenderer;
import com.vanja.hotellobbydisplay.player.TextRenderer;
import com.vanja.hotellobbydisplay.player.VideoRenderer;
import com.vanja.hotellobbydisplay.player.WebRenderer;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";
    private static final String TYPE_VIDEO = "VIDEO";
    private static final String TYPE_IMAGE = "IMAGE";
    private static final String TYPE_TEXT = "TEXT";
    private static final String TYPE_BANNER = "BANNER";
    private static final String TYPE_WEB_PAGE = "WEB_PAGE";

    private PlaylistRepository playlistRepository;
    private VideoRenderer videoRenderer;
    private ImageRenderer imageRenderer;
    private TextRenderer textRenderer;
    private WebRenderer webRenderer;

    // Temporary driver (APV-15 + APV-16 + APV-17 + APV-18): plays VIDEO, IMAGE,
    // TEXT, BANNER and WEB_PAGE items in a loop; other types are skipped for
    // now. Replaced by TimelineScheduler (APV-19) + PlaybackController (APV-20).
    private static final long RETRY_DELAY_MS = 3_000L;

    /** Extra: simple fade-in when a new item's view appears, instead of a hard cut. */
    private static final long TRANSITION_MS = 400L;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final List<PlaylistItemEntity> playbackItems = new ArrayList<>();
    private int currentItemIndex = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideSystemUI();

        PlayerView videoView = findViewById(R.id.video_view);
        videoRenderer = new VideoRenderer(this, videoView);

        ImageView imageView = findViewById(R.id.image_view);
        imageRenderer = new ImageRenderer(this, imageView);

        TextView textView = findViewById(R.id.text_view);
        textRenderer = new TextRenderer(textView);

        ViewGroup webContainer = findViewById(R.id.web_container);
        webRenderer = new WebRenderer(this, webContainer);

        playlistRepository = PlaylistRepository.getInstance(this);
        loadPlaylist();
    }

    /**
     * APV-13: ask the repository for the playlist. The repository handles assets,
     * parsing and Room storage on a background thread and calls back on the main
     * thread. The Activity never touches the parser, assets or DAOs directly.
     */
    private void loadPlaylist() {
        playlistRepository.loadInitialPlaylist(new PlaylistRepository.Callback() {
            @Override
            public void onPlaylistReady(List<PlaylistItemEntity> enabledItems) {
                Log.i(TAG, "Playlist ready: " + enabledItems.size() + " enabled items");

                playbackItems.clear();
                for (PlaylistItemEntity item : enabledItems) {
                    if (hasPlayableContent(item)) {
                        playbackItems.add(item);
                    }
                }
                Log.i(TAG, "Playable items (VIDEO/IMAGE/TEXT/BANNER/WEB_PAGE for now): "
                        + playbackItems.size());
                currentItemIndex = 0;
                playCurrentItem();
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Could not load playlist: " + message);
            }
        });
    }

    /**
     * VIDEO/IMAGE/WEB_PAGE need a url; TEXT/BANNER need text. Anything else is
     * not renderable yet.
     */
    private boolean hasPlayableContent(PlaylistItemEntity item) {
        String type = item.getType();
        if (TYPE_VIDEO.equals(type) || TYPE_IMAGE.equals(type) || TYPE_WEB_PAGE.equals(type)) {
            return item.getUrl() != null;
        }
        if (TYPE_TEXT.equals(type) || TYPE_BANNER.equals(type)) {
            return item.getText() != null;
        }
        return false;
    }

    /**
     * Temporary driver: show the current item with the matching renderer, then
     * advance on finish/error. Only one renderer view is ever visible - every
     * other renderer is stopped first.
     */
    private void playCurrentItem() {
        if (playbackItems.isEmpty()) {
            Log.i(TAG, "No playable items");
            return;
        }

        videoRenderer.release();
        imageRenderer.cancelPending();
        textRenderer.cancelPending();
        webRenderer.release();

        View videoView = findViewById(R.id.video_view);
        View imageView = findViewById(R.id.image_view);
        View textView = findViewById(R.id.text_view);
        View webContainer = findViewById(R.id.web_container);
        videoView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        textView.setVisibility(View.GONE);
        webContainer.setVisibility(View.GONE);

        PlaylistItemEntity item = playbackItems.get(currentItemIndex);
        Log.i(TAG, "ITEM " + (currentItemIndex + 1) + "/" + playbackItems.size()
                + " -> " + item.getId() + " (" + item.getType() + ")");

        switch (item.getType()) {
            case TYPE_VIDEO:
                fadeIn(videoView);
                videoRenderer.play(item.getUrl(), new VideoRenderer.Listener() {
                    @Override
                    public void onFinished() {
                        advanceToNextItem(0L);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Skipping VIDEO " + item.getId() + " after error: " + message);
                        advanceToNextItem(RETRY_DELAY_MS);
                    }
                });
                break;

            case TYPE_IMAGE:
                fadeIn(imageView);
                imageRenderer.play(item.getUrl(), item.getDurationSec(), item.getMetadataScaleType(),
                        new ImageRenderer.Listener() {
                            @Override
                            public void onFinished() {
                                advanceToNextItem(0L);
                            }

                            @Override
                            public void onError(String message) {
                                Log.e(TAG, "Skipping IMAGE " + item.getId()
                                        + " after error: " + message);
                                advanceToNextItem(RETRY_DELAY_MS);
                            }
                        });
                break;

            case TYPE_TEXT:
            case TYPE_BANNER:
                fadeIn(textView);
                textRenderer.play(item.getText(), item.getDurationSec(),
                        item.getMetadataBannerPosition(), () -> advanceToNextItem(0L));
                break;

            case TYPE_WEB_PAGE:
                fadeIn(webContainer);
                webRenderer.play(item.getUrl(), item.getDurationSec(),
                        item.isMetadataJavascriptEnabled(), new WebRenderer.Listener() {
                            @Override
                            public void onFinished() {
                                advanceToNextItem(0L);
                            }

                            @Override
                            public void onError(String message) {
                                Log.e(TAG, "Skipping WEB_PAGE " + item.getId()
                                        + " after error: " + message);
                                advanceToNextItem(RETRY_DELAY_MS);
                            }
                        });
                break;

            default:
                // LAYOUT (APV-30 bonus) - not renderable yet.
                Log.i(TAG, "Skipping unsupported type for now: " + item.getType());
                advanceToNextItem(0L);
        }
    }

    /**
     * Extra (not required by any task): a very simple transition between
     * playlist items - fade the new view in instead of a hard cut. The view
     * that is hiding is simply set to GONE (no fade-out), which keeps this
     * cheap and avoids two renderers being visible mid-transition.
     */
    private void fadeIn(View view) {
        view.animate().cancel();
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate().alpha(1f).setDuration(TRANSITION_MS).start();
    }

    private void advanceToNextItem(long delayMs) {
        uiHandler.postDelayed(() -> {
            currentItemIndex = (currentItemIndex + 1) % playbackItems.size();
            playCurrentItem();
        }, delayMs);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Returning from the background: re-create the renderer and resume the loop.
        if (!playbackItems.isEmpty()) {
            playCurrentItem();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop every renderer and drop any pending "next item" callback while
        // the app is not visible (APV-15/16/17/18 lifecycle rule).
        uiHandler.removeCallbacksAndMessages(null);
        if (videoRenderer != null) {
            videoRenderer.release();
        }
        if (imageRenderer != null) {
            imageRenderer.cancelPending();
        }
        if (textRenderer != null) {
            textRenderer.cancelPending();
        }
        if (webRenderer != null) {
            webRenderer.release();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
