package edu.kuet.classsync.routine.service;

import edu.kuet.classsync.routine.dto.RoutineResponse;

/** Query abstraction for reading the weekly routine (Proxy pattern). */
public interface RoutineQueryService {

    RoutineResponse getWeek();
}
