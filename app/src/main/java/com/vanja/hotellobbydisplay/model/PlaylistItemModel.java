package com.vanja.hotellobbydisplay.model;

/**
 * One item in the playlist.
 *
 * <p>Every item has the same set of fields. The ones a given {@code type} does
 * not need simply stay {@code null} or at their default value
 * (see docs/playlist-format.md). Keeping one flat class instead of a class per
 * type means no inheritance and no {@code instanceof} checks later.</p>
 *
 * <p>Fields are private with public getters; Gson sets them by reflection.</p>
 */
public class PlaylistItemModel {

    /** Unique id of the item. */
    private String id;

    /** One of: VIDEO, IMAGE, TEXT, BANNER, WEB_PAGE, LAYOUT. */
    private String type;

    /** Text to show. Used by TEXT and BANNER. {@code \n} means a new line. */
    private String text;

    /** Media / page address. Used by VIDEO, IMAGE, WEB_PAGE. */
    private String url;

    /** How long to show the item, in seconds. 0 for VIDEO means "play the whole video". */
    private int durationSec;

    /** Play order, smaller number first. */
    private int orderIndex;

    /** {@code false} means skip this item completely. */
    private boolean enabled;

    /** Higher number wins when choosing what to play next. Normal items use 0. */
    private int priority;

    /** {@code true} means interrupt normal playback while enabled and in schedule (APV-26). */
    private boolean isEmergency;

    /** When the item is allowed to play, or {@code null} = always allowed. */
    private ScheduleModel schedule;

    /** Extra per-type options, or {@code null}. */
    private MetadataModel metadata;

    /** Split-screen description, only for {@code type == "LAYOUT"}, otherwise {@code null}. */
    private LayoutModel layout;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }

    public int getDurationSec() {
        return durationSec;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isEmergency() {
        return isEmergency;
    }

    public ScheduleModel getSchedule() {
        return schedule;
    }

    public MetadataModel getMetadata() {
        return metadata;
    }

    public LayoutModel getLayout() {
        return layout;
    }

    @Override
    public String toString() {
        return "PlaylistItemModel{id='" + id + "', type='" + type
                + "', durationSec=" + durationSec + ", enabled=" + enabled + "}";
    }
}
