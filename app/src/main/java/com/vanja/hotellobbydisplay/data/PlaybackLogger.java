package com.vanja.hotellobbydisplay.data;

import android.content.Context;
import android.util.Log;

import com.vanja.hotellobbydisplay.data.local.AppDatabase;
import com.vanja.hotellobbydisplay.data.local.PlaybackLogDao;
import com.vanja.hotellobbydisplay.data.local.PlaybackLogEntity;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Writes playback history to the {@code playback_logs} Room table (APV-21).
 *
 * <p>{@link #logStart} inserts a row when an item starts. {@link #logFinished}
 * / {@link #logError} update that same row once the item ends. All writes run
 * on one background thread (Room forbids DB access on the main thread); the
 * caller ({@link com.vanja.hotellobbydisplay.playback.PlaybackController})
 * never waits for them.</p>
 */
public class PlaybackLogger {

    private static final String TAG = "PlaybackLogger";

    private final PlaybackLogDao dao;
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    /**
     * The row for the item currently playing, kept in memory between
     * {@link #logStart} and the matching finish/error call so the update goes
     * to the same row instead of creating a new one. Only ever touched from
     * {@code backgroundExecutor} (a single thread), so it needs no locking.
     */
    private PlaybackLogEntity currentLog;

    public PlaybackLogger(Context context) {
        this.dao = AppDatabase.getInstance(context.getApplicationContext()).playbackLogDao();
    }

    /**
     * Call right before an item starts playing.
     *
     * @param source "LOCAL" or "REMOTE" for media items (APV-24), or null for
     *               items that have no source concept (text / banner)
     */
    public void logStart(String itemId, String source) {
        long startedAt = System.currentTimeMillis();
        PlaybackLogEntity entity = new PlaybackLogEntity();
        entity.setItemId(itemId);
        entity.setStartedAt(startedAt);
        entity.setStatus("STARTED");
        entity.setSource(source);

        backgroundExecutor.execute(() -> {
            long id = dao.insert(entity);
            entity.setId(id);
            currentLog = entity;
            Log.i(TAG, "STARTED item=" + itemId
                    + (source != null ? " source=" + source : "") + " (row " + id + ")");
        });
    }

    /** Call when the item finishes normally. */
    public void logFinished(String itemId) {
        finish(itemId, "COMPLETED", null);
    }

    /** Call when the item fails with a playback error. */
    public void logError(String itemId, String errorMessage) {
        finish(itemId, "ERROR", errorMessage);
    }

    private void finish(String itemId, String status, String errorMessage) {
        long finishedAt = System.currentTimeMillis();
        backgroundExecutor.execute(() -> {
            PlaybackLogEntity entity = currentLog;
            if (entity == null || !itemId.equals(entity.getItemId())) {
                // No matching STARTED row (e.g. logger was just created) -
                // log to logcat only rather than update the wrong row.
                Log.w(TAG, "No STARTED row for item=" + itemId + ", status=" + status
                        + " - logcat only");
                return;
            }
            entity.setFinishedAt(finishedAt);
            entity.setStatus(status);
            entity.setErrorMessage(errorMessage);
            dao.update(entity);
            Log.i(TAG, status + " item=" + itemId + " (row " + entity.getId() + ")");
            currentLog = null;
        });
    }
}
