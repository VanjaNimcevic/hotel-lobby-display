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
 * Shows a single IMAGE playlist item on an {@link ImageView} for a fixed
 * duration.
 *
 * <p>Uses Glide (already a dependency from the TV project template) to load
 * the image from a remote URL or a local file. Unlike video, an image has no
 * natural "end" event, so the duration is timed with a {@link Handler}.</p>
 */
public class ImageRenderer {

    /** Told when the duration has passed or the image failed to load. */
    public interface Listener {
        void onFinished();

        void onError(String message);
    }

    private static final String TAG = "ImageRenderer";

    /** Used when the item's durationSec is missing or not positive. */
    private static final int DEFAULT_DURATION_SEC = 8;

    /** Extra: slow "Ken Burns" zoom while the image is shown, so it does not feel static. */
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

    /**
     * Loads and shows the image, then calls {@link Listener#onFinished()}
     * after {@code durationSec} seconds.
     *
     * @param source     an {@code http(s)://} URL or a local file path
     * @param durationSec how long to show the image; a value {@code <= 0}
     *                    falls back to {@link #DEFAULT_DURATION_SEC}
     * @param scaleType  "centerCrop" to fill the screen, anything else (or
     *                   {@code null}) keeps the safe default "fitCenter"
     */
    public void play(String source, int durationSec, String scaleType, Listener listener) {
        this.listener = listener;
        // Only drop a stale timer here - NOT the full cancelPending(), which
        // also cancels the view's animator. The caller (MainActivity) already
        // stops the previous renderer and may have just started a fade-in on
        // this same view; calling animate().cancel() here would kill that.
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
                        return false; // let Glide still set the drawable on the ImageView
                    }
                })
                .into(imageView);

        int seconds = durationSec > 0 ? durationSec : DEFAULT_DURATION_SEC;
        Log.i(TAG, "Showing image for " + seconds + "s: " + source);
        pendingDurationRunnable = this::notifyFinished;
        uiHandler.postDelayed(pendingDurationRunnable, seconds * 1000L);

        startZoom(seconds);
    }

    /**
     * Extra (not required by APV-16): a slow, steady zoom-in for the whole time
     * the image is shown, so a still signage image still feels alive.
     */
    private void startZoom(int seconds) {
        imageView.animate()
                .scaleX(ZOOM_END_SCALE)
                .scaleY(ZOOM_END_SCALE)
                .setDuration(seconds * 1000L)
                .setInterpolator(new LinearInterpolator())
                .start();
    }

    /** Cancels the pending "duration expired" callback and the zoom, if any. Safe to call anytime. */
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
        // Anything else is treated as a local file path.
        return new File(source);
    }
}
