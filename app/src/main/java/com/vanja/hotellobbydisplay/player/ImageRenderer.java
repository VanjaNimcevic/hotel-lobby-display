package com.vanja.hotellobbydisplay.player;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;

/**
 * Shows one IMAGE item on an {@link ImageView} for a fixed duration (Glide loads
 * it; an image has no "end" event, so a {@link Handler} times it out).
 */
public class ImageRenderer {

    public interface Listener {
        void onFinished();

        void onError(String message);
    }

    private static final String TAG = "ImageRenderer";
    private static final int DEFAULT_DURATION_SEC = 8;
    /** Slow "Ken Burns" zoom so the still image does not feel static. */
    private static final float ZOOM_END_SCALE = 1.08f;

    private final Context appContext;
    private final ImageView imageView;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private Listener listener;
    private Runnable pendingDurationRunnable;

    public ImageRenderer(Context context, ImageView imageView) {
        this.appContext = context.getApplicationContext();
        this.imageView = imageView;
    }

    /** {@code source} is an http(s):// URL or a local file path. */
    public void play(String source, int durationSec, String scaleType, Listener listener) {
        this.listener = listener;
        // Only the timer here, NOT animate().cancel() - the caller may have just
        // started a fade-in on this same view.
        cancelPendingTimerOnly();

        imageView.setScaleType("centerCrop".equals(scaleType)
                ? ImageView.ScaleType.CENTER_CROP
                : ImageView.ScaleType.FIT_CENTER);

        Glide.with(appContext)
                .load(toGlideModel(source))
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                            Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Image failed to load: " + source, e);
                        cancelPending();
                        notifyError(e != null ? e.getMessage() : "image load failed");
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                            Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(imageView);

        int seconds = durationSec > 0 ? durationSec : DEFAULT_DURATION_SEC;
        Log.i(TAG, "Showing image for " + seconds + "s: " + source);
        pendingDurationRunnable = this::notifyFinished;
        uiHandler.postDelayed(pendingDurationRunnable, seconds * 1000L);
        startZoom(seconds);
    }

    private void startZoom(int seconds) {
        imageView.animate()
                .scaleX(ZOOM_END_SCALE)
                .scaleY(ZOOM_END_SCALE)
                .setDuration(seconds * 1000L)
                .setInterpolator(new LinearInterpolator())
                .start();
    }

    /** Cancels the timer and the zoom, and resets scale. Safe to call anytime. */
    public void cancelPending() {
        cancelPendingTimerOnly();
        imageView.animate().cancel();
        imageView.setScaleX(1f);
        imageView.setScaleY(1f);
    }

    private void cancelPendingTimerOnly() {
        if (pendingDurationRunnable != null) {
            uiHandler.removeCallbacks(pendingDurationRunnable);
            pendingDurationRunnable = null;
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

    private Object toGlideModel(String source) {
        if (source != null && (source.startsWith("http://") || source.startsWith("https://"))) {
            return source;
        }
        return new File(source);
    }
}
