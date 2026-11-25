package edu.kuet.classsync.routine.pattern;

import java.util.List;

/** Composite pattern component for the weekly routine tree (Week -> Day -> Slot). */
public interface RoutineComponent {

    String name();

    List<RoutineComponent> children();

    int totalSlots();
}
