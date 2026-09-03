package com.vanja.hotellobbydisplay.model;

import java.util.List;

/**
 * The whole playlist, parsed from the playlist JSON (see docs/playlist-format.md).
 *
 * <p>These are plain data-holder classes (POJOs). The field names match the JSON
 * keys exactly, so Gson (APV-10) can fill them in automatically with no extra
 * configuration.</p>
 */
public class PlaylistModel {

    /** Unique id of this playlist. Required. */
    public String playlistId;

    /** Increases every time the playlist changes. Required. */
    public int version;

    /** ISO-8601 timestamp of the last change. Optional. */
    public String updatedAt;

    /** The content items, in play order. Required. */
    public List<PlaylistItemModel> items;

    @Override
    public String toString() {
        int count = (items == null) ? 0 : items.size();
        return "PlaylistModel{playlistId='" + playlistId + "', version=" + version
                + ", items=" + count + "}";
    }
}
