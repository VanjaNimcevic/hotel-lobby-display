package com.vanja.hotellobbydisplay.playback;

import android.view.View;
import android.widget.TextView;

/**
 * Optional on-screen debug readout (APV-27): current item id, type, source
 * (LOCAL / REMOTE / ASSETS) and online/offline state, in a corner of the screen.
 *
 * <p>Controlled entirely by the {@link #ENABLED} constant. When it is
 * {@code false} (the default) the overlay view stays {@code GONE} and
 * {@link #update} does nothing - zero cost for production-like playback. Flip
 * it to {@code true} while debugging on a device.</p>
 */
public class DebugOverlay {

    /** Flip to {@code true} to show the overlay. Keep {@code false} for normal playback. */
    public static final boolean ENABLED = true;

    private final TextView view;

    public DebugOverlay(TextView view) {
        this.view = view;
        this.view.setVisibility(ENABLED ? View.VISIBLE : View.GONE);
    }

    /**
     * @param itemId         id of the item now playing
     * @param type           its content type (VIDEO, IMAGE, ...)
     * @param source         "LOCAL", "REMOTE" or "ASSETS" (may be null)
     * @param online         current network state
     * @param playlistSource where the playlist itself came from (REMOTE / ASSETS / ROOM)
     */
    public void update(String itemId, String type, String source, boolean online,
            String playlistSource) {
        if (!ENABLED) {
            return;
        }
        String text = "DEBUG\n"
                + "item: " + itemId + "\n"
                + "type: " + type + "\n"
                + "source: " + (source != null ? source : "-") + "\n"
                + "playlist: " + (playlistSource != null ? playlistSource : "-") + "\n"
                + "net: " + (online ? "ONLINE" : "OFFLINE");
        view.setText(text);
        view.setVisibility(View.VISIBLE);
    }
}
