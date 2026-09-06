package com.vanja.hotellobbydisplay.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** "playback_logs" table. One row per played item: inserted on start, updated on end. */
@Entity(tableName = "playback_logs")
public class PlaybackLogEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String itemId;
    private long startedAt;
    /** 0 while still playing. */
    private long finishedAt;
    /** STARTED, COMPLETED or ERROR. */
    private String status;
    private String errorMessage;
    /** LOCAL / REMOTE for media, null for text/banner. */
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
