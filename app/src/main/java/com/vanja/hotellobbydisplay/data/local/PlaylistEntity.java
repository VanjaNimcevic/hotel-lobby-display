package com.vanja.hotellobbydisplay.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room table row for a whole playlist ("playlists" table).
 *
 * <p>Mirrors {@link com.vanja.hotellobbydisplay.model.PlaylistModel} but only
 * with columns Room can store directly. The list of items is a separate table
 * ({@link PlaylistItemEntity}).</p>
 *
 * <p>Private fields with public getters and setters - Room uses the no-arg
 * constructor and the setters to build a row when reading from the database.</p>
 */
@Entity(tableName = "playlists")
public class PlaylistEntity {

    /** From JSON "playlistId". Primary key. */
    @PrimaryKey
    @NonNull
    private String playlistId = "";

    /** From JSON "version". */
    private int version;

    /** From JSON "updatedAt" (ISO-8601), may be null. */
    private String updatedAt;

    /** True for the single playlist currently used for playback. */
    private boolean active;

    /** Epoch millis when this playlist was saved locally (for offline / freshness checks). */
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
