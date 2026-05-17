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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
@RequiredArgsConstructor
public class ThreadService {

    private final ThreadRepository threadRepository;

    @Transactional
    public ThreadResponse create(CreateThreadRequest request, MessagingPrincipal principal) {
        UUID bakeryId = resolveBakeryId(principal, request.bakeryId());
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .bakeryId(bakeryId)
                .orderId(request.orderId())
                .customerId(principal.userId())
                .status(ThreadStatus.OPEN)
                .build();
        try {
            Thread saved = threadRepository.save(thread);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Thread already exists for this bakery and order");
        }
    }

    @Transactional(readOnly = true)
    public List<ThreadResponse> listForUser(UUID bakeryId, MessagingPrincipal principal) {
        UUID effectiveBakeryId = resolveBakeryId(principal, bakeryId);
        List<Thread> threads = switch (principal.role()) {
            case "CUSTOMER" -> threadRepository.findByBakeryIdAndCustomerId(effectiveBakeryId, principal.userId());
            case "DRIVER" -> threadRepository.findByBakeryIdAndDriverId(effectiveBakeryId, principal.userId());
            default -> threadRepository.findByBakeryId(effectiveBakeryId);
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

    private UUID resolveBakeryId(MessagingPrincipal principal, UUID requestBakeryId) {
        UUID principalBakeryId = principal.bakeryId();
        if (principalBakeryId == null) {
            return requestBakeryId;
        }
        if (!principalBakeryId.equals(requestBakeryId)) {
            throw new ResponseStatusException(FORBIDDEN, "No access to this bakery");
        }
        return principalBakeryId;
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
