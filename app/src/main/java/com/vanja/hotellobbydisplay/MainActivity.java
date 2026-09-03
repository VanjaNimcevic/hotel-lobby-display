package com.vanja.hotellobbydisplay;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.fragment.app.FragmentActivity;

import com.vanja.hotellobbydisplay.data.PlaylistJsonParser;
import com.vanja.hotellobbydisplay.model.PlaylistItemModel;
import com.vanja.hotellobbydisplay.model.PlaylistModel;
import com.vanja.hotellobbydisplay.util.AssetFileReader;

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";

    /** Path of the bundled playlist inside app/src/main/assets. */
    private static final String PLAYLIST_ASSET = "json/sample_playlist.json";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideSystemUI();
        loadPlaylistJson();
    }

    /**
     * APV-7 + APV-10: on startup, read the sample playlist JSON from assets and
     * parse it into a {@link PlaylistModel}, logging the result. Later tasks add
     * remote loading (APV-14) and storage in Room (APV-13).
     */
    private void loadPlaylistJson() {
        String json = AssetFileReader.readAssetFile(this, PLAYLIST_ASSET);

        if (json == null) {
            // AssetFileReader already logged the underlying error. Make it clear
            // here too, and keep the app alive instead of crashing.
            Log.e(TAG, "Playlist JSON could not be loaded from assets ('"
                    + PLAYLIST_ASSET + "'). Continuing without a playlist.");
            return;
        }
        Log.i(TAG, "Playlist JSON loaded from assets (" + json.length() + " chars)");

        PlaylistModel playlist = new PlaylistJsonParser().parse(json);
        if (playlist == null) {
            // Parser already logged why. Keep the app alive.
            Log.e(TAG, "Playlist JSON could not be parsed. Continuing without a playlist.");
            return;
        }

        Log.i(TAG, "Playlist ready: " + playlist);
        for (PlaylistItemModel item : playlist.getItems()) {
            Log.i(TAG, "  item -> " + item);
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