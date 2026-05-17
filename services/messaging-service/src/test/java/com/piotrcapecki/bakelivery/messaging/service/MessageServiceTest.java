package com.piotrcapecki.bakelivery.messaging.service;

import com.piotrcapecki.bakelivery.messaging.dto.MessageResponse;
import com.piotrcapecki.bakelivery.messaging.dto.SendMessageRequest;
import com.piotrcapecki.bakelivery.messaging.event.MessageEventPublisher;
import com.piotrcapecki.bakelivery.messaging.model.Message;
import com.piotrcapecki.bakelivery.messaging.model.SenderRole;
import com.piotrcapecki.bakelivery.messaging.model.Thread;
import com.piotrcapecki.bakelivery.messaging.model.ThreadStatus;
import com.piotrcapecki.bakelivery.messaging.repository.MessageRepository;
import com.piotrcapecki.bakelivery.messaging.security.MessagingPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ThreadService threadService;

    @Mock
    private MessageEventPublisher eventPublisher;

    @InjectMocks
    private MessageService messageService;

    private UUID bakeryId;
    private UUID customerId;
    private UUID threadId;
    private Thread thread;
    private MessagingPrincipal customerPrincipal;

    @BeforeEach
    void setUp() {
        bakeryId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        threadId = UUID.randomUUID();
        thread = Thread.builder()
                .id(threadId)
                .bakeryId(bakeryId)
                .orderId(UUID.randomUUID())
                .customerId(customerId)
                .status(ThreadStatus.OPEN)
                .build();
        customerPrincipal = new MessagingPrincipal(customerId, "c@test.com", bakeryId, "CUSTOMER");
    }

    @Test
    void send_savesMessageAndPublishesEvent() {
        when(threadService.getThreadOrThrow(threadId)).thenReturn(thread);
        when(threadService.hasAccess(thread, customerPrincipal)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = messageService.send(threadId, new SendMessageRequest("Hello"), customerPrincipal);

        assertThat(response.content()).isEqualTo("Hello");
        assertThat(response.senderRole()).isEqualTo("CUSTOMER");
        verify(messageRepository).save(any(Message.class));
        verify(eventPublisher).publishMessageCreated(any(Message.class), any(Thread.class));
    }

    @Test
    void send_throwsForbidden_whenNoAccess() {
        MessagingPrincipal otherUser = new MessagingPrincipal(UUID.randomUUID(), "x@test.com", bakeryId, "CUSTOMER");
        when(threadService.getThreadOrThrow(threadId)).thenReturn(thread);
        when(threadService.hasAccess(thread, otherUser)).thenReturn(false);

        assertThatThrownBy(() -> messageService.send(threadId, new SendMessageRequest("Hi"), otherUser))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void list_returnsPaginatedMessages() {
        Message msg = Message.builder()
                .id(UUID.randomUUID())
                .bakeryId(bakeryId)
                .thread(thread)
                .senderId(customerId)
                .senderRole(SenderRole.CUSTOMER)
                .content("Test")
                .build();
        when(threadService.getThreadOrThrow(threadId)).thenReturn(thread);
        when(threadService.hasAccess(thread, customerPrincipal)).thenReturn(true);
        when(messageRepository.findByThreadIdOrderByCreatedAtAsc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(msg), PageRequest.of(0, 50), 1));

        var page = messageService.list(threadId, 0, 50, customerPrincipal);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).content()).isEqualTo("Test");
    }

    @Test
    void markRead_throwsBadRequest_whenSenderMarksOwnMessage() {
        Message msg = Message.builder()
                .id(UUID.randomUUID())
                .bakeryId(bakeryId)
                .thread(thread)
                .senderId(customerId)
                .senderRole(SenderRole.CUSTOMER)
                .content("Hi")
                .build();
        when(threadService.getThreadOrThrow(threadId)).thenReturn(thread);
        when(threadService.hasAccess(thread, customerPrincipal)).thenReturn(true);
        when(messageRepository.findByIdAndThreadId(msg.getId(), threadId)).thenReturn(Optional.of(msg));

        assertThatThrownBy(() -> messageService.markRead(threadId, msg.getId(), customerPrincipal))
                .isInstanceOf(ResponseStatusException.class);
    }
}
