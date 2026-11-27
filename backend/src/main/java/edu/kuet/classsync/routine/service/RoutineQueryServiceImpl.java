package edu.kuet.classsync.routine.service;

import edu.kuet.classsync.routine.dto.RoutineResponse;
import edu.kuet.classsync.routine.mapper.RoutineMapper;
import edu.kuet.classsync.routine.pattern.RoutineAssembler;
import edu.kuet.classsync.routine.pattern.WeekRoutine;
import edu.kuet.classsync.routine.repository.RoutineSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Real subject for {@link RoutineQueryService}: queries all slots and builds the Composite tree. */
@Service
public class RoutineQueryServiceImpl implements RoutineQueryService {

    private final RoutineSlotRepository routineSlotRepository;

    public RoutineQueryServiceImpl(RoutineSlotRepository routineSlotRepository) {
        this.routineSlotRepository = routineSlotRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RoutineResponse getWeek() {
        WeekRoutine week = RoutineAssembler.build(routineSlotRepository.findAllByOrderByDayAscStartTimeAsc());
        return RoutineMapper.toRoutineResponse(week);
    }
}
