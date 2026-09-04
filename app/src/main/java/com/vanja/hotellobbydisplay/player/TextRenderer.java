package com.vanja.hotellobbydisplay.player;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Shows a single TEXT or BANNER playlist item on a {@link TextView} for a
 * fixed duration.
 *
 * <p>Unlike video/image, plain text cannot fail to "load" - there is no
 * network call or file decode - so, unlike {@link VideoRenderer} and
 * {@link ImageRenderer}, the listener only reports {@code onFinished()}.</p>
 */
public class TextRenderer {

    /** Told when the duration has passed. */
    public interface Listener {
        void onFinished();
    }

    private static final String TAG = "TextRenderer";

    /** Used when the item's durationSec is missing or not positive. */
    private static final int DEFAULT_DURATION_SEC = 8;

    /**
     * Extra: same idea as {@link ImageRenderer}'s slow zoom, so text does not
     * feel static either. Smaller than the image's (1.08) - a big zoom on
     * sharp-edged letters is more distracting than on a photo.
     */
    private static final float ZOOM_END_SCALE = 1.04f;

    private final TextView textView;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private Listener listener;
    private Runnable pendingDurationRunnable;

    public TextRenderer(TextView textView) {
        this.textView = textView;
    }

    /**
     * Shows the text, then calls {@link Listener#onFinished()} after
     * {@code durationSec} seconds.
     *
     * @param text           the text to show; {@code null} is shown as empty
     * @param durationSec    how long to show it; a value {@code <= 0} falls
     *                       back to {@link #DEFAULT_DURATION_SEC}
     * @param bannerPosition "top", "bottom" or "center"/{@code null}/anything
     *                       else - unrecognized values safely fall back to
     *                       "center" (also used for plain TEXT items, which
     *                       have no position and always want the middle)
     */
    public void play(String text, int durationSec, String bannerPosition, Listener listener) {
        this.listener = listener;
        // Only drop a stale timer here - NOT the full cancelPending(), which
        // also cancels the view's animator. The caller (MainActivity) already
        // stops the previous renderer and may have just started a fade-in on
        // this same view; calling animate().cancel() here would kill that.
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

    /** Cancels the pending "duration expired" callback and the zoom, if any. Safe to call anytime. */
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
            // "center", null, or any value we don't recognize -> safe default.
            gravity = Gravity.CENTER;
        }
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) textView.getLayoutParams();
        params.gravity = gravity;
        textView.setLayoutParams(params);
    }
}
