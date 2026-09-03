package com.vanja.hotellobbydisplay.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Downloads a text resource over HTTP(S) with {@link HttpURLConnection}
 * (built into Android, no extra library).
 *
 * <p>Like {@link AssetFileReader}, it never throws: any problem - no internet,
 * timeout, bad URL, non-200 response - is logged and returns {@code null}.
 * Must be called off the main thread (Android forbids network on the UI thread).</p>
 */
public final class HttpTextFetcher {

    private static final String TAG = "HttpTextFetcher";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    private HttpTextFetcher() {
        // Utility class - never instantiated.
    }

    /**
     * Sends a GET request and returns the response body as UTF-8 text.
     *
     * @param urlString the full URL to fetch
     * @return the body text, or {@code null} if the request failed for any reason
     */
    public static String fetch(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "GET " + urlString + " returned HTTP " + status);
                return null;
            }

            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
            }

            Log.i(TAG, "GET " + urlString + " ok (" + builder.length() + " chars)");
            return builder.toString();

        } catch (IOException e) {
            // UnknownHostException (no internet), SocketTimeoutException,
            // MalformedURLException, etc. all land here.
            Log.w(TAG, "GET " + urlString + " failed: " + e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
