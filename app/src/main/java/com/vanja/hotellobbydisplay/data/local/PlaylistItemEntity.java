package com.vanja.hotellobbydisplay.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room table row for one playlist item ("playlist_items" table).
 *
 * <p>Mirrors {@link com.vanja.hotellobbydisplay.model.PlaylistItemModel}. Room
 * columns can only be simple types, so the nested JSON objects are flattened:</p>
 * <ul>
 *   <li>{@code schedule.*} becomes the {@code schedule...} columns; the
 *       {@code daysOfWeek} list is stored as a comma-separated string
 *       (e.g. {@code "1,2,3,4,5"}).</li>
 *   <li>{@code metadata.*} becomes the {@code metadata...} columns.</li>
 *   <li>{@code layout} (bonus feature) is kept as its raw JSON text in
 *       {@code layoutJson} and parsed on demand.</li>
 * </ul>
 *
 * <p>The remote {@code url} is the key used to look up a downloaded local copy
 * in {@link MediaCacheEntity}, so there is no local-path column here.</p>
 */
@Entity(tableName = "playlist_items")
public class PlaylistItemEntity {

    /** From JSON "id". Primary key. */
    @PrimaryKey
    @NonNull
    private String id = "";

    /** Which playlist this item belongs to ({@link PlaylistEntity#getPlaylistId()}). */
    private String playlistId;

    // --- core content ---

    /** VIDEO, IMAGE, TEXT, BANNER, WEB_PAGE, LAYOUT. */
    private String type;
    private String text;
    private String url;
    private int durationSec;
    private int orderIndex;
    private boolean enabled;
    private int priority;
    private boolean isEmergency;

    // --- schedule (null / empty when the item has no schedule) ---

    private String scheduleStartAt;
    private String scheduleEndAt;
    /** Comma-separated weekday numbers, 1 = Monday ... 7 = Sunday. E.g. "1,2,3,4,5". */
    private String scheduleDaysOfWeek;
    private String scheduleStartTime;
    private String scheduleEndTime;

    // --- metadata ---

    private String metadataBannerPosition;
    private boolean metadataJavascriptEnabled;
    private String metadataScaleType;

    // --- layout (bonus) ---

    /** Raw JSON of the "layout" object, or null. Parsed only if LAYOUT is supported. */
    private String layoutJson;

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(int durationSec) {
        this.durationSec = durationSec;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEmergency() {
        return isEmergency;
    }

    public void setEmergency(boolean emergency) {
        this.isEmergency = emergency;
    }

    public String getScheduleStartAt() {
        return scheduleStartAt;
    }

    public void setScheduleStartAt(String scheduleStartAt) {
        this.scheduleStartAt = scheduleStartAt;
    }

    public String getScheduleEndAt() {
        return scheduleEndAt;
    }

    public void setScheduleEndAt(String scheduleEndAt) {
        this.scheduleEndAt = scheduleEndAt;
    }

    public String getScheduleDaysOfWeek() {
        return scheduleDaysOfWeek;
    }

    public void setScheduleDaysOfWeek(String scheduleDaysOfWeek) {
        this.scheduleDaysOfWeek = scheduleDaysOfWeek;
    }

    public String getScheduleStartTime() {
        return scheduleStartTime;
    }

    public void setScheduleStartTime(String scheduleStartTime) {
        this.scheduleStartTime = scheduleStartTime;
    }

    public String getScheduleEndTime() {
        return scheduleEndTime;
    }

    public void setScheduleEndTime(String scheduleEndTime) {
        this.scheduleEndTime = scheduleEndTime;
    }

    public String getMetadataBannerPosition() {
        return metadataBannerPosition;
    }

    public void setMetadataBannerPosition(String metadataBannerPosition) {
        this.metadataBannerPosition = metadataBannerPosition;
    }

    public boolean isMetadataJavascriptEnabled() {
        return metadataJavascriptEnabled;
    }

    public void setMetadataJavascriptEnabled(boolean metadataJavascriptEnabled) {
        this.metadataJavascriptEnabled = metadataJavascriptEnabled;
    }

    public String getMetadataScaleType() {
        return metadataScaleType;
    }

    public void setMetadataScaleType(String metadataScaleType) {
        this.metadataScaleType = metadataScaleType;
    }

    public String getLayoutJson() {
        return layoutJson;
    }

    public void setLayoutJson(String layoutJson) {
        this.layoutJson = layoutJson;
    }
}
