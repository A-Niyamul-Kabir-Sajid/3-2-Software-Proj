package edu.kuet.classsync.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Request payload for creating or updating a subject. */
public record SubjectRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 120) String title,
        List<Long> teacherIds) { }
