package edu.kuet.classsync.routine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;

/** Request payload for creating or updating a routine slot. */
public record RoutineSlotRequest(
        @NotNull DayOfWeek day,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotBlank @Size(max = 30) String room,
        @NotNull Long subjectId,
        Long teacherId) { }
