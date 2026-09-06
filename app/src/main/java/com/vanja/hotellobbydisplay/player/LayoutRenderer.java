package com.vanja.hotellobbydisplay.player;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.vanja.hotellobbydisplay.model.LayoutModel;
import com.vanja.hotellobbydisplay.model.RegionModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a LAYOUT item: splits {@code container} into equal regions (side by
 * side, or stacked when the template name contains TOP/BOTTOM/STACK) and shows
 * one VIDEO / IMAGE / TEXT per region at the same time. A timer ends the item
 * after its duration. Region videos loop and play muted.
 */
public class LayoutRenderer {

    public interface Listener {
        void onFinished();

        void onError(String message);
    }

    private static final String TAG = "LayoutRenderer";
    private static final int DEFAULT_DURATION_SEC = 15;

    private final Context appContext;
    private final ViewGroup container;
    private final Gson gson = new Gson();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final List<ExoPlayer> players = new ArrayList<>();

    private Listener listener;
    private Runnable pendingDurationRunnable;

    public LayoutRenderer(Context context, ViewGroup container) {
        this.appContext = context.getApplicationContext();
        this.container = container;
    }

    public void play(String layoutJson, int durationSec, Listener listener) {
        this.listener = listener;
        release();

        LayoutModel layout = parse(layoutJson);
        if (layout == null || layout.getRegions() == null || layout.getRegions().isEmpty()) {
            notifyError("layout has no regions");
            return;
        }

        boolean stacked = isStacked(layout.getTemplate());
        LinearLayout split = new LinearLayout(appContext);
        split.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        split.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        for (RegionModel region : layout.getRegions()) {
            split.addView(buildRegion(region, stacked));
        }
        container.addView(split);

        int seconds = durationSec > 0 ? durationSec : DEFAULT_DURATION_SEC;
        Log.i(TAG, "Showing layout '" + layout.getTemplate() + "' with "
                + layout.getRegions().size() + " region(s) for " + seconds + "s");
        pendingDurationRunnable = this::notifyFinished;
        uiHandler.postDelayed(pendingDurationRunnable, seconds * 1000L);
    }

    /** Cancels the timer, releases every region player and clears the container. */
    public void release() {
        if (pendingDurationRunnable != null) {
            uiHandler.removeCallbacks(pendingDurationRunnable);
            pendingDurationRunnable = null;
        }
        for (ExoPlayer player : players) {
            player.release();
        }
        players.clear();
        container.removeAllViews();
    }

    private View buildRegion(RegionModel region, boolean stacked) {
        String type = region.getType();
        if ("VIDEO".equals(type)) {
            return buildVideoRegion(region.getUrl(), stacked);
        }
        if ("IMAGE".equals(type)) {
            return buildImageRegion(region.getUrl(), stacked);
        }
        return buildTextRegion(region.getText(), stacked);
    }

    private View buildVideoRegion(String url, boolean stacked) {
        PlayerView view = new PlayerView(appContext);
        view.setLayoutParams(equalWeight(stacked));
        view.setUseController(false);

        ExoPlayer player = new ExoPlayer.Builder(appContext).build();
        view.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(toUri(url)));
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        player.setVolume(0f);
        player.setPlayWhenReady(true);
        player.prepare();
        players.add(player);
        return view;
    }

    private View buildImageRegion(String url, boolean stacked) {
        ImageView view = new ImageView(appContext);
        view.setLayoutParams(equalWeight(stacked));
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(appContext)
                .load(isHttp(url) ? (Object) url : new File(url))
                .into(view);
        return view;
    }

    private View buildTextRegion(String text, boolean stacked) {
        TextView view = new TextView(appContext);
        view.setLayoutParams(equalWeight(stacked));
        view.setText(text != null ? text : "");
        view.setGravity(Gravity.CENTER);
        view.setTextColor(Color.WHITE);
        view.setBackgroundColor(Color.BLACK);
        view.setTextSize(32f);
        int pad = dp(24);
        view.setPadding(pad, pad, pad, pad);
        return view;
    }

    /** width 0 + weight for a horizontal split, height 0 + weight for a stacked one. */
    private LinearLayout.LayoutParams equalWeight(boolean stacked) {
        int width = stacked ? ViewGroup.LayoutParams.MATCH_PARENT : 0;
        int height = stacked ? 0 : ViewGroup.LayoutParams.MATCH_PARENT;
        return new LinearLayout.LayoutParams(width, height, 1f);
    }

    private boolean isStacked(String template) {
        return template != null && (template.contains("TOP")
                || template.contains("BOTTOM") || template.contains("STACK"));
    }

    private LayoutModel parse(String layoutJson) {
        if (layoutJson == null) {
            return null;
        }
        try {
            return gson.fromJson(layoutJson, LayoutModel.class);
        } catch (RuntimeException e) {
            Log.e(TAG, "Bad layout JSON", e);
            return null;
        }
    }

    private void notifyFinished() {
        pendingDurationRunnable = null;
        if (listener != null) {
            listener.onFinished();
        }
    }

    private void notifyError(String message) {
        if (listener != null) {
            listener.onError(message);
        }
    }

    private boolean isHttp(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private Uri toUri(String source) {
        if (source == null) {
            return Uri.EMPTY;
        }
        return isHttp(source) ? Uri.parse(source) : Uri.fromFile(new File(source));
    }

    private int dp(int value) {
        float density = appContext.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
