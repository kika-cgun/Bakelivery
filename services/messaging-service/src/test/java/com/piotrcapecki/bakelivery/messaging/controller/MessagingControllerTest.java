package com.piotrcapecki.bakelivery.messaging.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import com.piotrcapecki.bakelivery.messaging.dto.CreateThreadRequest;
import com.piotrcapecki.bakelivery.messaging.dto.MessageResponse;
import com.piotrcapecki.bakelivery.messaging.dto.SendMessageRequest;
import com.piotrcapecki.bakelivery.messaging.dto.ThreadResponse;
import com.piotrcapecki.bakelivery.messaging.security.MessagingPrincipal;
import com.piotrcapecki.bakelivery.messaging.service.MessageService;
import com.piotrcapecki.bakelivery.messaging.service.ThreadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class MessagingControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private ThreadService threadService;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private UsernamePasswordAuthenticationToken customerAuth(UUID userId, UUID bakeryId) {
        MessagingPrincipal principal = new MessagingPrincipal(userId, "c@test.com", bakeryId, "CUSTOMER");
        return new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    void createThread_returns201() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CreateThreadRequest request = new CreateThreadRequest(orderId, bakeryId);
        ThreadResponse response = new ThreadResponse(UUID.randomUUID(), orderId, customerId, null,
                "OPEN", OffsetDateTime.now(), OffsetDateTime.now());

        when(threadService.create(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/messaging/threads")
                        .with(authentication(customerAuth(customerId, bakeryId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()));
    }

    @Test
    void createThread_returns409_whenDuplicate() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CreateThreadRequest request = new CreateThreadRequest(orderId, bakeryId);

        when(threadService.create(any(), any())).thenThrow(new ConflictException("Thread already exists"));

        mockMvc.perform(post("/api/messaging/threads")
                        .with(authentication(customerAuth(customerId, bakeryId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void listThreads_returns200() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        when(threadService.listForUser(eq(bakeryId), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/messaging/threads")
                        .with(authentication(customerAuth(customerId, bakeryId)))
                        .param("bakeryId", bakeryId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void sendMessage_returns201() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        SendMessageRequest request = new SendMessageRequest("Hello driver!");
        MessageResponse response = new MessageResponse(UUID.randomUUID(), customerId, "CUSTOMER",
                "Hello driver!", null, OffsetDateTime.now());

        when(messageService.send(eq(threadId), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/messaging/threads/{threadId}/messages", threadId)
                        .with(authentication(customerAuth(customerId, bakeryId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Hello driver!"))
                .andExpect(jsonPath("$.senderRole").value("CUSTOMER"));
    }

    @Test
    void sendMessage_returns403_whenNoAccess() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        SendMessageRequest request = new SendMessageRequest("Unauthorized");

        when(messageService.send(eq(threadId), any(), any()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "No access"));

        mockMvc.perform(post("/api/messaging/threads/{threadId}/messages", threadId)
                        .with(authentication(customerAuth(customerId, bakeryId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
