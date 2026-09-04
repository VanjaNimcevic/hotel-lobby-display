package com.vanja.hotellobbydisplay.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Shows a single WEB_PAGE playlist item full screen for a fixed duration.
 *
 * <p>The {@link WebView} is created in {@link #play} and fully destroyed in
 * {@link #release} - it is never kept in the XML layout (see
 * {@code activity_main.xml}), which is how a WebView is prevented from leaking
 * the Activity.</p>
 */
public class WebRenderer {

    /** Told when the duration has passed or the page failed to load. */
    public interface Listener {
        void onFinished();

        void onError(String message);
    }

    private static final String TAG = "WebRenderer";

    /** Used when the item's durationSec is missing or not positive. */
    private static final int DEFAULT_DURATION_SEC = 15;

    private final Context appContext;
    private final ViewGroup container;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private WebView webView;
    private Listener listener;
    private Runnable pendingDurationRunnable;

    public WebRenderer(Context context, ViewGroup container) {
        this.appContext = context.getApplicationContext();
        this.container = container;
    }

    /**
     * Creates a WebView, loads the page and shows it, then calls
     * {@link Listener#onFinished()} after {@code durationSec} seconds.
     *
     * @param url               the page to load
     * @param durationSec       how long to show it; a value {@code <= 0} falls
     *                          back to {@link #DEFAULT_DURATION_SEC}
     * @param javascriptEnabled from the item's {@code metadata.javascriptEnabled}
     */
    public void play(String url, int durationSec, boolean javascriptEnabled, Listener listener) {
        this.listener = listener;
        release();

        webView = new WebView(appContext);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.getSettings().setJavaScriptEnabled(javascriptEnabled);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                    WebResourceError error) {
                if (request.isForMainFrame()) {
                    Log.e(TAG, "WebView error for " + url + ": " + error.getDescription());
                    notifyError(String.valueOf(error.getDescription()));
                }
                // Errors on sub-resources (ads, trackers, missing images, ...)
                // are ignored - the page itself still loaded.
            }
        });

        container.addView(webView);
        webView.loadUrl(url);

        int seconds = durationSec > 0 ? durationSec : DEFAULT_DURATION_SEC;
        Log.i(TAG, "Showing web page for " + seconds + "s: " + url
                + " (javascript=" + javascriptEnabled + ")");
        pendingDurationRunnable = this::notifyFinished;
        uiHandler.postDelayed(pendingDurationRunnable, seconds * 1000L);
    }

    /** Stops the page, destroys the WebView and cancels the timer. Safe to call anytime. */
    public void release() {
        cancelPending();
        if (webView != null) {
            container.removeView(webView);
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
    }

    /** Cancels the pending "duration expired" callback, if any. Safe to call anytime. */
    public void cancelPending() {
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
        cancelPending();
        if (listener != null) {
            listener.onError(message);
        }
    }
}
