package com.piotrcapecki.bakelivery.dispatching.scheduler;

import com.piotrcapecki.bakelivery.dispatching.model.DispatchStop;
import com.piotrcapecki.bakelivery.dispatching.model.FixedDeliveryPoint;
import com.piotrcapecki.bakelivery.dispatching.repository.DispatchStopRepository;
import com.piotrcapecki.bakelivery.dispatching.repository.FixedDeliveryPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FixedDeliveryScheduler {

    private final FixedDeliveryPointRepository pointRepository;
    private final DispatchStopRepository stopRepository;
    private final Clock clock;

    @Scheduled(cron = "0 0 5 * * *")
    @Transactional
    public void generateDailyStops() {
        LocalDate today = LocalDate.now(clock);
        int dayBit = today.getDayOfWeek().getValue() - 1;

        List<FixedDeliveryPoint> activePoints = pointRepository.findByActiveTrue();
        int created = 0;
        int skipped = 0;

        for (FixedDeliveryPoint point : activePoints) {
            if (((point.getDeliveryDays() >> dayBit) & 1) != 1) {
                skipped++;
                continue;
            }
            boolean exists = stopRepository.findByBakeryIdAndDate(point.getBakeryId(), today)
                    .stream()
                    .anyMatch(s -> point.getId().equals(s.getFixedPointId()));
            if (exists) {
                skipped++;
                continue;
            }
            DispatchStop stop = DispatchStop.builder()
                    .bakeryId(point.getBakeryId())
                    .date(today)
                    .fixedPointId(point.getId())
                    .customerName(point.getName())
                    .deliveryAddress(point.getAddress())
                    .lat(point.getLat())
                    .lon(point.getLon())
                    .notes(point.getDefaultNotes())
                    .build();
            stopRepository.save(stop);
            created++;
        }
        log.info("generateDailyStops date={} created={} skipped={}", today, created, skipped);
    }
}
