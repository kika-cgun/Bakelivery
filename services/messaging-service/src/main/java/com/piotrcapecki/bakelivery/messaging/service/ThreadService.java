package com.piotrcapecki.bakelivery.messaging.service;

import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.messaging.dto.CreateThreadRequest;
import com.piotrcapecki.bakelivery.messaging.dto.ThreadResponse;
import com.piotrcapecki.bakelivery.messaging.model.Thread;
import com.piotrcapecki.bakelivery.messaging.model.ThreadStatus;
import com.piotrcapecki.bakelivery.messaging.repository.ThreadRepository;
import com.piotrcapecki.bakelivery.messaging.security.MessagingPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ThreadService {

    private final ThreadRepository threadRepository;

    @Transactional
    public ThreadResponse create(CreateThreadRequest request, MessagingPrincipal principal) {
        if (threadRepository.existsByBakeryIdAndOrderId(request.bakeryId(), request.orderId())) {
            throw new ConflictException("Thread already exists for this bakery and order");
        }
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .bakeryId(request.bakeryId())
                .orderId(request.orderId())
                .customerId(principal.userId())
                .status(ThreadStatus.OPEN)
                .build();
        Thread saved = threadRepository.save(thread);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ThreadResponse> listForUser(UUID bakeryId, MessagingPrincipal principal) {
        List<Thread> threads = switch (principal.role()) {
            case "CUSTOMER" -> threadRepository.findByBakeryIdAndCustomerId(bakeryId, principal.userId());
            case "DRIVER" -> threadRepository.findByBakeryIdAndDriverId(bakeryId, principal.userId());
            default -> threadRepository.findByBakeryId(bakeryId);
        };
        return threads.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void assignDriver(UUID bakeryId, UUID orderId, UUID driverId) {
        threadRepository.findByBakeryIdAndOrderId(bakeryId, orderId).ifPresent(thread -> {
            thread.setDriverId(driverId);
            threadRepository.save(thread);
        });
    }

    @Transactional
    public void closeThread(UUID bakeryId, UUID orderId) {
        threadRepository.findByBakeryIdAndOrderId(bakeryId, orderId).ifPresent(thread -> {
            thread.setStatus(ThreadStatus.CLOSED);
            threadRepository.save(thread);
        });
    }

    public Thread getThreadOrThrow(UUID threadId) {
        return threadRepository.findById(threadId)
                .orElseThrow(() -> new NotFoundException("Thread not found: " + threadId));
    }

    public boolean hasAccess(Thread thread, MessagingPrincipal principal) {
        return switch (principal.role()) {
            case "CUSTOMER" -> thread.getCustomerId().equals(principal.userId());
            case "DRIVER" -> principal.userId().equals(thread.getDriverId());
            default -> thread.getBakeryId().equals(principal.bakeryId());
        };
    }

    private ThreadResponse toResponse(Thread t) {
        return new ThreadResponse(
                t.getId(),
                t.getOrderId(),
                t.getCustomerId(),
                t.getDriverId(),
                t.getStatus().name(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
