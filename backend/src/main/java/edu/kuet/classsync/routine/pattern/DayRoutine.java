package edu.kuet.classsync.routine.pattern;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;

/** Composite node representing one day's slots sorted by start time. */
public final class DayRoutine implements RoutineComponent {

    private final DayOfWeek day;
    private final List<SlotLeaf> slots;

    public DayRoutine(DayOfWeek day, List<SlotLeaf> slots) {
        this.day = day;
        this.slots = slots.stream()
                .sorted(Comparator.comparing(leaf -> leaf.slot().getStartTime()))
                .toList();
    }

    public DayOfWeek day() {
        return day;
    }

    public List<SlotLeaf> slots() {
        return slots;
    }

    @Override
    public String name() {
        return day.name();
    }

    @Override
    public List<RoutineComponent> children() {
        return List.copyOf(slots);
    }

    @Override
    public int totalSlots() {
        return slots.size();
    }
}
