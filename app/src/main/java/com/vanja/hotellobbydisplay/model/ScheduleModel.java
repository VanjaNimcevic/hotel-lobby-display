package com.vanja.hotellobbydisplay.model;

import java.util.List;

/** Optional time rules for a playlist item. Used by TimelineScheduler. */
public class ScheduleModel {

    /** ISO-8601, or null. */
    private String startAt;
    /** ISO-8601, or null. */
    private String endAt;
    /** 1 = Monday ... 7 = Sunday. */
    private List<Integer> daysOfWeek;
    /** "HH:mm". */
    private String startTime;
    /** "HH:mm". */
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
