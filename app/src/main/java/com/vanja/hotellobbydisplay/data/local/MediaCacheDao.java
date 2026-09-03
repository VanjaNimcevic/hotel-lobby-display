package com.vanja.hotellobbydisplay.data.local;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

/**
 * Reads and writes the {@code media_cache} table.
 *
 * <p>Synchronous - call off the main thread (see {@link PlaylistDao}).</p>
 */
@Dao
public interface MediaCacheDao {

    /** Insert the cache entry, or replace the existing row with the same sourceUrl. */
    @Upsert
    void upsert(MediaCacheEntity entry);

    /** @return the cache entry for a remote URL, or {@code null} if it was never seen. */
    @Query("SELECT * FROM media_cache WHERE sourceUrl = :sourceUrl LIMIT 1")
    MediaCacheEntity getByUrl(String sourceUrl);

    /** All cache entries (used for cleanup of old files, APV-22). */
    @Query("SELECT * FROM media_cache")
    List<MediaCacheEntity> getAll();

    /** Update the downloaded file path and status for one cached URL. */
    @Query("UPDATE media_cache SET localFilePath = :localFilePath, status = :status, "
            + "updatedAt = :updatedAt WHERE sourceUrl = :sourceUrl")
    void updatePathAndStatus(String sourceUrl, String localFilePath, String status, long updatedAt);
}
