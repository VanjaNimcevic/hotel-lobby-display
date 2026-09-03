package com.vanja.hotellobbydisplay;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.fragment.app.FragmentActivity;

import com.vanja.hotellobbydisplay.data.PlaylistRepository;
import com.vanja.hotellobbydisplay.data.local.PlaylistItemEntity;

import java.util.List;

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";

    private PlaylistRepository playlistRepository;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideSystemUI();

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
                for (PlaylistItemEntity item : enabledItems) {
                    Log.i(TAG, "  item -> " + item.getId() + " (" + item.getType()
                            + ", " + item.getDurationSec() + "s)");
                }
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Could not load playlist: " + message);
            }
        });
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