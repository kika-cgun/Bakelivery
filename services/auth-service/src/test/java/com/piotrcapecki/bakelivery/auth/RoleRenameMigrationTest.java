package com.piotrcapecki.bakelivery.auth;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RoleRenameMigrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void v3RenamesLegacyUserAndAdminRoles() throws SQLException {
        flyway("2").clean();
        flyway("2").migrate();
        insertLegacyUser("legacy-user@test.com", "USER");
        insertLegacyUser("legacy-admin@test.com", "ADMIN");

        flyway(null).migrate();

        assertThat(loadRoles())
                .containsEntry("legacy-user@test.com", "CUSTOMER")
                .containsEntry("legacy-admin@test.com", "SUPER_ADMIN");
    }

    private Flyway flyway(String targetVersion) {
        FluentConfiguration configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false);
        if (targetVersion != null) {
            configuration.target(targetVersion);
        }
        return configuration.load();
    }

    private void insertLegacyUser(String email, String role) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO users (id, email, password_hash, role)
                     VALUES (?, ?, ?, ?)
                     """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, email);
            statement.setString(3, "hashed-password");
            statement.setString(4, role);
            statement.executeUpdate();
        }
    }

    private Map<String, String> loadRoles() throws SQLException {
        Map<String, String> roles = new LinkedHashMap<>();
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT email, role
                     FROM users
                     WHERE email IN ('legacy-user@test.com', 'legacy-admin@test.com')
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                roles.put(resultSet.getString("email"), resultSet.getString("role"));
            }
        }
        return roles;
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
