package com.vanja.hotellobbydisplay.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.vanja.hotellobbydisplay.data.local.AppDatabase;
import com.vanja.hotellobbydisplay.data.local.PlaylistDao;
import com.vanja.hotellobbydisplay.data.local.PlaylistEntity;
import com.vanja.hotellobbydisplay.data.local.PlaylistItemDao;
import com.vanja.hotellobbydisplay.data.local.PlaylistItemEntity;
import com.vanja.hotellobbydisplay.model.PlaylistItemModel;
import com.vanja.hotellobbydisplay.model.PlaylistModel;
import com.vanja.hotellobbydisplay.util.AssetFileReader;
import com.vanja.hotellobbydisplay.util.HttpTextFetcher;
import com.vanja.hotellobbydisplay.util.NetworkMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * The one place the app gets playlist data. Hides where it comes from (remote
 * URL, bundled asset, or Room when offline), parsing, and Room storage. All
 * work runs on one background thread; results come back via {@link Callback}.
 */
public class PlaylistRepository {

    private static final String TAG = "PlaylistRepository";

    /** Replace with the real endpoint. The placeholder just triggers the asset fallback. */
    private static final String PLAYLIST_URL = "https://example.com/hotel-lobby/playlist.json";
    private static final String PLAYLIST_ASSET = "json/sample_playlist.json";

    public interface Callback {
        /** {@code playlistSource} is "REMOTE", "ASSETS" or "ROOM". */
        void onPlaylistReady(List<PlaylistItemEntity> enabledItems, String playlistSource);

        void onError(String message);
    }

    private static PlaylistRepository instance;

    public static synchronized PlaylistRepository getInstance(Context context) {
        if (instance == null) {
            instance = new PlaylistRepository(context.getApplicationContext());
        }
        return instance;
    }

