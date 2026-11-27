package edu.kuet.classsync.notice.mapper;

import edu.kuet.classsync.notice.dto.NoticeResponse;
import edu.kuet.classsync.notice.dto.NoticeResponse.AuthorSummary;
import edu.kuet.classsync.notice.entity.Notice;
import edu.kuet.classsync.user.entity.User;

public final class NoticeMapper {

    private NoticeMapper() { }

    public static NoticeResponse toResponse(Notice notice) {
        User author = notice.getAuthor();
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getBody(),
                notice.isPinned(),
                new AuthorSummary(author.getId(), author.getName()),
                notice.getCreatedAt(),
                notice.getUpdatedAt());
    }
}
