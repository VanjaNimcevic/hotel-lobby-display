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
 * Shows one WEB_PAGE item full screen for a fixed duration. The {@link WebView}
 * is created in {@link #play} and destroyed in {@link #release} - never kept in
 * the XML layout, so it cannot leak the Activity.
 */
public class WebRenderer {

    public interface Listener {
        void onFinished();

        void onError(String message);
    }

    private static final String TAG = "WebRenderer";
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
                // Only fail on the main page - ignore broken sub-resources (ads, images).
                if (request.isForMainFrame()) {
                    Log.e(TAG, "WebView error for " + url + ": " + error.getDescription());
                    notifyError(String.valueOf(error.getDescription()));
                }
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

    /** Destroys the WebView and cancels the timer. Safe to call anytime. */
    public void release() {
        cancelPending();
        if (webView != null) {
            container.removeView(webView);
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
    }

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
