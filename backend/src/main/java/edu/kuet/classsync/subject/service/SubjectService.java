package edu.kuet.classsync.subject.service;

import edu.kuet.classsync.common.exception.BadRequestException;
import edu.kuet.classsync.common.exception.ConflictException;
import edu.kuet.classsync.common.exception.ResourceNotFoundException;
import edu.kuet.classsync.routine.repository.RoutineSlotRepository;
import edu.kuet.classsync.subject.dto.SubjectRequest;
import edu.kuet.classsync.subject.dto.SubjectResponse;
import edu.kuet.classsync.subject.entity.Subject;
import edu.kuet.classsync.subject.mapper.SubjectMapper;
import edu.kuet.classsync.subject.repository.SubjectRepository;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import edu.kuet.classsync.user.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Subject CRUD and teacher assignment validation. */
@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final RoutineSlotRepository routineSlotRepository;

    public SubjectService(SubjectRepository subjectRepository,
                          UserRepository userRepository,
                          RoutineSlotRepository routineSlotRepository) {
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.routineSlotRepository = routineSlotRepository;
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> list() {
        return subjectRepository.findAllByOrderByCodeAsc().stream()
                .map(SubjectMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse get(Long id) {
        return SubjectMapper.toResponse(find(id));
    }

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        String code = request.code().trim();
        String title = request.title().trim();
        if (subjectRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Subject code already exists: " + code);
        }
        Set<User> teachers = resolveTeachers(request.teacherIds());
        Subject saved = subjectRepository.save(new Subject(code, title, teachers));
        return SubjectMapper.toResponse(saved);
    }

    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = find(id);
        String code = request.code().trim();
        String title = request.title().trim();
        if (subjectRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException("Subject code already exists: " + code);
        }
        Set<User> teachers = resolveTeachers(request.teacherIds());
        subject.setCode(code);
        subject.setTitle(title);
        subject.replaceTeachers(teachers);
        return SubjectMapper.toResponse(subject);
    }

    @Transactional
    public void delete(Long id) {
        Subject subject = find(id);
        if (routineSlotRepository.existsBySubjectId(id)) {
            throw new ConflictException("Subject " + id + " is in use by routine slots");
        }
        subjectRepository.delete(subject);
    }

    private Subject find(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", id));
    }

    private Set<User> resolveTeachers(List<Long> teacherIds) {
        if (teacherIds == null || teacherIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> uniqueIds = new LinkedHashSet<>();
        for (Long teacherId : teacherIds) {
            if (teacherId == null) {
                throw new BadRequestException("Teacher id must not be null");
            }
            uniqueIds.add(teacherId);
        }
        Map<Long, User> byId = userRepository.findAllById(uniqueIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Set<User> teachers = new LinkedHashSet<>();
        for (Long teacherId : uniqueIds) {
            User user = byId.get(teacherId);
            if (user == null) {
                throw new ResourceNotFoundException("User", teacherId);
            }
            if (!user.hasRole(RoleName.TEACHER)) {
                throw new BadRequestException("User " + teacherId + " does not have TEACHER role");
            }
            teachers.add(user);
        }
        return teachers;
    }
}
