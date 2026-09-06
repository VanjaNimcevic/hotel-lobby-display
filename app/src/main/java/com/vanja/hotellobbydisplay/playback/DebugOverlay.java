package com.vanja.hotellobbydisplay.playback;

import android.view.View;
import android.widget.TextView;

/**
 * Optional corner readout of the current playback state. Controlled by
 * {@link #ENABLED}; when false the view stays GONE and {@link #update} is a no-op.
 */
public class DebugOverlay {

    /** Flip to true to show the overlay. Keep false for normal playback. */
    public static final boolean ENABLED = false;

    private final TextView view;

    public DebugOverlay(TextView view) {
        this.view = view;
        this.view.setVisibility(ENABLED ? View.VISIBLE : View.GONE);
    }

    public void update(String itemId, String type, String source, boolean online,
            String playlistSource) {
        if (!ENABLED) {
            return;
        }
        view.setText("DEBUG\n"
                + "item: " + itemId + "\n"
                + "type: " + type + "\n"
                + "source: " + (source != null ? source : "-") + "\n"
                + "playlist: " + (playlistSource != null ? playlistSource : "-") + "\n"
                + "net: " + (online ? "ONLINE" : "OFFLINE"));
        view.setVisibility(View.VISIBLE);
    }
}
