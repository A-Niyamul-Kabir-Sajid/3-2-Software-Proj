package edu.kuet.classsync.routine.service;

import edu.kuet.classsync.routine.dto.RoutineResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/** Proxy pattern: caches the weekly routine response until invalidated by a mutation or undo. */
@Service
@Primary
public class CachingRoutineServiceProxy implements RoutineQueryService {

    private final RoutineQueryService delegate;
    private RoutineResponse cached;

    public CachingRoutineServiceProxy(@Qualifier("routineQueryServiceImpl") RoutineQueryService delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized RoutineResponse getWeek() {
        if (cached == null) {
            cached = delegate.getWeek();
        }
        return cached;
    }

    public synchronized void invalidate() {
        cached = null;
    }
}
