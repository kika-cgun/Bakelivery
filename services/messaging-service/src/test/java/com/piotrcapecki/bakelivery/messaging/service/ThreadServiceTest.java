package com.piotrcapecki.bakelivery.messaging.service;

import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import com.piotrcapecki.bakelivery.messaging.dto.CreateThreadRequest;
import com.piotrcapecki.bakelivery.messaging.dto.ThreadResponse;
import com.piotrcapecki.bakelivery.messaging.model.Thread;
import com.piotrcapecki.bakelivery.messaging.model.ThreadStatus;
import com.piotrcapecki.bakelivery.messaging.repository.ThreadRepository;
import com.piotrcapecki.bakelivery.messaging.security.MessagingPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThreadServiceTest {

    @Mock
    private ThreadRepository threadRepository;

    @InjectMocks
    private ThreadService threadService;

    @Test
    void create_savesThread_whenNoDuplicate() {
        UUID bakeryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        MessagingPrincipal principal = new MessagingPrincipal(customerId, "c@test.com", bakeryId, "CUSTOMER");
        CreateThreadRequest request = new CreateThreadRequest(orderId, bakeryId);

        when(threadRepository.save(any(Thread.class))).thenAnswer(inv -> inv.getArgument(0));

        ThreadResponse response = threadService.create(request, principal);

        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.status()).isEqualTo("OPEN");
        verify(threadRepository).save(any(Thread.class));
    }

    @Test
    void create_throwsConflict_whenDuplicateExists() {
        UUID bakeryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        MessagingPrincipal principal = new MessagingPrincipal(UUID.randomUUID(), "c@test.com", bakeryId, "CUSTOMER");
        CreateThreadRequest request = new CreateThreadRequest(orderId, bakeryId);

        when(threadRepository.save(any(Thread.class))).thenThrow(new DataIntegrityViolationException("uq_thread_order"));

        assertThatThrownBy(() -> threadService.create(request, principal))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void listForUser_customer_returnsOwnThreadsOnly() {
        UUID bakeryId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        MessagingPrincipal principal = new MessagingPrincipal(customerId, "c@test.com", bakeryId, "CUSTOMER");
        List<Thread> customerThreads = List.of(Thread.builder()
                .id(UUID.randomUUID())
                .bakeryId(bakeryId)
                .orderId(UUID.randomUUID())
                .customerId(customerId)
                .status(ThreadStatus.OPEN)
                .build());

        when(threadRepository.findByBakeryIdAndCustomerId(bakeryId, customerId)).thenReturn(customerThreads);

        List<ThreadResponse> result = threadService.listForUser(bakeryId, principal);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).customerId()).isEqualTo(customerId);
    }

    @Test
    void listForUser_admin_returnsAllBakeryThreads() {
        UUID bakeryId = UUID.randomUUID();
        MessagingPrincipal principal = new MessagingPrincipal(UUID.randomUUID(), "a@test.com", bakeryId, "BAKERY_ADMIN");
        List<Thread> allThreads = List.of(
                Thread.builder().id(UUID.randomUUID()).bakeryId(bakeryId).orderId(UUID.randomUUID())
                        .customerId(UUID.randomUUID()).status(ThreadStatus.OPEN).build(),
                Thread.builder().id(UUID.randomUUID()).bakeryId(bakeryId).orderId(UUID.randomUUID())
                        .customerId(UUID.randomUUID()).status(ThreadStatus.OPEN).build()
        );

        when(threadRepository.findByBakeryId(bakeryId)).thenReturn(allThreads);

        List<ThreadResponse> result = threadService.listForUser(bakeryId, principal);

        assertThat(result).hasSize(2);
    }
}
