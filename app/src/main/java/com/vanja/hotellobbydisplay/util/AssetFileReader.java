package com.vanja.hotellobbydisplay.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Small helper for reading text files that are bundled in {@code app/src/main/assets}.
 *
 * <p>Only depends on {@link Context} and java.io, so it can be used from anywhere
 * (repository, parser, tests) and not just from an Activity.</p>
 *
 * <p>Example:</p>
 * <pre>
 *     String json = AssetFileReader.readAssetFile(context, "json/sample_playlist.json");
 *     if (json == null) {
 *         // asset is missing or could not be read
 *     }
 * </pre>
 */
public final class AssetFileReader {

    private static final String TAG = "AssetFileReader";

    private AssetFileReader() {
        // Utility class - never instantiated.
    }

    /**
     * Reads a whole asset file into a String using UTF-8.
     *
     * @param context  any Context (application or activity context both work)
     * @param fileName path inside the assets folder, e.g. {@code "json/sample_playlist.json"}
     * @return the file content, or {@code null} if the file is missing or cannot be read
     */
    public static String readAssetFile(Context context, String fileName) {
        StringBuilder builder = new StringBuilder();

        try (InputStream inputStream = context.getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }

            Log.i(TAG, "Loaded asset file '" + fileName + "' (" + builder.length() + " chars)");
            return builder.toString();

        } catch (IOException e) {
            // Missing file, permission issue, read error - anything that goes wrong
            // lands here. We log it and return null instead of crashing the app.
            Log.e(TAG, "Could not read asset file '" + fileName + "'", e);
            return null;
        }
    }
}
