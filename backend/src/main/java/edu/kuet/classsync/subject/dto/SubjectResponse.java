package edu.kuet.classsync.subject.dto;

import java.util.List;

/** Public representation of a subject with its assigned teachers. */
public record SubjectResponse(Long id, String code, String title, List<TeacherSummary> teachers) {

    public record TeacherSummary(Long id, String name) { }
}
