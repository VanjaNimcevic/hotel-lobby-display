package com.vanja.hotellobbydisplay.player;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Shows one TEXT or BANNER item on a {@link TextView} for a fixed duration.
 * Text cannot fail to load, so the listener only reports {@code onFinished()}.
 */
public class TextRenderer {

    public interface Listener {
        void onFinished();
    }

    private static final String TAG = "TextRenderer";
    private static final int DEFAULT_DURATION_SEC = 8;
    /** Smaller zoom than the image - movement on sharp text is more noticeable. */
    private static final float ZOOM_END_SCALE = 1.04f;

    private final TextView textView;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private Listener listener;
    private Runnable pendingDurationRunnable;

    public TextRenderer(TextView textView) {
        this.textView = textView;
    }

    public void play(String text, int durationSec, String bannerPosition, Listener listener) {
        this.listener = listener;
        // Only the timer here, NOT animate().cancel() - the caller may have just
        // started a fade-in on this same view.
        cancelPendingTimerOnly();

        textView.setText(text != null ? text : "");
        applyPosition(bannerPosition);

        int seconds = durationSec > 0 ? durationSec : DEFAULT_DURATION_SEC;
        Log.i(TAG, "Showing text for " + seconds + "s (position=" + bannerPosition + ")");
        pendingDurationRunnable = this::notifyFinished;
        uiHandler.postDelayed(pendingDurationRunnable, seconds * 1000L);
        startZoom(seconds);
    }

    private void startZoom(int seconds) {
        textView.animate()
                .scaleX(ZOOM_END_SCALE)
                .scaleY(ZOOM_END_SCALE)
                .setDuration(seconds * 1000L)
                .setInterpolator(new LinearInterpolator())
                .start();
    }

    /** Cancels the timer and the zoom, and resets scale. Safe to call anytime. */
    public void cancelPending() {
        cancelPendingTimerOnly();
        textView.animate().cancel();
        textView.setScaleX(1f);
        textView.setScaleY(1f);
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

    private void applyPosition(String bannerPosition) {
        int gravity;
        if ("top".equals(bannerPosition)) {
            gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        } else if ("bottom".equals(bannerPosition)) {
            gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        } else {
            gravity = Gravity.CENTER; // "center", null or anything unrecognized
        }
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) textView.getLayoutParams();
        params.gravity = gravity;
        textView.setLayoutParams(params);
    }
}
