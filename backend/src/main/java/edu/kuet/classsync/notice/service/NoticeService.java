package edu.kuet.classsync.notice.service;

import edu.kuet.classsync.auth.CurrentUserProvider;
import edu.kuet.classsync.common.dto.PageResponse;
import edu.kuet.classsync.common.exception.ForbiddenOperationException;
import edu.kuet.classsync.common.exception.ResourceNotFoundException;
import edu.kuet.classsync.notice.dto.NoticeRequest;
import edu.kuet.classsync.notice.dto.NoticeResponse;
import edu.kuet.classsync.notice.entity.Notice;
import edu.kuet.classsync.notice.mapper.NoticeMapper;
import edu.kuet.classsync.notice.repository.NoticeRepository;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Notice CRUD with pinned-first ordering, keyword search, and author/ADMIN ownership checks. */
@Service
public class NoticeService {

    private static final Sort DEFAULT_SORT = Sort.by(
            Order.desc("pinned"),
            Order.desc("createdAt"),
            Order.desc("id"));

    private final NoticeRepository noticeRepository;
    private final CurrentUserProvider currentUser;

    public NoticeService(NoticeRepository noticeRepository, CurrentUserProvider currentUser) {
        this.noticeRepository = noticeRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public PageResponse<NoticeResponse> list(String query, Pageable pageable) {
        Pageable ordered = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
        Page<Notice> page = (query == null || query.isBlank())
                ? noticeRepository.findAllBy(ordered)
                : noticeRepository.search("%" + query.trim().toLowerCase(Locale.ROOT) + "%", ordered);
        return PageResponse.of(page, NoticeMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public NoticeResponse get(Long id) {
        return NoticeMapper.toResponse(find(id));
    }

    @Transactional
    public NoticeResponse create(NoticeRequest request) {
        User author = currentUser.get();
        Notice saved = noticeRepository.save(new Notice(
                request.title().trim(),
                normalizeBody(request.body()),
                request.pinned(),
                author));
        return NoticeMapper.toResponse(saved);
    }

    @Transactional
    public NoticeResponse update(Long id, NoticeRequest request) {
        Notice notice = find(id);
        requireAuthorOrAdmin(notice, currentUser.get());
        notice.setTitle(request.title().trim());
        notice.setBody(normalizeBody(request.body()));
        notice.setPinned(request.pinned());
        return NoticeMapper.toResponse(notice);
    }

    @Transactional
    public void delete(Long id) {
        Notice notice = find(id);
        requireAuthorOrAdmin(notice, currentUser.get());
        noticeRepository.delete(notice);
    }

    private Notice find(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", id));
    }

    private static void requireAuthorOrAdmin(Notice notice, User actor) {
        boolean isAdmin = actor.hasRole(RoleName.ADMIN);
        boolean isAuthor = Objects.equals(notice.getAuthor().getId(), actor.getId());
        if (!isAdmin && !isAuthor) {
            throw new ForbiddenOperationException("Only the notice author or an ADMIN can modify this notice");
        }
    }

    private static String normalizeBody(String body) {
        return body == null ? "" : body.trim();
    }
}
