package edu.kuet.classsync.routine.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.kuet.classsync.routine.dto.RoutineSlotResponse.SubjectSummary;
import edu.kuet.classsync.routine.dto.RoutineSlotResponse.TeacherSummary;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/** Composite weekly routine response grouped by day. */
public record RoutineResponse(List<DaySchedule> days, int totalSlots) {

    public record DaySchedule(DayOfWeek day, List<SlotItem> slots) { }

    public record SlotItem(
            Long id,
            @JsonFormat(pattern = "HH:mm") LocalTime start,
            @JsonFormat(pattern = "HH:mm") LocalTime end,
            String room,
            SubjectSummary subject,
            TeacherSummary teacher) { }
}
