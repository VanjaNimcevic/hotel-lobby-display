package com.vanja.hotellobbydisplay.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** "media_cache" table. One row per downloaded media file, keyed by its remote URL. */
@Entity(tableName = "media_cache")
public class MediaCacheEntity {

    @NonNull
    @PrimaryKey
    private String sourceUrl = "";
    private String localFilePath;
    /** PENDING, DOWNLOADING, COMPLETED or FAILED. */
    private String status;
    private long fileSizeBytes;
    private long updatedAt;

    @NonNull
    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(@NonNull String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
