package com.piotrcapecki.bakelivery.realtime.relay;

import com.piotrcapecki.bakelivery.common.jwt.JwtClaims;
import com.piotrcapecki.bakelivery.common.jwt.JwtUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RedisRelayIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @LocalServerPort
    private int port;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void messagingThread_publishToRedis_receivedOverStomp() throws Exception {
        UUID threadId = UUID.randomUUID();
        String payload = "{\"threadId\":\"" + threadId + "\",\"content\":\"Hello\"}";

        String token = jwtUtil.generateAccessToken(
                new JwtClaims("test@test.com", UUID.randomUUID(), null, "CUSTOMER"));

        WebSocketStompClient stompClient = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setMessageConverter(new StringMessageConverter());

        String wsUrl = "http://localhost:" + port + "/ws?token=" + token;
        StompSession session = stompClient
                .connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/thread." + threadId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((String) payload);
            }
        });

        String message = publishUntilReceived(received, "messaging.thread." + threadId, payload);
        assertThat(message).isNotNull();
        assertThat(message).contains(threadId.toString());

        session.disconnect();
    }

    @Test
    void driverPosition_publishToRedis_receivedOverStomp() throws Exception {
        UUID driverId = UUID.randomUUID();
        String payload = "{\"driverId\":\"" + driverId + "\",\"lat\":52.2297,\"lon\":21.0122}";

        String token = jwtUtil.generateAccessToken(
                new JwtClaims("driver@test.com", UUID.randomUUID(), null, "DRIVER"));

        WebSocketStompClient stompClient = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setMessageConverter(new StringMessageConverter());

        String wsUrl = "http://localhost:" + port + "/ws?token=" + token;
        StompSession session = stompClient
                .connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/driver.position." + driverId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((String) payload);
            }
        });

        String message = publishUntilReceived(received, "driver.pos." + driverId, payload);
        assertThat(message).isNotNull();
        assertThat(message).contains(driverId.toString());

        session.disconnect();
    }

    private String publishUntilReceived(BlockingQueue<String> received, String channel, String payload)
            throws InterruptedException {
        String message = null;
        for (int attempt = 0; attempt < 10 && message == null; attempt++) {
            redisTemplate.convertAndSend(channel, payload);
            message = received.poll(300, TimeUnit.MILLISECONDS);
        }
        return message;
    }
}
