package edu.kuet.classsync.subject.mapper;

import edu.kuet.classsync.subject.dto.SubjectResponse;
import edu.kuet.classsync.subject.dto.SubjectResponse.TeacherSummary;
import edu.kuet.classsync.subject.entity.Subject;
import edu.kuet.classsync.user.entity.User;
import java.util.Comparator;
import java.util.List;

public final class SubjectMapper {

    private SubjectMapper() { }

    public static SubjectResponse toResponse(Subject subject) {
        List<TeacherSummary> teachers = subject.getTeachers().stream()
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(User::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(t -> new TeacherSummary(t.getId(), t.getName()))
                .toList();
        return new SubjectResponse(subject.getId(), subject.getCode(), subject.getTitle(), teachers);
    }
}
