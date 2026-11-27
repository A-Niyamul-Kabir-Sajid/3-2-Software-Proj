package edu.kuet.classsync.routine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import edu.kuet.classsync.routine.dto.RoutineResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CachingRoutineServiceProxyTest {

    @Mock RoutineQueryService delegate;
    @InjectMocks CachingRoutineServiceProxy proxy;

    @Test
    void getWeek_secondCall_usesCache_andInvalidateClears() {
        RoutineResponse first = new RoutineResponse(List.of(), 0);
        RoutineResponse updated = new RoutineResponse(List.of(), 1);
        given(delegate.getWeek()).willReturn(first, updated);

        assertThat(proxy.getWeek()).isSameAs(first);
        assertThat(proxy.getWeek()).isSameAs(first);
        verify(delegate, times(1)).getWeek();

        proxy.invalidate();

        assertThat(proxy.getWeek()).isSameAs(updated);
        verify(delegate, times(2)).getWeek();
    }
}
