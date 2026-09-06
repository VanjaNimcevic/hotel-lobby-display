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
 * Picks which playlist item should play right now.
 *
 * <p>Given the enabled items of a playlist (from
 * {@link com.vanja.hotellobbydisplay.data.PlaylistRepository}), every call to
 * {@link #getNextItem()}:</p>
 * <ol>
 *   <li>works out which items are currently allowed to play, based on their
 *       schedule (startAt/endAt, allowed weekdays, startTime/endTime);</li>
 *   <li>returns an eligible emergency or priority item first, if there is one;</li>
 *   <li>otherwise returns the next normal item, rotating through orderIndex
 *       and looping back to the start once the end is reached.</li>
 * </ol>
 *
 * <p>Pure in-memory logic, no I/O - safe to call from the main thread as often
 * as needed.</p>
 */
public class TimelineScheduler {

    private static final String TAG = "TimelineScheduler";

    private final List<PlaylistItemEntity> items;

    /** Position in the normal (non-emergency, non-priority) rotation. */
    private int normalIndex = 0;

    public TimelineScheduler(List<PlaylistItemEntity> items) {
        this.items = items;
    }

    /**
     * @return the next item to play right now, or {@code null} if nothing is
     *         currently eligible (e.g. every item is outside its schedule)
     */
    public PlaylistItemEntity getNextItem() {
        List<PlaylistItemEntity> eligible = eligibleNow();
        if (eligible.isEmpty()) {
            return null;
        }

        // APV-26: an eligible EMERGENCY item takes over completely - it is
        // returned every cycle, so nothing else plays, until it stops being
        // eligible (disabled, or its startAt/endAt window closed), at which
        // point the normal rotation simply resumes where it left off (the
        // rotation cursor is not touched here).
        for (PlaylistItemEntity item : eligible) {
            if (item.isEmergency()) {
                return item;
            }
        }

        // Normal rotation: higher priority first, then orderIndex; loops forever.
        // A non-emergency item with priority > 0 just sorts to the front of each
        // loop - it still shares the rotation, it does not starve the rest.
        List<PlaylistItemEntity> rotation = new ArrayList<>(eligible);
        rotation.sort(Comparator
                .comparingInt(PlaylistItemEntity::getPriority).reversed()
                .thenComparingInt(PlaylistItemEntity::getOrderIndex));

        if (normalIndex >= rotation.size()) {
            // The eligible set can shrink between calls (e.g. an item's time
            // window just closed) - clamp instead of throwing.
            normalIndex = 0;
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

    /** {@code daysCsv} looks like {@code "1,2,3,4,5"}; 1 = Monday ... 7 = Sunday. */
    private boolean isTodayAllowed(String daysCsv) {
        if (daysCsv == null || daysCsv.trim().isEmpty()) {
            return true; // no restriction
        }
        int today = LocalDate.now().getDayOfWeek().getValue();
        for (String part : daysCsv.split(",")) {
            try {
                if (Integer.parseInt(part.trim()) == today) {
                    return true;
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Bad weekday value '" + part + "' in '" + daysCsv + "', skipping it");
            }
        }
        return false;
    }

    private boolean isWithinTimeOfDay(String startTime, String endTime) {
        if (startTime == null || endTime == null) {
            return true; // no restriction
        }
        LocalTime start = parseTimeOrNull(startTime);
        LocalTime end = parseTimeOrNull(endTime);
        if (start == null || end == null) {
            return true; // bad data - do not block playback because of it
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
            Log.w(TAG, "Bad schedule date '" + iso + "', ignoring this bound", e);
            return null;
        }
    }

    private LocalTime parseTimeOrNull(String hhmm) {
        try {
            return LocalTime.parse(hhmm);
        } catch (DateTimeParseException e) {
            Log.w(TAG, "Bad schedule time '" + hhmm + "', ignoring this bound", e);
            return null;
        }
    }
}
