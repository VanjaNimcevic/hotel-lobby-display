package com.vanja.hotellobbydisplay.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Reads a bundled text file from {@code assets/}. Returns null on any failure. */
public final class AssetFileReader {

    private static final String TAG = "AssetFileReader";

    private AssetFileReader() {
    }

    public static String readAssetFile(Context context, String fileName) {
        StringBuilder builder = new StringBuilder();
        try (InputStream inputStream = context.getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            Log.i(TAG, "Loaded asset '" + fileName + "' (" + builder.length() + " chars)");
            return builder.toString();
        } catch (IOException e) {
            Log.e(TAG, "Could not read asset '" + fileName + "'", e);
            return null;
        }
    }
}
