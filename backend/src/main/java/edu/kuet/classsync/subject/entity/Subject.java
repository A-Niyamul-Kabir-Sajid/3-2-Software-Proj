package edu.kuet.classsync.subject.entity;

import edu.kuet.classsync.common.entity.BaseEntity;
import edu.kuet.classsync.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Academic subject taught by one or more teachers ({@code teacher_subjects} M:M join table). */
@Entity
@Table(name = "subjects")
public class Subject extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String title;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "teacher_subjects",
            joinColumns = @JoinColumn(name = "subject_id"),
            inverseJoinColumns = @JoinColumn(name = "teacher_id"))
    private Set<User> teachers = new HashSet<>();

    protected Subject() { }

    public Subject(String code, String title, Collection<User> teachers) {
        this.code = code;
        this.title = title;
        if (teachers != null) {
            this.teachers.addAll(teachers);
        }
    }

    public String getCode() { return code; }

    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public Set<User> getTeachers() { return Set.copyOf(teachers); }

    public void replaceTeachers(Collection<User> newTeachers) {
        teachers.clear();
        if (newTeachers != null) {
            teachers.addAll(newTeachers);
        }
    }
}
