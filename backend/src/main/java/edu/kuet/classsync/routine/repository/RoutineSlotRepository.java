package edu.kuet.classsync.routine.repository;

import edu.kuet.classsync.routine.entity.RoutineSlot;
import java.time.DayOfWeek;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineSlotRepository extends JpaRepository<RoutineSlot, Long> {

    boolean existsBySubjectId(Long subjectId);

    List<RoutineSlot> findByDay(DayOfWeek day);

    @EntityGraph(attributePaths = {"subject", "teacher"})
    List<RoutineSlot> findAllByOrderByDayAscStartTimeAsc();
}
