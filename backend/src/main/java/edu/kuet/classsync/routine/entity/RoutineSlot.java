package edu.kuet.classsync.routine.entity;

import edu.kuet.classsync.common.entity.BaseEntity;
import edu.kuet.classsync.subject.entity.Subject;
import edu.kuet.classsync.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;

/** Weekly class routine slot linking a subject and an optional assigned teacher. */
@Entity
@Table(name = "routine_slots")
public class RoutineSlot extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 16)
    private DayOfWeek day;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, length = 30)
    private String room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    protected RoutineSlot() { }

    public RoutineSlot(DayOfWeek day, LocalTime startTime, LocalTime endTime,
                       String room, Subject subject, User teacher) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.subject = subject;
        this.teacher = teacher;
    }

    public boolean overlaps(LocalTime otherStart, LocalTime otherEnd) {
        return startTime.isBefore(otherEnd) && endTime.isAfter(otherStart);
    }

    public DayOfWeek getDay() { return day; }

    public void setDay(DayOfWeek day) { this.day = day; }

    public LocalTime getStartTime() { return startTime; }

    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }

    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getRoom() { return room; }

    public void setRoom(String room) { this.room = room; }

    public Subject getSubject() { return subject; }

    public void setSubject(Subject subject) { this.subject = subject; }

    public User getTeacher() { return teacher; }

    public void setTeacher(User teacher) { this.teacher = teacher; }
}
