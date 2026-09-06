package com.vanja.hotellobbydisplay.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * "playlist_items" table. Nested JSON objects are flattened into columns:
 * schedule.* -> schedule... (daysOfWeek as a CSV string), metadata.* ->
 * metadata..., layout -> layoutJson (raw JSON, parsed on demand). The local
 * copy of a media file is looked up by {@code url} in {@link MediaCacheEntity}.
 */
@Entity(tableName = "playlist_items")
public class PlaylistItemEntity {

    @PrimaryKey
    @NonNull
    private String id = "";
    private String playlistId;

    private String type;
    private String text;
    private String url;
    private int durationSec;
    private int orderIndex;
    private boolean enabled;
    private int priority;
    private boolean isEmergency;

    private String scheduleStartAt;
    private String scheduleEndAt;
    /** CSV weekday numbers, 1 = Monday ... 7 = Sunday. */
    private String scheduleDaysOfWeek;
    private String scheduleStartTime;
    private String scheduleEndTime;

    private String metadataBannerPosition;
    private boolean metadataJavascriptEnabled;
    private String metadataScaleType;

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
