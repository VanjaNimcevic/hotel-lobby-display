package com.vanja.hotellobbydisplay.model;

import java.util.List;

/**
 * Optional time rules for a playlist item (see docs/playlist-format.md).
 * Used by TimelineScheduler (APV-19).
 *
 * <p>Private fields with public getters; Gson sets them by reflection.</p>
 */
public class ScheduleModel {

    /** ISO-8601. Item does not play before this moment. May be {@code null}. */
    private String startAt;

    /** ISO-8601. Item does not play after this moment. May be {@code null}. */
    private String endAt;

    /** Allowed weekdays: 1 = Monday ... 7 = Sunday. */
    private List<Integer> daysOfWeek;

    /** "HH:mm" - earliest time of day the item may play. */
    private String startTime;

    /** "HH:mm" - latest time of day the item may play. */
    private String endTime;

    public String getStartAt() {
        return startAt;
    }

    public String getEndAt() {
        return endAt;
    }

    public List<Integer> getDaysOfWeek() {
        return daysOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }
}
