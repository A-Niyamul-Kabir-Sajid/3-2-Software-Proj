package edu.kuet.classsync.routine.pattern;

import static org.assertj.core.api.Assertions.assertThat;

import edu.kuet.classsync.routine.entity.RoutineSlot;
import edu.kuet.classsync.subject.entity.Subject;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoutineAssemblerTest {

    @Test
    void build_groupsSlotsByDay_sortedByStart() {
        Subject dbms = new Subject("CSE 3205", "DBMS", Set.of());
        dbms.setId(1L);
        Subject se = new Subject("CSE 3201", "Software Eng", Set.of());
        se.setId(2L);

        RoutineSlot sunLate = new RoutineSlot(
                DayOfWeek.SUNDAY, LocalTime.of(10, 0), LocalTime.of(10, 50), "301", dbms, null);
        RoutineSlot sunEarly = new RoutineSlot(
                DayOfWeek.SUNDAY, LocalTime.of(8, 0), LocalTime.of(8, 50), "301", se, null);
        RoutineSlot monSlot = new RoutineSlot(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 50), "204", dbms, null);

        WeekRoutine week = RoutineAssembler.build(List.of(sunLate, monSlot, sunEarly));

        assertThat(week.totalSlots()).isEqualTo(3);
        assertThat(week.nonEmptyDays()).extracting(DayRoutine::day)
                .containsExactly(DayOfWeek.MONDAY, DayOfWeek.SUNDAY);
        DayRoutine sunday = week.nonEmptyDays().get(1);
        assertThat(sunday.slots())
                .extracting(leaf -> leaf.slot().getStartTime())
                .containsExactly(LocalTime.of(8, 0), LocalTime.of(10, 0));
    }
}
