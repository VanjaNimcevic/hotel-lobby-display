package com.vanja.hotellobbydisplay.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** "playlists" table. Items live in {@link PlaylistItemEntity}. */
@Entity(tableName = "playlists")
public class PlaylistEntity {

    @PrimaryKey
    @NonNull
    private String playlistId = "";
    private int version;
    private String updatedAt;
    /** True for the one playlist currently used for playback. */
    private boolean active;
    /** Epoch millis when saved locally. */
    private long fetchedAt;

    @NonNull
    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(@NonNull String playlistId) {
        this.playlistId = playlistId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(long fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}
