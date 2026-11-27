package edu.kuet.classsync.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payload for publishing or editing a notice. */
public record NoticeRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 5000) String body,
        boolean pinned) { }
