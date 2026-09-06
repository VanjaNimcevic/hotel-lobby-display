package com.vanja.hotellobbydisplay.model;

import java.util.List;

/** Whole playlist parsed from JSON. Field names match the JSON keys for Gson. */
public class PlaylistModel {

    private String playlistId;
    private int version;
    private String updatedAt;
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
