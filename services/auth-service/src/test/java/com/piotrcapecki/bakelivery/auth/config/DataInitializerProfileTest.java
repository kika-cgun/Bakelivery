package com.piotrcapecki.bakelivery.auth.config;

import com.piotrcapecki.bakelivery.auth.repository.BakeryRepository;
import com.piotrcapecki.bakelivery.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DataInitializerProfileTest {

    @Test
    void dataInitializerIsNotRegisteredWithoutDevProfile() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(MockDependencies.class, DataInitializer.class);

            context.refresh();

            assertThat(context.getBeanNamesForType(DataInitializer.class)).isEmpty();
        }
    }

    @Test
    void dataInitializerIsRegisteredForDevProfile() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("dev");
            context.register(MockDependencies.class, DataInitializer.class);

            context.refresh();

            assertThat(context.getBeanNamesForType(DataInitializer.class)).hasSize(1);
        }
    }

    @Configuration
    static class MockDependencies {

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        BakeryRepository bakeryRepository() {
            return mock(BakeryRepository.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return mock(PasswordEncoder.class);
        }
    }
}
