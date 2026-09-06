package com.vanja.hotellobbydisplay.playback;

import android.util.Log;

import com.vanja.hotellobbydisplay.data.local.PlaylistItemEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Picks which playlist item should play right now. Each {@link #getNextItem()}:
 * filters items by schedule, returns an eligible emergency item if there is one,
 * otherwise rotates through the rest (priority first, then orderIndex, looping).
 * Pure in-memory - safe to call on the main thread.
 */
public class TimelineScheduler {

    private static final String TAG = "TimelineScheduler";

    private final List<PlaylistItemEntity> items;
    private int normalIndex = 0;

    public TimelineScheduler(List<PlaylistItemEntity> items) {
        this.items = items;
    }

    /** @return the next item, or null if nothing is currently eligible. */
    public PlaylistItemEntity getNextItem() {
        List<PlaylistItemEntity> eligible = eligibleNow();
        if (eligible.isEmpty()) {
            return null;
        }

        // An eligible emergency item takes over completely: returned every cycle
        // until it stops being eligible, and the normal cursor is left untouched
        // so normal playback resumes where it was.
        for (PlaylistItemEntity item : eligible) {
            if (item.isEmergency()) {
                return item;
            }
        }

        // Priority > 0 just sorts to the front of the loop; it does not starve
        // the rest.
        List<PlaylistItemEntity> rotation = new ArrayList<>(eligible);
        rotation.sort(Comparator
                .comparingInt(PlaylistItemEntity::getPriority).reversed()
                .thenComparingInt(PlaylistItemEntity::getOrderIndex));

        if (normalIndex >= rotation.size()) {
            normalIndex = 0; // eligible set shrank since last call
        }
        PlaylistItemEntity item = rotation.get(normalIndex);
        normalIndex = (normalIndex + 1) % rotation.size();
        return item;
    }

    private List<PlaylistItemEntity> eligibleNow() {
        List<PlaylistItemEntity> result = new ArrayList<>();
        for (PlaylistItemEntity item : items) {
            if (item.isEnabled() && isWithinSchedule(item)) {
                result.add(item);
            }
        }
        return result;
    }

    private boolean isWithinSchedule(PlaylistItemEntity item) {
        Instant startAt = parseInstantOrNull(item.getScheduleStartAt());
        if (startAt != null && Instant.now().isBefore(startAt)) {
            return false;
        }
        Instant endAt = parseInstantOrNull(item.getScheduleEndAt());
        if (endAt != null && Instant.now().isAfter(endAt)) {
            return false;
        }
        if (!isTodayAllowed(item.getScheduleDaysOfWeek())) {
            return false;
        }
        return isWithinTimeOfDay(item.getScheduleStartTime(), item.getScheduleEndTime());
    }

    /** {@code daysCsv} = "1,2,3,4,5"; 1 = Monday ... 7 = Sunday. Empty = no restriction. */
    private boolean isTodayAllowed(String daysCsv) {
        if (daysCsv == null || daysCsv.trim().isEmpty()) {
            return true;
        }
        int today = LocalDate.now().getDayOfWeek().getValue();
        for (String part : daysCsv.split(",")) {
            try {
                if (Integer.parseInt(part.trim()) == today) {
                    return true;
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Bad weekday value '" + part + "'");
            }
        }
        return false;
    }

    private boolean isWithinTimeOfDay(String startTime, String endTime) {
        if (startTime == null || endTime == null) {
            return true;
        }
        LocalTime start = parseTimeOrNull(startTime);
        LocalTime end = parseTimeOrNull(endTime);
        if (start == null || end == null) {
            return true; // bad data must not block playback
        }
        LocalTime now = LocalTime.now();
        if (!start.isAfter(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        }
        // Window crosses midnight, e.g. 22:00 - 06:00.
        return !now.isBefore(start) || !now.isAfter(end);
    }

    private Instant parseInstantOrNull(String iso) {
        if (iso == null) {
            return null;
        }
        try {
            return Instant.parse(iso);
        } catch (DateTimeParseException e) {
            Log.w(TAG, "Bad schedule date '" + iso + "'");
            return null;
        }
    }

    private LocalTime parseTimeOrNull(String hhmm) {
        try {
            return LocalTime.parse(hhmm);
        } catch (DateTimeParseException e) {
            Log.w(TAG, "Bad schedule time '" + hhmm + "'");
            return null;
        }
    }
}
