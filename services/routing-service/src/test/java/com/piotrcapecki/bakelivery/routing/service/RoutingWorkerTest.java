package com.piotrcapecki.bakelivery.routing.service;

import com.piotrcapecki.bakelivery.routing.event.OptimizeRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutingWorkerTest {

    @Mock RedissonClient redisson;
    @Mock RLock lock;
    @Mock RoutingPlanService planService;
    @InjectMocks RoutingWorker worker;

    private OptimizeRequest request() {
        return new OptimizeRequest(UUID.randomUUID(), LocalDate.now(), "test-user");
    }

    @Test
    void callsOptimizeWhenLockAcquired() throws InterruptedException {
        OptimizeRequest req = request();
        when(redisson.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(0, 300, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        worker.handle(req);

        verify(planService).optimize(eq(req.bakeryId()), eq(req.date()), eq("Bearer internal"));
        verify(lock).unlock();
    }

    @Test
    void skipsOptimizeWhenLockNotAcquired() throws InterruptedException {
        OptimizeRequest req = request();
        when(redisson.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(0, 300, TimeUnit.SECONDS)).thenReturn(false);

        worker.handle(req);

        verifyNoInteractions(planService);
        verify(lock, never()).unlock();
    }
}
