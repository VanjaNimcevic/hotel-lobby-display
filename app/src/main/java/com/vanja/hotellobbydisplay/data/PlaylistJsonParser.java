package com.vanja.hotellobbydisplay.data;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vanja.hotellobbydisplay.model.PlaylistModel;

/**
 * Turns raw playlist JSON text into a {@link PlaylistModel} using Gson.
 *
 * <p>The parser never throws: on invalid JSON or missing required fields it
 * logs a clear message and returns {@code null}, so the app can decide what to
 * do (e.g. fall back to a previous playlist) instead of crashing.</p>
 */
public class PlaylistJsonParser {

    private static final String TAG = "PlaylistJsonParser";

    private final Gson gson = new Gson();

    /**
     * @param json raw JSON text (for example the contents of sample_playlist.json)
     * @return the parsed playlist, or {@code null} if the text is not valid JSON
     *         or does not contain the required fields (playlistId, version, items)
     */
    public PlaylistModel parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            Log.e(TAG, "Playlist JSON is null or empty");
            return null;
        }

        PlaylistModel playlist;
        try {
            playlist = gson.fromJson(json, PlaylistModel.class);
        } catch (JsonSyntaxException e) {
            // Text is not valid JSON (missing bracket, trailing comma, ...).
            Log.e(TAG, "Playlist JSON is not valid JSON", e);
            return null;
        }

        if (playlist == null) {
            // Happens for input like "null" or "".
            Log.e(TAG, "Playlist JSON parsed to null");
            return null;
        }

        // --- required fields (see docs/playlist-format.md) ---

        if (playlist.getPlaylistId() == null || playlist.getPlaylistId().trim().isEmpty()) {
            Log.e(TAG, "Playlist JSON is missing required field 'playlistId'");
            return null;
        }
        if (playlist.getVersion() < 1) {
            Log.e(TAG, "Playlist JSON has missing or invalid 'version' (must be >= 1), was "
                    + playlist.getVersion());
            return null;
        }
        if (playlist.getItems() == null || playlist.getItems().isEmpty()) {
            Log.e(TAG, "Playlist JSON is missing required field 'items' or the list is empty");
            return null;
        }

        Log.i(TAG, "Parsed playlist " + playlist);
        return playlist;
    }
}
