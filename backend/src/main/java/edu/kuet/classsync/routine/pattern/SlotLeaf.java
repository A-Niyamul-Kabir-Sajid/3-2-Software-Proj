package edu.kuet.classsync.routine.pattern;

import edu.kuet.classsync.routine.entity.RoutineSlot;
import java.util.List;

/** Composite leaf wrapping a single {@link RoutineSlot}. */
public final class SlotLeaf implements RoutineComponent {

    private final RoutineSlot slot;

    public SlotLeaf(RoutineSlot slot) {
        this.slot = slot;
    }

    public RoutineSlot slot() {
        return slot;
    }

    @Override
    public String name() {
        return slot.getSubject().getCode() + " " + slot.getStartTime();
    }

    @Override
    public List<RoutineComponent> children() {
        return List.of();
    }

    @Override
    public int totalSlots() {
        return 1;
    }
}
