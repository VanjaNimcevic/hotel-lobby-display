package com.vanja.hotellobbydisplay.data;

import android.content.Context;
import android.util.Log;

import com.vanja.hotellobbydisplay.data.local.AppDatabase;
import com.vanja.hotellobbydisplay.data.local.PlaybackLogDao;
import com.vanja.hotellobbydisplay.data.local.PlaybackLogEntity;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Writes playback history to the {@code playback_logs} table: {@link #logStart}
 * inserts a row, {@link #logFinished} / {@link #logError} update that same row.
 * All writes run on one background thread.
 */
public class PlaybackLogger {

    private static final String TAG = "PlaybackLogger";

    private final PlaybackLogDao dao;
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    /** The current item's row, kept between logStart and finish. Only touched on backgroundExecutor. */
    private PlaybackLogEntity currentLog;

    public PlaybackLogger(Context context) {
        this.dao = AppDatabase.getInstance(context.getApplicationContext()).playbackLogDao();
    }

    /** {@code source} is "LOCAL" / "REMOTE" for media, null for text/banner. */
    public void logStart(String itemId, String source) {
        PlaybackLogEntity entity = new PlaybackLogEntity();
        entity.setItemId(itemId);
        entity.setStartedAt(System.currentTimeMillis());
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

    public void logFinished(String itemId) {
        finish(itemId, "COMPLETED", null);
    }

    public void logError(String itemId, String errorMessage) {
        finish(itemId, "ERROR", errorMessage);
    }

    private void finish(String itemId, String status, String errorMessage) {
        long finishedAt = System.currentTimeMillis();
        backgroundExecutor.execute(() -> {
            PlaybackLogEntity entity = currentLog;
            if (entity == null || !itemId.equals(entity.getItemId())) {
                Log.w(TAG, "No STARTED row for item=" + itemId + ", status=" + status);
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
