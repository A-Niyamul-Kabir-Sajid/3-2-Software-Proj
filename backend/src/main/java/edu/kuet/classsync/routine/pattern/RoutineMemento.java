package edu.kuet.classsync.routine.pattern;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

/** Memento pattern: immutable snapshot of all routine slots at a point in time. */
public final class RoutineMemento {

    private final List<SlotSnapshot> slots;
    private final Instant capturedAt;

    public RoutineMemento(List<SlotSnapshot> slots, Instant capturedAt) {
        this.slots = List.copyOf(slots);
        this.capturedAt = capturedAt;
    }

    public List<SlotSnapshot> slots() {
        return slots;
    }

    public Instant capturedAt() {
        return capturedAt;
    }

    /** Immutable value copy of a {@link edu.kuet.classsync.routine.entity.RoutineSlot}. */
    public record SlotSnapshot(
            DayOfWeek day,
            LocalTime startTime,
            LocalTime endTime,
            String room,
            Long subjectId,
            Long teacherId) { }
}
