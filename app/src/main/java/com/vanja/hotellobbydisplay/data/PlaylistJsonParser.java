package com.vanja.hotellobbydisplay.data;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vanja.hotellobbydisplay.model.PlaylistModel;

/**
 * Parses playlist JSON with Gson. Never throws: on invalid JSON or a missing
 * required field it logs and returns null.
 */
public class PlaylistJsonParser {

    private static final String TAG = "PlaylistJsonParser";

    private final Gson gson = new Gson();

    /** @return the parsed playlist, or null if the text is invalid or missing playlistId/version/items. */
    public PlaylistModel parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            Log.e(TAG, "Playlist JSON is null or empty");
            return null;
        }

        PlaylistModel playlist;
        try {
            playlist = gson.fromJson(json, PlaylistModel.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Playlist JSON is not valid JSON", e);
            return null;
        }
        if (playlist == null) {
            Log.e(TAG, "Playlist JSON parsed to null");
            return null;
        }

        if (playlist.getPlaylistId() == null || playlist.getPlaylistId().trim().isEmpty()) {
            Log.e(TAG, "Playlist JSON is missing 'playlistId'");
            return null;
        }
        if (playlist.getVersion() < 1) {
            Log.e(TAG, "Playlist JSON has missing or invalid 'version': " + playlist.getVersion());
            return null;
        }
        if (playlist.getItems() == null || playlist.getItems().isEmpty()) {
            Log.e(TAG, "Playlist JSON has no 'items'");
            return null;
        }

        Log.i(TAG, "Parsed playlist " + playlist);
        return playlist;
    }
}
