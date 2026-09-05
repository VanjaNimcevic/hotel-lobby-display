package com.vanja.hotellobbydisplay.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.vanja.hotellobbydisplay.data.local.AppDatabase;
import com.vanja.hotellobbydisplay.data.local.PlaylistEntity;
import com.vanja.hotellobbydisplay.data.local.PlaylistItemEntity;
import com.vanja.hotellobbydisplay.util.HttpFileDownloader;

import java.util.List;

/**
 * Downloads the active playlist's VIDEO and IMAGE files for offline playback
 * (APV-23).
 *
 * <p>Runs as a WorkManager {@link Worker}: {@code doWork()} is called on a
 * background thread WorkManager owns, so it can do blocking network + Room work
 * directly. It survives the app being backgrounded or killed, and WorkManager
 * retries it (with back-off) if any file failed.</p>
 *
 * <p>Enqueued by {@link PlaylistRepository} every time a playlist is stored.</p>
 */
public class MediaDownloadWorker extends Worker {

    private static final String TAG = "MediaDownloadWorker";

    /** Unique work name so a new playlist replaces a still-pending download. */
    public static final String WORK_NAME = "media-download";

    private static final String TYPE_VIDEO = "VIDEO";
    private static final String TYPE_IMAGE = "IMAGE";

    public MediaDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(context);
        MediaCacheManager cache = new MediaCacheManager(context);

        PlaylistEntity active = db.playlistDao().getActivePlaylist();
        if (active == null) {
            Log.i(TAG, "No active playlist - nothing to download");
            return Result.success();
        }

        List<PlaylistItemEntity> items = db.playlistItemDao().getEnabledItems(active.getPlaylistId());

        int downloaded = 0;
        int skipped = 0;
        int failed = 0;

        for (PlaylistItemEntity item : items) {
            String type = item.getType();
            if (!TYPE_VIDEO.equals(type) && !TYPE_IMAGE.equals(type)) {
                continue;
            }
            String url = item.getUrl();
            if (url == null) {
                continue;
            }

            if (cache.isCached(url)) {
                skipped++;
                continue;
            }

            cache.markStatus(url, "DOWNLOADING");
            long bytes = HttpFileDownloader.download(url, cache.fileFor(url));
            if (bytes >= 0) {
                cache.markDownloaded(url, bytes);
                downloaded++;
            } else {
                cache.markStatus(url, "FAILED");
                failed++;
                Log.w(TAG, "Failed to download " + url + " (continuing with the rest)");
            }
        }

        Log.i(TAG, "Download pass done: " + downloaded + " downloaded, " + skipped
                + " already cached, " + failed + " failed");

        // Ask WorkManager to try the failed ones again later (it skips the ones
        // that already succeeded). A clean pass just succeeds.
        return failed > 0 ? Result.retry() : Result.success();
    }
}
