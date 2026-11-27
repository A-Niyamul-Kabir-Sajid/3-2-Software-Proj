package edu.kuet.classsync.routine.pattern;

import edu.kuet.classsync.routine.entity.RoutineSlot;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds a {@link WeekRoutine} Composite tree from flat {@link RoutineSlot} entities. */
public final class RoutineAssembler {

    private RoutineAssembler() { }

    public static WeekRoutine build(List<RoutineSlot> slots) {
        Map<DayOfWeek, List<SlotLeaf>> byDay = slots.stream()
                .collect(Collectors.groupingBy(
                        RoutineSlot::getDay,
                        Collectors.mapping(SlotLeaf::new, Collectors.toList())));

        List<DayRoutine> days = Arrays.stream(DayOfWeek.values())
                .map(day -> new DayRoutine(day, byDay.getOrDefault(day, List.of())))
                .toList();

        return new WeekRoutine(days);
    }
}
