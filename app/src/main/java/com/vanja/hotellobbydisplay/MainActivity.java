package com.vanja.hotellobbydisplay;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.media3.ui.PlayerView;

import com.vanja.hotellobbydisplay.data.PlaylistRepository;
import com.vanja.hotellobbydisplay.data.local.PlaylistItemEntity;
import com.vanja.hotellobbydisplay.playback.PlaybackController;

import java.util.List;

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";

    private PlaylistRepository playlistRepository;
    private PlaybackController playbackController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideSystemUI();

        PlayerView videoView = findViewById(R.id.video_view);
        ImageView imageView = findViewById(R.id.image_view);
        TextView textView = findViewById(R.id.text_view);
        ViewGroup webContainer = findViewById(R.id.web_container);
        playbackController = new PlaybackController(this, videoView, imageView, textView, webContainer);

        playlistRepository = PlaylistRepository.getInstance(this);
        loadPlaylist();
    }

    /**
     * APV-13 + APV-19 + APV-20: ask the repository for the playlist, then hand
     * it to the PlaybackController, which uses TimelineScheduler to pick items
     * and the right renderer to show them. The Activity never touches assets,
     * the parser, the DAOs or a renderer directly.
     */
    private void loadPlaylist() {
        playlistRepository.loadInitialPlaylist(new PlaylistRepository.Callback() {
            @Override
            public void onPlaylistReady(List<PlaylistItemEntity> enabledItems) {
                Log.i(TAG, "Playlist ready: " + enabledItems.size() + " enabled items");
                playbackController.setItems(enabledItems);
                playbackController.start();
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Could not load playlist: " + message);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        playbackController.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        playbackController.stop();
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
