package com.piotrcapecki.bakelivery.auth.config;

import com.piotrcapecki.bakelivery.auth.model.Bakery;
import com.piotrcapecki.bakelivery.auth.model.Role;
import com.piotrcapecki.bakelivery.auth.model.User;
import com.piotrcapecki.bakelivery.auth.repository.BakeryRepository;
import com.piotrcapecki.bakelivery.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("dev")
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final BakeryRepository bakeryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        Bakery testBakery = seedBakery();
        seedUser("super@bakelivery.pl", "super123", Role.SUPER_ADMIN, null);
        seedUser("admin@test.bakelivery.pl", "admin123", Role.BAKERY_ADMIN, testBakery);
        seedUser("dispatcher@test.bakelivery.pl", "dispatch123", Role.DISPATCHER, testBakery);
        seedUser("driver@test.bakelivery.pl", "driver123", Role.DRIVER, testBakery);
        seedUser("customer@test.bakelivery.pl", "customer123", Role.CUSTOMER, testBakery);
    }

    private Bakery seedBakery() {
        return bakeryRepository.findBySlug("test-bakery")
                .orElseGet(() -> {
                    Bakery bakery = bakeryRepository.save(Bakery.builder()
                            .name("Test Bakery")
                            .slug("test-bakery")
                            .contactEmail("admin@test.bakelivery.pl")
                            .build());
                    log.info("Seed bakery created: {}", bakery.getSlug());
                    return bakery;
                });
    }

    private void seedUser(String email, String password, Role role, Bakery bakery) {
        if (userRepository.existsByEmail(email)) {
            log.info("Seed user already exists: {}", email);
            return;
        }
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .bakery(bakery)
                .build());
        log.info("Seed user created: {} ({})", email, role);
    }
}
