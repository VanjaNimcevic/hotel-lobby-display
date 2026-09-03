package com.vanja.hotellobbydisplay.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * The single place the rest of the app asks for playlist data.
 *
 * <p>Hides <em>where</em> the playlist comes from (bundled assets JSON for now,
 * a remote URL in APV-14), <em>how</em> it is parsed ({@link PlaylistJsonParser})
 * and <em>how</em> it is stored (Room). The UI layer talks only to this class -
 * never to the parser, {@link AssetFileReader} or the DAOs.</p>
 *
 * <p>All database work runs on one background thread; results come back on the
 * main thread through {@link Callback}.</p>
 */
public class PlaylistRepository {

    private static final String TAG = "PlaylistRepository";

    /**
     * Where the playlist is downloaded from. Replace with the real CDN URL.
     * The placeholder below does not serve a playlist, so out of the box the
     * app falls back to the bundled asset - which is exactly the behaviour
     * APV-14 asks us to support.
     */
    private static final String PLAYLIST_URL = "https://example.com/hotel-lobby/playlist.json";

    /** Bundled copy used when the remote URL cannot be reached or is invalid. */
    private static final String PLAYLIST_ASSET = "json/sample_playlist.json";

    /** Delivered on the main thread when a load finishes. */
    public interface Callback {
        void onPlaylistReady(List<PlaylistItemEntity> enabledItems);

        void onError(String message);
    }

    private static PlaylistRepository instance;

    /** One shared repository instance for the whole app. */
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

    private PlaylistRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.db = AppDatabase.getInstance(this.appContext);
    }

    /**
     * Loads the bundled playlist from assets, parses it, stores it in Room and
     * calls back with the enabled items. Runs off the main thread.
     */
    public void loadInitialPlaylist(Callback callback) {
        backgroundExecutor.execute(() -> {
            try {
                List<PlaylistItemEntity> items = loadAndStore();
                mainThread.post(() -> callback.onPlaylistReady(items));
            } catch (Exception e) {
                Log.e(TAG, "Failed to load playlist", e);
                mainThread.post(() -> callback.onError(String.valueOf(e.getMessage())));
            }
        });
    }

    /**
     * Reads the enabled items of the currently active playlist straight from
     * Room (no assets, no parsing). Runs off the main thread.
     */
    public void getCurrentItems(Callback callback) {
        backgroundExecutor.execute(() -> {
            try {
                mainThread.post(() -> callback.onPlaylistReady(readEnabledItemsOfActivePlaylist()));
            } catch (Exception e) {
                Log.e(TAG, "Failed to read current items", e);
                mainThread.post(() -> callback.onError(String.valueOf(e.getMessage())));
            }
        });
    }

    // ------------------------------------------------------------------
    // Everything below runs on the background thread.
    // ------------------------------------------------------------------

    private List<PlaylistItemEntity> loadAndStore() {
        PlaylistModel model = null;
        String source = null;

        // 1. Try the remote URL first.
        String remoteJson = HttpTextFetcher.fetch(PLAYLIST_URL);
        if (remoteJson != null) {
            model = parser.parse(remoteJson);
            if (model != null) {
                source = "REMOTE";
            }
        }

        // 2. Fall back to the bundled asset if remote failed or was invalid.
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
            throw new IllegalStateException("playlist could not be loaded from remote or assets");
        }

        Log.i(TAG, "Playlist loaded from " + source + " (playlistId=" + model.getPlaylistId()
                + ", version=" + model.getVersion() + ")");

        PlaylistEntity playlistEntity = toEntity(model);
        List<PlaylistItemEntity> itemEntities = toEntities(model);

        PlaylistDao playlistDao = db.playlistDao();
        PlaylistItemDao itemDao = db.playlistItemDao();

        // One transaction: either the whole playlist is replaced, or nothing.
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
        return stored;
    }

    private List<PlaylistItemEntity> readEnabledItemsOfActivePlaylist() {
        PlaylistEntity active = db.playlistDao().getActivePlaylist();
        if (active == null) {
            return new ArrayList<>();
        }
        return db.playlistItemDao().getEnabledItems(active.getPlaylistId());
    }

    // ------------------------------------------------------------------
    // Mapping: model (from JSON) -> entity (Room row).
    // ------------------------------------------------------------------

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
            // Bonus feature: keep the layout object as raw JSON, parsed on demand.
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
