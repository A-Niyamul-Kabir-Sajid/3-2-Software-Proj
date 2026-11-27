package edu.kuet.classsync.routine.pattern;

import java.util.List;

/** Composite root spanning Monday through Sunday; {@link #children()} exposes non-empty days. */
public final class WeekRoutine implements RoutineComponent {

    private final List<DayRoutine> allDays;

    public WeekRoutine(List<DayRoutine> allDays) {
        this.allDays = List.copyOf(allDays);
    }

    public List<DayRoutine> nonEmptyDays() {
        return allDays.stream()
                .filter(d -> d.totalSlots() > 0)
                .toList();
    }

    @Override
    public String name() {
        return "WEEK";
    }

    @Override
    public List<RoutineComponent> children() {
        return List.copyOf(nonEmptyDays());
    }

    @Override
    public int totalSlots() {
        return allDays.stream().mapToInt(DayRoutine::totalSlots).sum();
    }
}
