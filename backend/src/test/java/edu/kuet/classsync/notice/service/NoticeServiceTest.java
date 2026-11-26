package edu.kuet.classsync.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import edu.kuet.classsync.auth.CurrentUserProvider;
import edu.kuet.classsync.common.exception.ForbiddenOperationException;
import edu.kuet.classsync.notice.dto.NoticeRequest;
import edu.kuet.classsync.notice.dto.NoticeResponse;
import edu.kuet.classsync.notice.entity.Notice;
import edu.kuet.classsync.notice.repository.NoticeRepository;
import edu.kuet.classsync.user.entity.Role;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock NoticeRepository noticeRepository;
    @Mock CurrentUserProvider currentUser;
    @InjectMocks NoticeService service;

    private static User user(long id, RoleName role) {
        User u = User.builder().name("User " + id).email("u" + id + "@kuet.ac.bd")
                .passwordHash("h").role(new Role(role)).build();
        u.setId(id);
        return u;
    }

    @Test
    void update_byNonAuthor_throwsForbidden() {
        User author = user(1L, RoleName.CR);
        User otherTeacher = user(2L, RoleName.TEACHER);
        Notice existing = new Notice("Old title", "Old body", false, author);
        existing.setId(10L);

        given(noticeRepository.findById(10L)).willReturn(Optional.of(existing));
        given(currentUser.get()).willReturn(otherTeacher);

        assertThatThrownBy(() -> service.update(10L, new NoticeRequest("New", "Body", true)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(existing.getTitle()).isEqualTo("Old title");
    }

    @Test
    void update_byAuthor_updatesNotice() {
        User author = user(1L, RoleName.TEACHER);
        Notice existing = new Notice("Old title", "Old body", false, author);
        existing.setId(10L);

        given(noticeRepository.findById(10L)).willReturn(Optional.of(existing));
        given(currentUser.get()).willReturn(author);

        NoticeResponse res = service.update(10L, new NoticeRequest(" Updated ", " New body ", true));

        assertThat(res.title()).isEqualTo("Updated");
        assertThat(res.body()).isEqualTo("New body");
        assertThat(res.pinned()).isTrue();
    }

    @Test
    void update_byAdmin_allowsModifyingOthersNotice() {
        User author = user(1L, RoleName.CR);
        User admin = user(99L, RoleName.ADMIN);
        Notice existing = new Notice("Old", "Body", false, author);
        existing.setId(10L);

        given(noticeRepository.findById(10L)).willReturn(Optional.of(existing));
        given(currentUser.get()).willReturn(admin);

        NoticeResponse res = service.update(10L, new NoticeRequest("Moderated", "Edited", false));

        assertThat(res.title()).isEqualTo("Moderated");
    }

    @Test
    void delete_byNonAuthor_throwsForbidden() {
        User author = user(1L, RoleName.CR);
        User otherCr = user(2L, RoleName.CR);
        Notice existing = new Notice("Title", "Body", false, author);
        existing.setId(10L);

        given(noticeRepository.findById(10L)).willReturn(Optional.of(existing));
        given(currentUser.get()).willReturn(otherCr);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(noticeRepository, never()).delete(any());
    }

    @Test
    void create_setsCurrentUserAsAuthor() {
        User cr = user(5L, RoleName.CR);
        given(currentUser.get()).willReturn(cr);
        given(noticeRepository.save(any(Notice.class))).willAnswer(inv -> {
            Notice n = inv.getArgument(0);
            n.setId(22L);
            return n;
        });

        NoticeResponse res = service.create(new NoticeRequest(" Exam ", " Starts Monday ", true));

        assertThat(res.id()).isEqualTo(22L);
        assertThat(res.title()).isEqualTo("Exam");
        assertThat(res.pinned()).isTrue();
        assertThat(res.author().id()).isEqualTo(5L);
    }
}
