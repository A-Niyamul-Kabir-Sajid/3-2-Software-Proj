package edu.kuet.classsync.subject.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import edu.kuet.classsync.common.exception.BadRequestException;
import edu.kuet.classsync.common.exception.ConflictException;
import edu.kuet.classsync.common.exception.ResourceNotFoundException;
import edu.kuet.classsync.routine.repository.RoutineSlotRepository;
import edu.kuet.classsync.subject.dto.SubjectRequest;
import edu.kuet.classsync.subject.dto.SubjectResponse;
import edu.kuet.classsync.subject.entity.Subject;
import edu.kuet.classsync.subject.repository.SubjectRepository;
import edu.kuet.classsync.user.entity.Role;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import edu.kuet.classsync.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock SubjectRepository subjectRepository;
    @Mock UserRepository userRepository;
    @Mock RoutineSlotRepository routineSlotRepository;
    @InjectMocks SubjectService service;

    private static User user(long id, RoleName role) {
        User u = User.builder().name("User " + id).email("u" + id + "@kuet.ac.bd")
                .passwordHash("h").role(new Role(role)).build();
        u.setId(id);
        return u;
    }

    @Test
    void create_duplicateCode_throwsConflict() {
        given(subjectRepository.existsByCodeIgnoreCase("CSE 3201")).willReturn(true);

        assertThatThrownBy(() -> service.create(new SubjectRequest(" CSE 3201 ", "Software Eng", List.of())))
                .isInstanceOf(ConflictException.class);
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void create_nonTeacherUser_throwsBadRequest() {
        User student = user(5L, RoleName.STUDENT);
        given(subjectRepository.existsByCodeIgnoreCase("CSE 3201")).willReturn(false);
        given(userRepository.findAllById(Set.of(5L))).willReturn(List.of(student));

        assertThatThrownBy(() -> service.create(new SubjectRequest("CSE 3201", "Software Eng", List.of(5L))))
                .isInstanceOf(BadRequestException.class);
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void create_withValidTeacher_savesAndReturnsResponse() {
        User teacher = user(3L, RoleName.TEACHER);
        given(subjectRepository.existsByCodeIgnoreCase("CSE 3201")).willReturn(false);
        given(userRepository.findAllById(Set.of(3L))).willReturn(List.of(teacher));
        given(subjectRepository.save(any(Subject.class))).willAnswer(inv -> {
            Subject s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        SubjectResponse res = service.create(new SubjectRequest("CSE 3201", "Software Eng", List.of(3L)));

        assertThat(res.id()).isEqualTo(10L);
        assertThat(res.code()).isEqualTo("CSE 3201");
        assertThat(res.teachers()).extracting(SubjectResponse.TeacherSummary::id).containsExactly(3L);
    }

    @Test
    void delete_whenReferencedByRoutineSlot_throwsConflict() {
        Subject subject = new Subject("CSE 3201", "Software Eng", Set.of());
        subject.setId(7L);
        given(subjectRepository.findById(7L)).willReturn(Optional.of(subject));
        given(routineSlotRepository.existsBySubjectId(7L)).willReturn(true);

        assertThatThrownBy(() -> service.delete(7L))
                .isInstanceOf(ConflictException.class);
        verify(subjectRepository, never()).delete(any());
    }

    @Test
    void get_missingSubject_throwsNotFound() {
        given(subjectRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
