package com.vanja.hotellobbydisplay.model;

/**
 * One playlist item. All types share this flat shape; unused fields stay null.
 * See docs/playlist-format.md.
 */
public class PlaylistItemModel {

    private String id;
    /** VIDEO, IMAGE, TEXT, BANNER, WEB_PAGE or LAYOUT. */
    private String type;
    private String text;
    private String url;
    /** Seconds to show the item; 0 for VIDEO means "the whole video". */
    private int durationSec;
    private int orderIndex;
    private boolean enabled;
    private int priority;
    private boolean isEmergency;
    /** Time rules, or null = always allowed. */
    private ScheduleModel schedule;
    private MetadataModel metadata;
    /** Only for type LAYOUT. */
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
