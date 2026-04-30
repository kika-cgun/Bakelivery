package com.piotrcapecki.bakelivery.auth;

import com.piotrcapecki.bakelivery.auth.model.Bakery;
import com.piotrcapecki.bakelivery.auth.model.Role;
import com.piotrcapecki.bakelivery.auth.model.User;
import com.piotrcapecki.bakelivery.auth.repository.BakeryRepository;
import com.piotrcapecki.bakelivery.auth.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired
    private BakeryRepository bakeryRepository;

    @Autowired
    private EntityManager entityManager;

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

    @Test
    void bakeryRepositorySavesAndFindsBySlug() {
        Bakery bakery = Bakery.builder()
                .name("Task Bakery")
                .slug("task-bakery")
                .contactEmail("owner@task-bakery.test")
                .contactPhone("+48 123 456 789")
                .build();

        Bakery saved = bakeryRepository.saveAndFlush(bakery);

        Bakery loaded = bakeryRepository.findBySlug("task-bakery").orElseThrow();
        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getName()).isEqualTo("Task Bakery");
        assertThat(loaded.getContactEmail()).isEqualTo("owner@task-bakery.test");
        assertThat(loaded.getContactPhone()).isEqualTo("+48 123 456 789");
        assertThat(loaded.getTimezone()).isEqualTo("Europe/Warsaw");
        assertThat(loaded.isActive()).isTrue();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void bakerySlugMustBeUnique() {
        bakeryRepository.saveAndFlush(Bakery.builder()
                .name("First Duplicate Bakery")
                .slug("duplicate-bakery")
                .build());

        Bakery duplicate = Bakery.builder()
                .name("Second Duplicate Bakery")
                .slug("duplicate-bakery")
                .build();

        assertThatThrownBy(() -> bakeryRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void userCanBePersistedAndLoadedWithBakeryRelation() {
        Bakery bakery = bakeryRepository.saveAndFlush(Bakery.builder()
                .name("Relation Bakery")
                .slug("relation-bakery")
                .build());
        User user = User.builder()
                .email("bakery-user@test.com")
                .passwordHash("hashedPw")
                .role(Role.USER)
                .bakery(bakery)
                .build();

        User saved = userRepository.saveAndFlush(user);
        entityManager.flush();
        entityManager.clear();

        User loaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getBakery()).isNotNull();
        assertThat(loaded.getBakery().getId()).isEqualTo(bakery.getId());
        assertThat(loaded.getBakery().getSlug()).isEqualTo("relation-bakery");
    }
}
