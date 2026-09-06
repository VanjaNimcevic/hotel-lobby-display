package com.vanja.hotellobbydisplay.data.local;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

/** {@code media_cache} table. Blocking - call off the main thread. */
@Dao
public interface MediaCacheDao {

    @Upsert
    void upsert(MediaCacheEntity entry);

    @Query("SELECT * FROM media_cache WHERE sourceUrl = :sourceUrl LIMIT 1")
    MediaCacheEntity getByUrl(String sourceUrl);

    @Query("SELECT * FROM media_cache")
    List<MediaCacheEntity> getAll();

    @Query("UPDATE media_cache SET localFilePath = :localFilePath, status = :status, "
            + "updatedAt = :updatedAt WHERE sourceUrl = :sourceUrl")
    void updatePathAndStatus(String sourceUrl, String localFilePath, String status, long updatedAt);

    @Query("DELETE FROM media_cache WHERE sourceUrl = :sourceUrl")
    void deleteByUrl(String sourceUrl);
}
