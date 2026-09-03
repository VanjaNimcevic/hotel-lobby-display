package com.vanja.hotellobbydisplay.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Reads and writes the {@code playback_logs} table.
 *
 * <p>Synchronous - call off the main thread (see {@link PlaylistDao}).</p>
 */
@Dao
public interface PlaybackLogDao {

    /**
     * Insert one log row (written when an item starts).
     *
     * @return the auto-generated row id, so the caller can {@link #update} the
     *         same row later with finish time / status
     */
    @Insert
    long insert(PlaybackLogEntity log);

    /** Update an existing log row (e.g. set finishedAt and status when the item ends). */
    @Update
    void update(PlaybackLogEntity log);

    /** Most recent log rows, newest first (for debugging / logcat dump). */
    @Query("SELECT * FROM playback_logs ORDER BY startedAt DESC LIMIT :limit")
    List<PlaybackLogEntity> getRecent(int limit);
}
