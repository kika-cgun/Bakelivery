package com.piotrcapecki.bakelivery.auth;

import com.piotrcapecki.bakelivery.auth.model.Role;
import com.piotrcapecki.bakelivery.auth.model.User;
import com.piotrcapecki.bakelivery.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers
class PersistenceMigrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void flywaySchemaMatchesJpaMappingForUserPersistence() {
        User user = User.builder()
                .email("migration-user@test.com")
                .passwordHash("hashedPw")
                .role(Role.USER)
                .build();

        User saved = userRepository.saveAndFlush(user);

        User loaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getEmail()).isEqualTo("migration-user@test.com");
        assertThat(loaded.getPasswordHash()).isEqualTo("hashedPw");
        assertThat(loaded.getRole()).isEqualTo(Role.USER);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }
}
