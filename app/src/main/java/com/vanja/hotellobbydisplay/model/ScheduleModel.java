package com.vanja.hotellobbydisplay.model;

import java.util.List;

/**
 * Optional time rules for a playlist item (see docs/playlist-format.md).
 * Used by TimelineScheduler (APV-19).
 */
public class ScheduleModel {

    /** ISO-8601. Item does not play before this moment. May be {@code null}. */
    public String startAt;

    /** ISO-8601. Item does not play after this moment. May be {@code null}. */
    public String endAt;

    /** Allowed weekdays: 1 = Monday ... 7 = Sunday. */
    public List<Integer> daysOfWeek;

    /** "HH:mm" - earliest time of day the item may play. */
    public String startTime;

    /** "HH:mm" - latest time of day the item may play. */
    public String endTime;
}