    private final Context appContext;
    private final AppDatabase db;
    private final PlaylistJsonParser parser = new PlaylistJsonParser();
    private final Gson gson = new Gson();
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());

    private volatile String lastPlaylistSource = "-";

    private PlaylistRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.db = AppDatabase.getInstance(this.appContext);
    }

    /** Loads, parses and stores the playlist, then calls back with the enabled items. */
    public void loadInitialPlaylist(Callback callback) {
        backgroundExecutor.execute(() -> {
            try {
                List<PlaylistItemEntity> items = loadAndStore();
                String source = lastPlaylistSource;
                mainThread.post(() -> callback.onPlaylistReady(items, source));
            } catch (Exception e) {
                Log.e(TAG, "Failed to load playlist", e);
                mainThread.post(() -> callback.onError(String.valueOf(e.getMessage())));
            }
        });
    }

    /** Reads the active playlist's enabled items straight from Room. */
    public void getCurrentItems(Callback callback) {
        backgroundExecutor.execute(() -> {
            try {
                List<PlaylistItemEntity> items = readEnabledItemsOfActivePlaylist();
                mainThread.post(() -> callback.onPlaylistReady(items, "ROOM"));
            } catch (Exception e) {
                Log.e(TAG, "Failed to read current items", e);
                mainThread.post(() -> callback.onError(String.valueOf(e.getMessage())));
            }
        });
    }

    // --- runs on the background thread ---

    private List<PlaylistItemEntity> loadAndStore() {
        boolean online = NetworkMonitor.isOnline(appContext);
        Log.i(TAG, "Network state: " + (online ? "ONLINE" : "OFFLINE"));

        // Offline: skip the network, use the last playlist stored in Room.
        if (!online) {
            List<PlaylistItemEntity> fromRoom = readEnabledItemsOfActivePlaylist();
            if (!fromRoom.isEmpty()) {
                Log.i(TAG, "Offline: using the playlist already in Room (" + fromRoom.size()
                        + " enabled items)");
                lastPlaylistSource = "ROOM";
                return fromRoom;
            }
            Log.w(TAG, "Offline and nothing stored yet - using the bundled asset");
        }

        PlaylistModel model = null;
        String source = null;

        if (online) {
            String remoteJson = HttpTextFetcher.fetch(PLAYLIST_URL);
            if (remoteJson != null) {
                model = parser.parse(remoteJson);
                if (model != null) {
                    source = "REMOTE";
                }
            }
        }

        if (model == null) {
            String assetJson = AssetFileReader.readAssetFile(appContext, PLAYLIST_ASSET);
            if (assetJson != null) {
                model = parser.parse(assetJson);
                if (model != null) {
                    source = "ASSETS";
                }
            }
        }

        if (model == null) {
            List<PlaylistItemEntity> fromRoom = readEnabledItemsOfActivePlaylist();
            if (!fromRoom.isEmpty()) {
                Log.w(TAG, "Could not load a fresh playlist - using the one already in Room");
                lastPlaylistSource = "ROOM";
                return fromRoom;
            }
            throw new IllegalStateException("playlist could not be loaded from remote, assets or Room");
        }

        lastPlaylistSource = source;
        Log.i(TAG, "Playlist loaded from " + source + " (playlistId=" + model.getPlaylistId()
                + ", version=" + model.getVersion() + ")");

        PlaylistEntity playlistEntity = toEntity(model);
        List<PlaylistItemEntity> itemEntities = toEntities(model);
        PlaylistDao playlistDao = db.playlistDao();
        PlaylistItemDao itemDao = db.playlistItemDao();

        // One transaction: the whole playlist is replaced, or nothing.
        db.runInTransaction(() -> {
            playlistDao.upsert(playlistEntity);
            itemDao.deleteForPlaylist(playlistEntity.getPlaylistId());
            itemDao.upsertAll(itemEntities);
            playlistDao.clearActive();
            playlistDao.setActive(playlistEntity.getPlaylistId());
        });

        List<PlaylistItemEntity> stored = itemDao.getEnabledItems(playlistEntity.getPlaylistId());
        Log.i(TAG, "Stored playlist '" + playlistEntity.getPlaylistId() + "' with "
                + itemEntities.size() + " items (" + stored.size() + " enabled)");

        enqueueMediaDownload();
        return stored;
    }

    /** REPLACE so a fresh playlist supersedes a download still queued for the previous one. */
    private void enqueueMediaDownload() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MediaDownloadWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(appContext).enqueueUniqueWork(
                MediaDownloadWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, request);
        Log.i(TAG, "Enqueued media download work");
    }

    private List<PlaylistItemEntity> readEnabledItemsOfActivePlaylist() {
        PlaylistEntity active = db.playlistDao().getActivePlaylist();
        if (active == null) {
            return new ArrayList<>();
        }
        return db.playlistItemDao().getEnabledItems(active.getPlaylistId());
    }

    // --- model (JSON) -> entity (Room row) ---

    private PlaylistEntity toEntity(PlaylistModel model) {
        PlaylistEntity e = new PlaylistEntity();
        e.setPlaylistId(model.getPlaylistId());
        e.setVersion(model.getVersion());
        e.setUpdatedAt(model.getUpdatedAt());
        e.setActive(true);
        e.setFetchedAt(System.currentTimeMillis());
        return e;
    }

    private List<PlaylistItemEntity> toEntities(PlaylistModel model) {
        List<PlaylistItemEntity> result = new ArrayList<>();
        for (PlaylistItemModel item : model.getItems()) {
            result.add(toEntity(item, model.getPlaylistId()));
        }
        return result;
    }

    private PlaylistItemEntity toEntity(PlaylistItemModel item, String playlistId) {
        PlaylistItemEntity e = new PlaylistItemEntity();
        e.setId(item.getId());
        e.setPlaylistId(playlistId);
        e.setType(item.getType());
        e.setText(item.getText());
        e.setUrl(item.getUrl());
        e.setDurationSec(item.getDurationSec());
        e.setOrderIndex(item.getOrderIndex());
        e.setEnabled(item.isEnabled());
        e.setPriority(item.getPriority());
        e.setEmergency(item.isEmergency());

        if (item.getSchedule() != null) {
            e.setScheduleStartAt(item.getSchedule().getStartAt());
            e.setScheduleEndAt(item.getSchedule().getEndAt());
            e.setScheduleDaysOfWeek(joinDays(item.getSchedule().getDaysOfWeek()));
            e.setScheduleStartTime(item.getSchedule().getStartTime());
            e.setScheduleEndTime(item.getSchedule().getEndTime());
        }
        if (item.getMetadata() != null) {
            e.setMetadataBannerPosition(item.getMetadata().getBannerPosition());
            e.setMetadataJavascriptEnabled(item.getMetadata().isJavascriptEnabled());
            e.setMetadataScaleType(item.getMetadata().getScaleType());
        }
        if (item.getLayout() != null) {
            e.setLayoutJson(gson.toJson(item.getLayout()));
        }
        return e;
    }

    /** [1, 2, 3] -> "1,2,3"; null / empty -> null. */
    private String joinDays(List<Integer> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }
        return TextUtils.join(",", days);
    }
}
