package com.vanja.hotellobbydisplay.data.local;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

/**
 * Reads and writes the {@code playlists} table.
 *
 * <p>All methods are synchronous - they block until the database finishes - so
 * they must be called off the main thread. The repository (APV-13) runs them on
 * a background executor.</p>
 */
@Dao
public interface PlaylistDao {

    /** Insert the playlist, or replace the existing row with the same playlistId. */
    @Upsert
    void upsert(PlaylistEntity playlist);

    /** @return the playlist currently marked active, or {@code null} if there is none. */
    @Query("SELECT * FROM playlists WHERE active = 1 LIMIT 1")
    PlaylistEntity getActivePlaylist();

    /** Clear the active flag on every playlist. */
    @Query("UPDATE playlists SET active = 0")
    void clearActive();

    /** Mark one playlist as the active one (call {@link #clearActive()} first). */
    @Query("UPDATE playlists SET active = 1 WHERE playlistId = :playlistId")
    void setActive(String playlistId);
}
