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
 * The on-device media cache: owns the cache directory, answers "is this URL
 * cached / where is it?", records downloads in {@code media_cache}, and deletes
 * files the playlist no longer needs.
 *
 * <p>{@link #isCached} / {@link #localPathIfAvailable} / {@link #fileFor} are
 * file-system only and thread-safe. The Room-writing methods are blocking - call
 * them off the main thread (the download worker does).</p>
 */
public class MediaCacheManager {

    private static final String TAG = "MediaCacheManager";
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

    /** Where the file for this URL lives (existence not checked). */
    public File fileFor(String sourceUrl) {
        return new File(cacheDir, buildFileName(sourceUrl));
    }

    public boolean isCached(String sourceUrl) {
        if (sourceUrl == null) {
            return false;
        }
        File file = fileFor(sourceUrl);
        return file.exists() && file.length() > 0;
    }

    /** @return local path to play, or null if there is no usable local copy. */
    public String localPathIfAvailable(String sourceUrl) {
        return isCached(sourceUrl) ? fileFor(sourceUrl).getAbsolutePath() : null;
    }

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

    /** Deletes cached files + rows for URLs not in {@code neededUrls}. */
    public void cleanUpUnused(Set<String> neededUrls) {
        Set<String> keepFileNames = new HashSet<>();
        for (String url : neededUrls) {
            keepFileNames.add(buildFileName(url));
        }

        for (MediaCacheEntity entry : dao.getAll()) {
            if (!neededUrls.contains(entry.getSourceUrl())) {
                deleteFile(entry.getLocalFilePath());
                dao.deleteByUrl(entry.getSourceUrl());
                Log.i(TAG, "Removed cache entry for " + entry.getSourceUrl());
            }
        }

        // Also drop stray files with no matching need (e.g. left after a crash).
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

    /** Stable file name from a URL: hex of hashCode + the clean file extension. */
    private String buildFileName(String sourceUrl) {
        String hash = Integer.toHexString(sourceUrl.hashCode());
        String extension = "";
        int lastDot = sourceUrl.lastIndexOf('.');
        int lastSlash = sourceUrl.lastIndexOf('/');
        if (lastDot > lastSlash && lastDot < sourceUrl.length() - 1) {
            String candidate = sourceUrl.substring(lastDot);
            if (candidate.length() <= 5 && candidate.matches("\\.[A-Za-z0-9]+")) {
                extension = candidate.toLowerCase();
            }
        }
        return hash + extension;
    }
}
