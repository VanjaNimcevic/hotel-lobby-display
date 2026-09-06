package com.vanja.hotellobbydisplay.data.local;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

/** {@code playlist_items} table. Blocking - call off the main thread. */
@Dao
public interface PlaylistItemDao {

    @Upsert
    void upsertAll(List<PlaylistItemEntity> items);

    /** Enabled items, higher priority first then smaller orderIndex. */
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId AND enabled = 1 "
            + "ORDER BY priority DESC, orderIndex ASC")
    List<PlaylistItemEntity> getEnabledItems(String playlistId);

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    List<PlaylistItemEntity> getAllItems(String playlistId);

    /** Remove every item of a playlist, before saving a fresh copy. */
    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    void deleteForPlaylist(String playlistId);
}
