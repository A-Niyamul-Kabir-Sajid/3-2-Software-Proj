package edu.kuet.classsync.routine.service;

import edu.kuet.classsync.common.exception.BadRequestException;
import edu.kuet.classsync.common.exception.ConflictException;
import edu.kuet.classsync.common.exception.ResourceNotFoundException;
import edu.kuet.classsync.routine.dto.RoutineSlotRequest;
import edu.kuet.classsync.routine.dto.RoutineSlotResponse;
import edu.kuet.classsync.routine.entity.RoutineSlot;
import edu.kuet.classsync.routine.mapper.RoutineMapper;
import edu.kuet.classsync.routine.pattern.RoutineCaretaker;
import edu.kuet.classsync.routine.pattern.RoutineMemento;
import edu.kuet.classsync.routine.pattern.RoutineMemento.SlotSnapshot;
import edu.kuet.classsync.routine.repository.RoutineSlotRepository;
import edu.kuet.classsync.subject.entity.Subject;
import edu.kuet.classsync.subject.repository.SubjectRepository;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import edu.kuet.classsync.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Routine slot CRUD with overlap validation, Memento undo snapshots, and cache invalidation. */
@Service
public class RoutineService {

    private final RoutineSlotRepository routineSlotRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final RoutineCaretaker caretaker;
    private final CachingRoutineServiceProxy cacheProxy;

    public RoutineService(RoutineSlotRepository routineSlotRepository,
                          SubjectRepository subjectRepository,
                          UserRepository userRepository,
                          RoutineCaretaker caretaker,
                          CachingRoutineServiceProxy cacheProxy) {
        this.routineSlotRepository = routineSlotRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.caretaker = caretaker;
        this.cacheProxy = cacheProxy;
    }

    @Transactional(readOnly = true)
    public RoutineSlotResponse getSlot(Long id) {
        return RoutineMapper.toSlotResponse(findSlot(id));
    }

    @Transactional
    public RoutineSlotResponse createSlot(RoutineSlotRequest request) {
        validateTimeRange(request.startTime(), request.endTime());
        checkOverlap(request.day(), request.startTime(), request.endTime(), null);
        Subject subject = findSubject(request.subjectId());
        User teacher = resolveTeacher(request.teacherId());

        caretaker.save(snapshot());
        RoutineSlot saved = routineSlotRepository.save(new RoutineSlot(
                request.day(),
                request.startTime(),
                request.endTime(),
                request.room().trim(),
                subject,
                teacher));
        cacheProxy.invalidate();
        return RoutineMapper.toSlotResponse(saved);
    }

    @Transactional
    public RoutineSlotResponse updateSlot(Long id, RoutineSlotRequest request) {
        RoutineSlot slot = findSlot(id);
        validateTimeRange(request.startTime(), request.endTime());
        checkOverlap(request.day(), request.startTime(), request.endTime(), id);
        Subject subject = findSubject(request.subjectId());
        User teacher = resolveTeacher(request.teacherId());

        caretaker.save(snapshot());
        slot.setDay(request.day());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setRoom(request.room().trim());
        slot.setSubject(subject);
        slot.setTeacher(teacher);
        cacheProxy.invalidate();
        return RoutineMapper.toSlotResponse(slot);
    }

    @Transactional
    public void deleteSlot(Long id) {
        RoutineSlot slot = findSlot(id);
        caretaker.save(snapshot());
        routineSlotRepository.delete(slot);
        cacheProxy.invalidate();
    }

    @Transactional
    public void undo() {
        RoutineMemento memento = caretaker.pop()
                .orElseThrow(() -> new ConflictException("Nothing to undo"));
        routineSlotRepository.deleteAllInBatch();
        routineSlotRepository.flush();
        if (!memento.slots().isEmpty()) {
            List<RoutineSlot> restored = memento.slots().stream()
                    .map(s -> new RoutineSlot(
                            s.day(),
                            s.startTime(),
                            s.endTime(),
                            s.room(),
                            findSubject(s.subjectId()),
                            s.teacherId() == null ? null : findUser(s.teacherId())))
                    .toList();
            routineSlotRepository.saveAll(restored);
        }
        cacheProxy.invalidate();
    }

    private RoutineMemento snapshot() {
        List<SlotSnapshot> copies = routineSlotRepository.findAllByOrderByDayAscStartTimeAsc().stream()
                .map(s -> new SlotSnapshot(
                        s.getDay(),
                        s.getStartTime(),
                        s.getEndTime(),
                        s.getRoom(),
                        s.getSubject().getId(),
                        s.getTeacher() == null ? null : s.getTeacher().getId()))
                .toList();
        return new RoutineMemento(copies, Instant.now());
    }

    private RoutineSlot findSlot(Long id) {
        return routineSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoutineSlot", id));
    }

    private Subject findSubject(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private User resolveTeacher(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        User user = findUser(teacherId);
        if (!user.hasRole(RoleName.TEACHER)) {
            throw new BadRequestException("User " + teacherId + " does not have TEACHER role");
        }
        return user;
    }

    private static void validateTimeRange(LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new BadRequestException("endTime must be after startTime");
        }
    }

    private void checkOverlap(DayOfWeek day, LocalTime start, LocalTime end, Long excludeSlotId) {
        boolean overlaps = routineSlotRepository.findByDay(day).stream()
                .filter(existing -> !Objects.equals(existing.getId(), excludeSlotId))
                .anyMatch(existing -> existing.overlaps(start, end));
        if (overlaps) {
            throw new ConflictException("Routine slot overlaps with an existing slot on " + day);
        }
    }
}
