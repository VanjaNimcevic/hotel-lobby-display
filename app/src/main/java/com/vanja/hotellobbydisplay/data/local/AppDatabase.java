package com.vanja.hotellobbydisplay.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * The app's Room database.
 *
 * <p>APV-11 wires the four entities into a schema so Room's compiler can
 * validate it. DAO accessor methods are added in APV-12 and a singleton
 * builder in APV-13.</p>
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
    // DAOs added in APV-12.
}
