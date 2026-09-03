package com.vanja.hotellobbydisplay.model;

/**
 * One item in the playlist.
 *
 * <p>Every item has the same set of fields. The ones a given {@code type} does
 * not need simply stay {@code null} or at their default value
 * (see docs/playlist-format.md). Keeping one flat class instead of a class per
 * type means no inheritance and no {@code instanceof} checks later.</p>
 */
public class PlaylistItemModel {

    /** Unique id of the item. */
    public String id;

    /** One of: VIDEO, IMAGE, TEXT, BANNER, WEB_PAGE, LAYOUT. */
    public String type;

    /** Text to show. Used by TEXT and BANNER. {@code \n} means a new line. */
    public String text;

    /** Media / page address. Used by VIDEO, IMAGE, WEB_PAGE. */
    public String url;

    /** How long to show the item, in seconds. 0 for VIDEO means "play the whole video". */
    public int durationSec;

    /** Play order, smaller number first. */
    public int orderIndex;

    /** {@code false} means skip this item completely. */
    public boolean enabled;

    /** Higher number wins when choosing what to play next. Normal items use 0. */
    public int priority;

    /** {@code true} means interrupt normal playback while enabled and in schedule (APV-26). */
    public boolean isEmergency;

    /** When the item is allowed to play, or {@code null} = always allowed. */
    public ScheduleModel schedule;

    /** Extra per-type options, or {@code null}. */
    public MetadataModel metadata;

    /** Split-screen description, only for {@code type == "LAYOUT"}, otherwise {@code null}. */
    public LayoutModel layout;

    @Override
    public String toString() {
        return "PlaylistItemModel{id='" + id + "', type='" + type
                + "', durationSec=" + durationSec + ", enabled=" + enabled + "}";
    }
}
