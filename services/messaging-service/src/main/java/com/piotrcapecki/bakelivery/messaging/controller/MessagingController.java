package com.piotrcapecki.bakelivery.messaging.controller;

import com.piotrcapecki.bakelivery.messaging.dto.CreateThreadRequest;
import com.piotrcapecki.bakelivery.messaging.dto.MessageResponse;
import com.piotrcapecki.bakelivery.messaging.dto.SendMessageRequest;
import com.piotrcapecki.bakelivery.messaging.dto.ThreadResponse;
import com.piotrcapecki.bakelivery.messaging.security.MessagingPrincipal;
import com.piotrcapecki.bakelivery.messaging.service.MessageService;
import com.piotrcapecki.bakelivery.messaging.service.ThreadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messaging")
@RequiredArgsConstructor
public class MessagingController {

    private final ThreadService threadService;
    private final MessageService messageService;

    @PostMapping("/threads")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadResponse createThread(@Valid @RequestBody CreateThreadRequest request,
                                       @AuthenticationPrincipal MessagingPrincipal principal) {
        return threadService.create(request, principal);
    }

    @GetMapping("/threads")
    public List<ThreadResponse> listThreads(@RequestParam UUID bakeryId,
                                             @AuthenticationPrincipal MessagingPrincipal principal) {
        return threadService.listForUser(bakeryId, principal);
    }

    @GetMapping("/threads/{threadId}/messages")
    public Page<MessageResponse> listMessages(@PathVariable UUID threadId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "50") int size,
                                               @AuthenticationPrincipal MessagingPrincipal principal) {
        return messageService.list(threadId, page, size, principal);
    }

    @PostMapping("/threads/{threadId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(@PathVariable UUID threadId,
                                       @Valid @RequestBody SendMessageRequest request,
                                       @AuthenticationPrincipal MessagingPrincipal principal) {
        return messageService.send(threadId, request, principal);
    }

    @PostMapping("/threads/{threadId}/messages/{msgId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID threadId,
                         @PathVariable UUID msgId,
                         @AuthenticationPrincipal MessagingPrincipal principal) {
        messageService.markRead(threadId, msgId, principal);
    }
}
