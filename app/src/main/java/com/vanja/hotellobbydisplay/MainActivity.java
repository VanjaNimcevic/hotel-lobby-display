package com.vanja.hotellobbydisplay;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import android.widget.ImageView;

import androidx.fragment.app.FragmentActivity;
import androidx.media3.ui.PlayerView;

import com.vanja.hotellobbydisplay.data.PlaylistRepository;
import com.vanja.hotellobbydisplay.data.local.PlaylistItemEntity;
import com.vanja.hotellobbydisplay.player.ImageRenderer;
import com.vanja.hotellobbydisplay.player.VideoRenderer;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";
    private static final String TYPE_VIDEO = "VIDEO";
    private static final String TYPE_IMAGE = "IMAGE";

    private PlaylistRepository playlistRepository;
    private VideoRenderer videoRenderer;
    private ImageRenderer imageRenderer;

    // Temporary driver (APV-15 + APV-16): plays VIDEO and IMAGE items in a
    // loop. Replaced by TimelineScheduler (APV-19) + PlaybackController (APV-20),
    // which will handle every type and the real schedule.
    private static final long RETRY_DELAY_MS = 3_000L;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final List<PlaylistItemEntity> playbackItems = new ArrayList<>();
    private int currentItemIndex = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideSystemUI();

        PlayerView videoView = findViewById(R.id.video_view);
        videoRenderer = new VideoRenderer(this, videoView);

        ImageView imageView = findViewById(R.id.image_view);
        imageRenderer = new ImageRenderer(this, imageView);

        playlistRepository = PlaylistRepository.getInstance(this);
        loadPlaylist();
    }

    /**
     * APV-13: ask the repository for the playlist. The repository handles assets,
     * parsing and Room storage on a background thread and calls back on the main
     * thread. The Activity never touches the parser, assets or DAOs directly.
     */
    private void loadPlaylist() {
        playlistRepository.loadInitialPlaylist(new PlaylistRepository.Callback() {
            @Override
            public void onPlaylistReady(List<PlaylistItemEntity> enabledItems) {
                Log.i(TAG, "Playlist ready: " + enabledItems.size() + " enabled items");

                playbackItems.clear();
                for (PlaylistItemEntity item : enabledItems) {
                    boolean supported = TYPE_VIDEO.equals(item.getType())
                            || TYPE_IMAGE.equals(item.getType());
                    if (supported && item.getUrl() != null) {
                        playbackItems.add(item);
                    }
                }
                Log.i(TAG, "Playable items (VIDEO/IMAGE for now): " + playbackItems.size());
                currentItemIndex = 0;
                playCurrentItem();
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Could not load playlist: " + message);
            }
        });
    }

    /**
     * Temporary driver: show the current item with the matching renderer, then
     * advance on finish/error. Only one of video_view / image_view is ever
     * visible - the other renderer is stopped first.
     */
    private void playCurrentItem() {
        if (playbackItems.isEmpty()) {
            Log.i(TAG, "No playable items");
            return;
        }

        videoRenderer.release();
        imageRenderer.cancelPending();

        View videoView = findViewById(R.id.video_view);
        View imageView = findViewById(R.id.image_view);

        PlaylistItemEntity item = playbackItems.get(currentItemIndex);
        Log.i(TAG, "ITEM " + (currentItemIndex + 1) + "/" + playbackItems.size()
                + " -> " + item.getId() + " (" + item.getType() + ")");

        if (TYPE_VIDEO.equals(item.getType())) {
            imageView.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            videoRenderer.play(item.getUrl(), new VideoRenderer.Listener() {
                @Override
                public void onFinished() {
                    advanceToNextItem(0L);
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "Skipping VIDEO " + item.getId() + " after error: " + message);
                    advanceToNextItem(RETRY_DELAY_MS);
                }
            });
        } else {
            videoView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            imageRenderer.play(item.getUrl(), item.getDurationSec(), item.getMetadataScaleType(),
                    new ImageRenderer.Listener() {
                        @Override
                        public void onFinished() {
                            advanceToNextItem(0L);
                        }

                        @Override
                        public void onError(String message) {
                            Log.e(TAG, "Skipping IMAGE " + item.getId()
                                    + " after error: " + message);
                            advanceToNextItem(RETRY_DELAY_MS);
                        }
                    });
        }
    }

    private void advanceToNextItem(long delayMs) {
        uiHandler.postDelayed(() -> {
            currentItemIndex = (currentItemIndex + 1) % playbackItems.size();
            playCurrentItem();
        }, delayMs);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Returning from the background: re-create the renderer and resume the loop.
        if (!playbackItems.isEmpty()) {
            playCurrentItem();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop every renderer and drop any pending "next item" callback while
        // the app is not visible (APV-15/16 lifecycle rule).
        uiHandler.removeCallbacksAndMessages(null);
        if (videoRenderer != null) {
            videoRenderer.release();
        }
        if (imageRenderer != null) {
            imageRenderer.cancelPending();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
