package com.piotrcapecki.bakelivery.routing.repository;

import com.piotrcapecki.bakelivery.routing.model.RoutePlan;
import com.piotrcapecki.bakelivery.routing.model.RoutePlanStatus;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoutePlanRepositoryTest {

    @MockitoBean RedissonClient redissonClient;
    @MockitoBean RabbitTemplate rabbitTemplate;

    @Autowired RoutePlanRepository repo;

    @Test
    void saveAndFindByBakeryAndDate() {
        UUID bakeryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        RoutePlan plan = RoutePlan.builder()
                .bakeryId(bakeryId).driverId(driverId).date(today)
                .status(RoutePlanStatus.READY).build();
        repo.save(plan);

        var found = repo.findByBakeryIdAndDriverIdAndDate(bakeryId, driverId, today);
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(RoutePlanStatus.READY);
    }

    @Test
    void existsByBakeryDateAndStatus() {
        UUID bakeryId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        RoutePlan plan = RoutePlan.builder()
                .bakeryId(bakeryId).driverId(UUID.randomUUID()).date(today)
                .status(RoutePlanStatus.OPTIMIZING).build();
        repo.save(plan);

        boolean exists = repo.existsByBakeryIdAndDateAndStatusIn(
                bakeryId, today, List.of(RoutePlanStatus.READY, RoutePlanStatus.OPTIMIZING));
        assertThat(exists).isTrue();
    }
}
