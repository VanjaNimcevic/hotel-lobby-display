package com.vanja.hotellobbydisplay.data.local;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

/** {@code playlists} table. All methods block - call off the main thread. */
@Dao
public interface PlaylistDao {

    @Upsert
    void upsert(PlaylistEntity playlist);

    @Query("SELECT * FROM playlists WHERE active = 1 LIMIT 1")
    PlaylistEntity getActivePlaylist();

    @Query("UPDATE playlists SET active = 0")
    void clearActive();

    /** Mark one playlist active (call {@link #clearActive()} first). */
    @Query("UPDATE playlists SET active = 1 WHERE playlistId = :playlistId")
    void setActive(String playlistId);
}
