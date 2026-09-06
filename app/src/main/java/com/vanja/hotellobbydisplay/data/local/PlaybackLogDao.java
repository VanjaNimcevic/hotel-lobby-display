package com.vanja.hotellobbydisplay.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/** {@code playback_logs} table. Blocking - call off the main thread. */
@Dao
public interface PlaybackLogDao {

    /** @return the auto-generated row id, so the caller can {@link #update} the same row later. */
    @Insert
    long insert(PlaybackLogEntity log);

    @Update
    void update(PlaybackLogEntity log);

    @Query("SELECT * FROM playback_logs ORDER BY startedAt DESC LIMIT :limit")
    List<PlaybackLogEntity> getRecent(int limit);
}
