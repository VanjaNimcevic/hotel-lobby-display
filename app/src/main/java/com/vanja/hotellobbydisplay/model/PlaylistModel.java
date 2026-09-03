package com.vanja.hotellobbydisplay.model;

import java.util.List;

/**
 * The whole playlist, parsed from the playlist JSON (see docs/playlist-format.md).
 *
 * <p>Plain data-holder class. Fields are private with public getters
 * (encapsulation). The field names match the JSON keys exactly, so Gson (APV-10)
 * fills them in by reflection with no extra configuration and no setters.</p>
 */
public class PlaylistModel {

    /** Unique id of this playlist. Required. */
    private String playlistId;

    /** Increases every time the playlist changes. Required. */
    private int version;

    /** ISO-8601 timestamp of the last change. Optional. */
    private String updatedAt;

    /** The content items, in play order. Required. */
    private List<PlaylistItemModel> items;

    public String getPlaylistId() {
        return playlistId;
    }

    public int getVersion() {
        return version;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public List<PlaylistItemModel> getItems() {
        return items;
    }

    @Override
    public String toString() {
        int count = (items == null) ? 0 : items.size();
        return "PlaylistModel{playlistId='" + playlistId + "', version=" + version
                + ", items=" + count + "}";
    }
}
