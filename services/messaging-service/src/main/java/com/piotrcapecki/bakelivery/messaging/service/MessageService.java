package com.piotrcapecki.bakelivery.messaging.service;

import com.piotrcapecki.bakelivery.messaging.dto.MessageResponse;
import com.piotrcapecki.bakelivery.messaging.dto.SendMessageRequest;
import com.piotrcapecki.bakelivery.messaging.event.MessageEventPublisher;
import com.piotrcapecki.bakelivery.messaging.model.Message;
import com.piotrcapecki.bakelivery.messaging.model.SenderRole;
import com.piotrcapecki.bakelivery.messaging.model.Thread;
import com.piotrcapecki.bakelivery.messaging.repository.MessageRepository;
import com.piotrcapecki.bakelivery.messaging.security.MessagingPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ThreadService threadService;
    private final MessageEventPublisher eventPublisher;

    @Transactional
    public MessageResponse send(UUID threadId, SendMessageRequest request, MessagingPrincipal principal) {
        Thread thread = threadService.getThreadOrThrow(threadId);
        if (!threadService.hasAccess(thread, principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this thread");
        }
        SenderRole senderRole = SenderRole.valueOf(principal.role());
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .bakeryId(thread.getBakeryId())
                .thread(thread)
                .senderId(principal.userId())
                .senderRole(senderRole)
                .content(request.content())
                .build();
        Message saved = messageRepository.save(message);
        eventPublisher.publishMessageCreated(saved, thread);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> list(UUID threadId, int page, int size, MessagingPrincipal principal) {
        Thread thread = threadService.getThreadOrThrow(threadId);
        if (!threadService.hasAccess(thread, principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this thread");
        }
        return messageRepository.findByThreadIdOrderByCreatedAtAsc(threadId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional
    public void markRead(UUID threadId, UUID msgId, MessagingPrincipal principal) {
        Thread thread = threadService.getThreadOrThrow(threadId);
        if (!threadService.hasAccess(thread, principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this thread");
        }
        Message message = messageRepository.findById(msgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        // Only the recipient (someone other than the sender) can mark as read
        if (message.getSenderId().equals(principal.userId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot mark own message as read");
        }
        if (message.getReadAt() == null) {
            message.setReadAt(LocalDateTime.now());
            messageRepository.save(message);
        }
    }

    private MessageResponse toResponse(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getSenderId(),
                m.getSenderRole().name(),
                m.getContent(),
                m.getReadAt(),
                m.getCreatedAt()
        );
    }
}
