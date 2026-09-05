package com.vanja.hotellobbydisplay.data;

import android.content.Context;
import android.util.Log;

import com.vanja.hotellobbydisplay.data.local.AppDatabase;
import com.vanja.hotellobbydisplay.data.local.MediaCacheDao;
import com.vanja.hotellobbydisplay.data.local.MediaCacheEntity;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages the on-device cache of downloaded media files (APV-22).
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>own the cache directory inside app private storage;</li>
 *   <li>answer "is there a local copy of this URL?" and "what is its path?"
 *       (used by APV-24 to prefer local files during playback);</li>
 *   <li>record downloads in the {@code media_cache} table (called from the
 *       download worker in APV-23);</li>
 *   <li>delete cached files that the current playlist no longer needs.</li>
 * </ul>
 *
 * <p>{@link #isCached} / {@link #localPathIfAvailable} / {@link #fileFor} only
 * touch the file system and are safe to call from any thread. The methods that
 * write to Room ({@link #markDownloaded}, {@link #markStatus},
 * {@link #cleanUpUnused}) are synchronous and MUST be called off the main
 * thread - their only caller today is {@link MediaDownloadWorker}, which
 * already runs on a background thread.</p>
 */
public class MediaCacheManager {

    private static final String TAG = "MediaCacheManager";

    /** Sub-folder of the app's private files dir where media is cached. */
    private static final String CACHE_DIR_NAME = "media_cache";

    private final File cacheDir;
    private final MediaCacheDao dao;

    public MediaCacheManager(Context context) {
        Context app = context.getApplicationContext();
        this.cacheDir = new File(app.getFilesDir(), CACHE_DIR_NAME);
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            Log.w(TAG, "Could not create media cache dir: " + cacheDir.getAbsolutePath());
        } else {
            Log.i(TAG, "Media cache dir: " + cacheDir.getAbsolutePath());
        }
        this.dao = AppDatabase.getInstance(app).mediaCacheDao();
    }

    /**
     * Where the file for this URL lives (or would live). Does not check that it
     * actually exists - use {@link #isCached} or {@link #localPathIfAvailable}.
     */
    public File fileFor(String sourceUrl) {
        return new File(cacheDir, buildFileName(sourceUrl));
    }

    /** @return true if a non-empty local copy of this URL is on disk. */
    public boolean isCached(String sourceUrl) {
        if (sourceUrl == null) {
            return false;
        }
        File file = fileFor(sourceUrl);
        return file.exists() && file.length() > 0;
    }

    /**
     * @return absolute path of the local copy to play, or {@code null} if there
     *         is no usable local copy. APV-24 uses this to choose local vs remote.
     */
    public String localPathIfAvailable(String sourceUrl) {
        return isCached(sourceUrl) ? fileFor(sourceUrl).getAbsolutePath() : null;
    }

    /** Records in {@code media_cache} that a file has finished downloading. Call off the main thread. */
    public void markDownloaded(String sourceUrl, long fileSizeBytes) {
        MediaCacheEntity entry = new MediaCacheEntity();
        entry.setSourceUrl(sourceUrl);
        entry.setLocalFilePath(fileFor(sourceUrl).getAbsolutePath());
        entry.setStatus("COMPLETED");
        entry.setFileSizeBytes(fileSizeBytes);
        entry.setUpdatedAt(System.currentTimeMillis());
        dao.upsert(entry);
        Log.i(TAG, "Cached " + sourceUrl + " (" + fileSizeBytes + " bytes)");
    }

    /** Records a status change (e.g. DOWNLOADING, FAILED) in {@code media_cache}. Call off the main thread. */
    public void markStatus(String sourceUrl, String status) {
        MediaCacheEntity entry = new MediaCacheEntity();
        entry.setSourceUrl(sourceUrl);
        entry.setLocalFilePath(null);
        entry.setStatus(status);
        entry.setFileSizeBytes(0);
        entry.setUpdatedAt(System.currentTimeMillis());
        dao.upsert(entry);
        Log.i(TAG, "Status " + status + " for " + sourceUrl);
    }

    /**
     * Deletes cached files (and their {@code media_cache} rows) for URLs that
     * are no longer in the current playlist. Call off the main thread.
     *
     * @param neededUrls the media URLs the current playlist still uses
     */
    public void cleanUpUnused(Set<String> neededUrls) {
        Set<String> keepFileNames = new HashSet<>();
        for (String url : neededUrls) {
            keepFileNames.add(buildFileName(url));
        }

        // 1. Drop DB rows + files for URLs the playlist no longer references.
        for (MediaCacheEntity entry : dao.getAll()) {
            if (!neededUrls.contains(entry.getSourceUrl())) {
                deleteFile(entry.getLocalFilePath());
                dao.deleteByUrl(entry.getSourceUrl());
                Log.i(TAG, "Removed cache entry for " + entry.getSourceUrl());
            }
        }

        // 2. Drop stray files on disk that have no matching need.
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!keepFileNames.contains(file.getName()) && file.delete()) {
                    Log.i(TAG, "Deleted stray cache file: " + file.getName());
                }
            }
        }
    }

    private void deleteFile(String path) {
        if (path == null) {
            return;
        }
        File file = new File(path);
        if (file.exists() && file.delete()) {
            Log.i(TAG, "Deleted cached file: " + path);
        }
    }

    /**
     * Turns a URL into a stable file name: a hex of the URL's hashCode (so two
     * different URLs almost never collide) plus the original file extension
     * (so the player can still tell .mp4 from .jpg).
     */
    private String buildFileName(String sourceUrl) {
        String hash = Integer.toHexString(sourceUrl.hashCode());
        String extension = "";
        int lastDot = sourceUrl.lastIndexOf('.');
        int lastSlash = sourceUrl.lastIndexOf('/');
        if (lastDot > lastSlash && lastDot < sourceUrl.length() - 1) {
            String candidate = sourceUrl.substring(lastDot);
            // Only keep short, clean extensions (avoid ".mp4?token=..." junk).
            if (candidate.length() <= 5 && candidate.matches("\\.[A-Za-z0-9]+")) {
                extension = candidate.toLowerCase();
            }
        }
        return hash + extension;
    }
}
