package com.vanja.hotellobbydisplay.data.local;

import androidx.room.Database;
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

    public abstract PlaylistDao playlistDao();

    public abstract PlaylistItemDao playlistItemDao();

    public abstract MediaCacheDao mediaCacheDao();

    public abstract PlaybackLogDao playbackLogDao();
}
