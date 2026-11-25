package edu.kuet.classsync.routine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import edu.kuet.classsync.common.exception.BadRequestException;
import edu.kuet.classsync.common.exception.ConflictException;
import edu.kuet.classsync.routine.dto.RoutineSlotRequest;
import edu.kuet.classsync.routine.entity.RoutineSlot;
import edu.kuet.classsync.routine.pattern.RoutineCaretaker;
import edu.kuet.classsync.routine.pattern.RoutineMemento;
import edu.kuet.classsync.routine.pattern.RoutineMemento.SlotSnapshot;
import edu.kuet.classsync.routine.repository.RoutineSlotRepository;
import edu.kuet.classsync.subject.entity.Subject;
import edu.kuet.classsync.subject.repository.SubjectRepository;
import edu.kuet.classsync.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutineServiceTest {

    @Mock RoutineSlotRepository routineSlotRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock UserRepository userRepository;
    @Mock RoutineCaretaker caretaker;
    @Mock CachingRoutineServiceProxy cacheProxy;
    @InjectMocks RoutineService service;

    @Test
    void create_whenOverlapping_throwsConflict() {
        Subject subject = new Subject("CSE 3201", "Software Eng", Set.of());
        subject.setId(1L);
        RoutineSlot existing = new RoutineSlot(
                DayOfWeek.SUNDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), "301", subject, null);
        existing.setId(10L);

        given(routineSlotRepository.findByDay(DayOfWeek.SUNDAY)).willReturn(List.of(existing));

        RoutineSlotRequest overlapping = new RoutineSlotRequest(
                DayOfWeek.SUNDAY, LocalTime.of(8, 30), LocalTime.of(9, 30), "302", 1L, null);

        assertThatThrownBy(() -> service.createSlot(overlapping))
                .isInstanceOf(ConflictException.class);
        verify(routineSlotRepository, never()).save(any());
        verify(caretaker, never()).save(any());
    }

    @Test
    void create_whenEndBeforeStart_throwsBadRequest() {
        RoutineSlotRequest invalid = new RoutineSlotRequest(
                DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(9, 0), "301", 1L, null);

        assertThatThrownBy(() -> service.createSlot(invalid))
                .isInstanceOf(BadRequestException.class);
        verify(routineSlotRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void undo_restoresPreviousSnapshot() {
        Subject subject = new Subject("CSE 3201", "Software Eng", Set.of());
        subject.setId(1L);
        RoutineMemento memento = new RoutineMemento(
                List.of(new SlotSnapshot(
                        DayOfWeek.SUNDAY, LocalTime.of(8, 0), LocalTime.of(8, 50), "301", 1L, null)),
                Instant.now());

        given(caretaker.pop()).willReturn(Optional.of(memento));
        given(subjectRepository.findById(1L)).willReturn(Optional.of(subject));

        service.undo();

        verify(routineSlotRepository).deleteAllInBatch();
        verify(routineSlotRepository).flush();
        ArgumentCaptor<List<RoutineSlot>> captor = ArgumentCaptor.forClass(List.class);
        verify(routineSlotRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        RoutineSlot restored = captor.getValue().get(0);
        assertThat(restored.getDay()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(restored.getRoom()).isEqualTo("301");
        assertThat(restored.getSubject()).isSameAs(subject);
        verify(cacheProxy).invalidate();
    }

    @Test
    void undo_whenEmpty_throwsConflict() {
        given(caretaker.pop()).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.undo())
                .isInstanceOf(ConflictException.class)
                .hasMessage("Nothing to undo");
        verify(routineSlotRepository, never()).deleteAllInBatch();
        verify(routineSlotRepository, never()).saveAll(anyList());
    }
}
