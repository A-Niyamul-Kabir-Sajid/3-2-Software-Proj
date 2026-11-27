package edu.kuet.classsync.routine.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;

/** Public representation of a single routine slot. */
public record RoutineSlotResponse(
        Long id,
        DayOfWeek day,
        @JsonFormat(pattern = "HH:mm") LocalTime start,
        @JsonFormat(pattern = "HH:mm") LocalTime end,
        String room,
        SubjectSummary subject,
        TeacherSummary teacher) {

    public record SubjectSummary(Long id, String code, String title) { }

    public record TeacherSummary(Long id, String name) { }
}
