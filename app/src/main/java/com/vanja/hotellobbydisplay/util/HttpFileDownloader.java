package com.vanja.hotellobbydisplay.util;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Downloads a binary file. Writes to a {@code .tmp} sibling and renames on
 * success, so a half-finished download never looks complete. Returns -1 on
 * failure. Call off the main thread.
 */
public final class HttpFileDownloader {

    private static final String TAG = "HttpFileDownloader";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int BUFFER_SIZE = 8192;

    private HttpFileDownloader() {
    }

    /** @return bytes written, or -1 if the download failed. */
    public static long download(String urlString, File destFile) {
        File tempFile = new File(destFile.getAbsolutePath() + ".tmp");
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
                return -1;
            }

            long total = 0;
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    total += read;
                }
            }

            if (destFile.exists() && !destFile.delete()) {
                Log.w(TAG, "Could not replace existing file: " + destFile.getAbsolutePath());
                tempFile.delete();
                return -1;
            }
            if (!tempFile.renameTo(destFile)) {
                Log.w(TAG, "Could not rename temp file for " + urlString);
                tempFile.delete();
                return -1;
            }

            Log.i(TAG, "Downloaded " + urlString + " (" + total + " bytes)");
            return total;

        } catch (IOException e) {
            Log.w(TAG, "Download failed for " + urlString + ": " + e);
            tempFile.delete();
            return -1;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
