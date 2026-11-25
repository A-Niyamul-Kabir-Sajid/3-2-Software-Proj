package edu.kuet.classsync.routine.mapper;

import edu.kuet.classsync.routine.dto.RoutineResponse;
import edu.kuet.classsync.routine.dto.RoutineResponse.DaySchedule;
import edu.kuet.classsync.routine.dto.RoutineResponse.SlotItem;
import edu.kuet.classsync.routine.dto.RoutineSlotResponse;
import edu.kuet.classsync.routine.dto.RoutineSlotResponse.SubjectSummary;
import edu.kuet.classsync.routine.dto.RoutineSlotResponse.TeacherSummary;
import edu.kuet.classsync.routine.entity.RoutineSlot;
import edu.kuet.classsync.routine.pattern.DayRoutine;
import edu.kuet.classsync.routine.pattern.WeekRoutine;
import edu.kuet.classsync.subject.entity.Subject;
import edu.kuet.classsync.user.entity.User;
import java.util.List;

public final class RoutineMapper {

    private RoutineMapper() { }

    public static RoutineSlotResponse toSlotResponse(RoutineSlot slot) {
        return new RoutineSlotResponse(
                slot.getId(),
                slot.getDay(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getRoom(),
                toSubjectSummary(slot.getSubject()),
                toTeacherSummary(slot.getTeacher()));
    }

    public static RoutineResponse toRoutineResponse(WeekRoutine week) {
        List<DaySchedule> days = week.nonEmptyDays().stream()
                .map(RoutineMapper::toDaySchedule)
                .toList();
        return new RoutineResponse(days, week.totalSlots());
    }

    private static DaySchedule toDaySchedule(DayRoutine day) {
        List<SlotItem> slots = day.slots().stream()
                .map(leaf -> toSlotItem(leaf.slot()))
                .toList();
        return new DaySchedule(day.day(), slots);
    }

    private static SlotItem toSlotItem(RoutineSlot slot) {
        return new SlotItem(
                slot.getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getRoom(),
                toSubjectSummary(slot.getSubject()),
                toTeacherSummary(slot.getTeacher()));
    }

    private static SubjectSummary toSubjectSummary(Subject subject) {
        return new SubjectSummary(subject.getId(), subject.getCode(), subject.getTitle());
    }

    private static TeacherSummary toTeacherSummary(User teacher) {
        return teacher == null ? null : new TeacherSummary(teacher.getId(), teacher.getName());
    }
}
