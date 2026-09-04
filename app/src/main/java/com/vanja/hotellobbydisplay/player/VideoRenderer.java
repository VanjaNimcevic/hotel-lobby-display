package com.vanja.hotellobbydisplay.player;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.io.File;

/**
 * Plays a single VIDEO playlist item on a Media3 {@link PlayerView}.
 *
 * <p>Owns one {@link ExoPlayer}. Call {@link #play} to start an item and
 * {@link #release} from the Activity lifecycle to free it. The
 * {@link Listener} reports when the video ends or fails, so the caller (a
 * temporary driver now, {@code PlaybackController} in APV-20) can move to the
 * next item.</p>
 */
public class VideoRenderer {

    /** Told when the current video finishes or cannot be played. */
    public interface Listener {
        void onFinished();

        void onError(String message);
    }

    private static final String TAG = "VideoRenderer";

    private final Context appContext;
    private final PlayerView playerView;

    private ExoPlayer player;
    private Listener listener;

    public VideoRenderer(Context context, PlayerView playerView) {
        this.appContext = context.getApplicationContext();
        this.playerView = playerView;
    }

    /**
     * Starts playing the given source.
     *
     * @param source   an {@code http(s)://} URL or a local file path (a
     *                 downloaded copy - deciding which to pass is APV-24's job)
     * @param listener callbacks for finish / error
     */
    public void play(String source, Listener listener) {
        this.listener = listener;
        release();

        player = new ExoPlayer.Builder(appContext).build();
        playerView.setPlayer(player);
        playerView.setUseController(false);

        player.setMediaItem(MediaItem.fromUri(toUri(source)));
        player.setRepeatMode(Player.REPEAT_MODE_OFF);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    Log.i(TAG, "Video ended: " + source);
                    notifyFinished();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Video error for " + source + ": " + error.getErrorCodeName(), error);
                notifyError(error.getMessage());
            }
        });

        player.setPlayWhenReady(true);
        player.prepare();
        Log.i(TAG, "Playing video: " + source);
    }

    /** Stops playback and frees the player. Safe to call more than once. */
    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
        playerView.setPlayer(null);
    }

    private void notifyFinished() {
        if (listener != null) {
            listener.onFinished();
        }
    }

    private void notifyError(String message) {
        if (listener != null) {
            listener.onError(message);
        }
    }

    private Uri toUri(String source) {
        if (source == null) {
            return Uri.EMPTY;
        }
        if (source.startsWith("http://") || source.startsWith("https://")) {
            return Uri.parse(source);
        }
        // Anything else is treated as a local file path.
        return Uri.fromFile(new File(source));
    }
}
