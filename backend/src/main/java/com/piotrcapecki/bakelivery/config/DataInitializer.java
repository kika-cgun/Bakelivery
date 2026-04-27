package com.piotrcapecki.bakelivery.config;

import com.piotrcapecki.bakelivery.dto.RegisterRequest;
import com.piotrcapecki.bakelivery.repository.UserRepository;
import com.piotrcapecki.bakelivery.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AuthService authService;

    @Override
    public void run(ApplicationArguments args) {
        seedUser("admin@bakelivery.pl", "admin123");
        seedUser("demo@bakelivery.pl", "demo123");
    }

    private void seedUser(String email, String password) {
        if (userRepository.existsByEmail(email)) {
            log.info("Seed user already exists: {}", email);
            return;
        }
        authService.register(new RegisterRequest(email, password));
        log.info("Seed user created: {} / {}", email, password);
    }
}
