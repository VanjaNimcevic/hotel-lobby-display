package com.vanja.hotellobbydisplay.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room table row for one playback event ("playback_logs" table).
 *
 * <p>Written by the playback layer (APV-21): one row when an item starts, then
 * updated when it finishes or fails.</p>
 */
@Entity(tableName = "playback_logs")
public class PlaybackLogEntity {

    /** Auto-incremented row id. Primary key. */
    @PrimaryKey(autoGenerate = true)
    private long id;

    /** {@link PlaylistItemEntity#getId()} of the item that played. */
    private String itemId;

    /** Epoch millis when the item started. */
    private long startedAt;

    /** Epoch millis when the item finished, or 0 while it is still playing. */
    private long finishedAt;

    /** STARTED, COMPLETED or ERROR. */
    private String status;

    /** Error text when status is ERROR, otherwise null. */
    private String errorMessage;

    /** "LOCAL" or "REMOTE" for media items (APV-24); null for text/banner. */
    private String source;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(long finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
