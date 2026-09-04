package com.vanja.hotellobbydisplay;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.fragment.app.FragmentActivity;
import androidx.media3.ui.PlayerView;

import com.vanja.hotellobbydisplay.data.PlaylistRepository;
import com.vanja.hotellobbydisplay.data.local.PlaylistItemEntity;
import com.vanja.hotellobbydisplay.player.VideoRenderer;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";

    private PlaylistRepository playlistRepository;
    private VideoRenderer videoRenderer;

    // Temporary APV-15 driver: just the VIDEO items, played in a loop.
    // Replaced by TimelineScheduler (APV-19) + PlaybackController (APV-20).
    private static final long RETRY_DELAY_MS = 3_000L;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final List<PlaylistItemEntity> videoItems = new ArrayList<>();
    private int currentVideoIndex = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideSystemUI();

        PlayerView videoView = findViewById(R.id.video_view);
        videoRenderer = new VideoRenderer(this, videoView);

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

                videoItems.clear();
                for (PlaylistItemEntity item : enabledItems) {
                    if ("VIDEO".equals(item.getType()) && item.getUrl() != null) {
                        videoItems.add(item);
                    }
                }
                Log.i(TAG, "VIDEO items: " + videoItems.size());
                currentVideoIndex = 0;
                playCurrentVideo();
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Could not load playlist: " + message);
            }
        });
    }

    /** APV-15 temporary driver: play the current VIDEO item; advance on finish/error. */
    private void playCurrentVideo() {
        if (videoItems.isEmpty()) {
            Log.i(TAG, "No VIDEO items to play");
            return;
        }

        PlaylistItemEntity item = videoItems.get(currentVideoIndex);
        findViewById(R.id.video_view).setVisibility(View.VISIBLE);
        Log.i(TAG, "VIDEO " + (currentVideoIndex + 1) + "/" + videoItems.size()
                + " -> " + item.getId());

        videoRenderer.play(item.getUrl(), new VideoRenderer.Listener() {
            @Override
            public void onFinished() {
                advanceToNextVideo(0L);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Skipping VIDEO " + item.getId() + " after error: " + message);
                // Wait a bit so a fully broken playlist does not spin the network.
                advanceToNextVideo(RETRY_DELAY_MS);
            }
        });
    }

    private void advanceToNextVideo(long delayMs) {
        uiHandler.postDelayed(() -> {
            currentVideoIndex = (currentVideoIndex + 1) % videoItems.size();
            playCurrentVideo();
        }, delayMs);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Returning from the background: re-create the player and resume the loop.
        if (!videoItems.isEmpty()) {
            playCurrentVideo();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Free the player and drop any pending "next video" callback while the
        // app is not visible (APV-15 lifecycle rule).
        uiHandler.removeCallbacksAndMessages(null);
        if (videoRenderer != null) {
            videoRenderer.release();
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