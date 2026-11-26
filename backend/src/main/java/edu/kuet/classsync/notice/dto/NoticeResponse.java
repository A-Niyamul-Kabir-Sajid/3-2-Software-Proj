package edu.kuet.classsync.notice.dto;

import java.time.Instant;

/** Public representation of a class notice. */
public record NoticeResponse(
        Long id,
        String title,
        String body,
        boolean pinned,
        AuthorSummary author,
        Instant createdAt,
        Instant updatedAt) {

    public record AuthorSummary(Long id, String name) { }
}
