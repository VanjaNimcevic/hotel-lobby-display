package com.vanja.hotellobbydisplay.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room table row tracking one downloaded media file ("media_cache" table).
 *
 * <p>Keyed by the remote {@code sourceUrl}: given a playlist item's URL, the app
 * looks here for a local copy (APV-22 / APV-24). Filled in by the download
 * logic (APV-23).</p>
 */
@Entity(tableName = "media_cache")
public class MediaCacheEntity {

    /** Remote URL of the media file. Primary key. */
    @NonNull
    @PrimaryKey
    private String sourceUrl = "";

    /** Absolute path of the downloaded file in app private storage, or null. */
    private String localFilePath;

    /** PENDING, DOWNLOADING, COMPLETED or FAILED. */
    private String status;

    /** Size of the downloaded file in bytes (0 until downloaded). */
    private long fileSizeBytes;

    /** Epoch millis of the last status change. */
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
