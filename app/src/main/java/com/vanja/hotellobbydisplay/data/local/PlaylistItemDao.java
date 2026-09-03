package com.vanja.hotellobbydisplay.data.local;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

/**
 * Reads and writes the {@code playlist_items} table.
 *
 * <p>Synchronous - call off the main thread (see {@link PlaylistDao}).</p>
 */
@Dao
public interface PlaylistItemDao {

    /** Insert all items, replacing any existing rows with the same id. */
    @Upsert
    void upsertAll(List<PlaylistItemEntity> items);

    /**
     * Enabled items of a playlist, most important first:
     * higher {@code priority} first, then smaller {@code orderIndex}.
     */
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId AND enabled = 1 "
            + "ORDER BY priority DESC, orderIndex ASC")
    List<PlaylistItemEntity> getEnabledItems(String playlistId);

    /** All items of a playlist (enabled or not), in play order. */
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    List<PlaylistItemEntity> getAllItems(String playlistId);

    /** Remove every item of a playlist (used before saving a fresh copy). */
    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    void deleteForPlaylist(String playlistId);
}
