package com.vanja.hotellobbydisplay.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room database, accessed as a singleton.
 *
 * <p>v2 added {@code PlaybackLogEntity.source}. No {@code Migration} is written:
 * {@code fallbackToDestructiveMigration()} wipes and rebuilds on a version bump
 * (playback logs are debug data; the playlist reloads on next launch).</p>
 */
@Database(
        entities = {
                PlaylistEntity.class,
                PlaylistItemEntity.class,
                MediaCacheEntity.class,
                PlaybackLogEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "hotel_lobby.db";

    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(), AppDatabase.class, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public abstract PlaylistDao playlistDao();

    public abstract PlaylistItemDao playlistItemDao();

    public abstract MediaCacheDao mediaCacheDao();

    public abstract PlaybackLogDao playbackLogDao();
}
