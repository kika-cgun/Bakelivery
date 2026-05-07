package com.piotrcapecki.bakelivery.routing.service;

import com.piotrcapecki.bakelivery.routing.event.OptimizeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.piotrcapecki.bakelivery.routing.config.RabbitConfig.QUEUE_OPTIMIZE;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingWorker {

    private final RedissonClient redisson;
    private final RoutingPlanService planService;

    @RabbitListener(queues = QUEUE_OPTIMIZE)
    public void handle(OptimizeRequest request) {
        String lockKey = "lock:routing:bakery:" + request.bakeryId();
        RLock lock = redisson.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, 300, TimeUnit.SECONDS);
            if (!acquired) {
                log.info("Optimization already running for bakery {}, skipping", request.bakeryId());
                return;
            }
            planService.optimize(request.bakeryId(), request.date(), "Bearer internal");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Optimization failed for bakery {}: {}", request.bakeryId(), e.getMessage(), e);
            throw e;
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
