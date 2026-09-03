package com.vanja.hotellobbydisplay.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * The app's Room database.
 *
 * <p>APV-11 wires the four entities into a schema so Room's compiler can
 * validate it. APV-12 adds the DAO accessor methods below. A singleton builder
 * is added in APV-13.</p>
 *
 * <p>{@code exportSchema = false}: we are not keeping a JSON history of the
 * schema for migrations in this project. If migrations become important later,
 * set this to true and add a schema directory to the Gradle config.</p>
 */
@Database(
        entities = {
                PlaylistEntity.class,
                PlaylistItemEntity.class,
                MediaCacheEntity.class,
                PlaybackLogEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "hotel_lobby.db";

    private static AppDatabase instance;

    /**
     * Returns the one shared database instance, creating it on first call.
     * {@code synchronized} so two threads calling this at the same time cannot
     * build two databases.
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DB_NAME)
                    // This project has no migrations: if the schema version ever
                    // changes without one, wipe and rebuild instead of crashing.
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
