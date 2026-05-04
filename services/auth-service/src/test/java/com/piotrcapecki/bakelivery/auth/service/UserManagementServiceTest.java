package com.piotrcapecki.bakelivery.auth.service;

import com.piotrcapecki.bakelivery.auth.dto.CreateEmployeeRequest;
import com.piotrcapecki.bakelivery.auth.model.Bakery;
import com.piotrcapecki.bakelivery.auth.model.Role;
import com.piotrcapecki.bakelivery.auth.model.User;
import com.piotrcapecki.bakelivery.auth.repository.BakeryRepository;
import com.piotrcapecki.bakelivery.auth.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock UserRepository userRepository;
    @Mock BakeryRepository bakeryRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserManagementService userManagementService;

    @Test
    void listEmployees_returnsUsersForBakery() {
        UUID bakeryId = UUID.randomUUID();
        User user1 = User.builder()
                .id(UUID.randomUUID())
                .email("admin@bakery.test")
                .passwordHash("hashedPw")
                .role(Role.BAKERY_ADMIN)
                .build();
        User user2 = User.builder()
                .id(UUID.randomUUID())
                .email("driver@bakery.test")
                .passwordHash("hashedPw")
                .role(Role.DRIVER)
                .build();
        when(userRepository.findAllByBakeryId(bakeryId)).thenReturn(List.of(user1, user2));

        List<User> result = userManagementService.listEmployees(bakeryId);

        assertThat(result).containsExactly(user1, user2);
        verify(userRepository).findAllByBakeryId(bakeryId);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"DISPATCHER", "DRIVER"})
    void createEmployee_savesDispatcherOrDriverForActingBakery(Role role) {
        UUID bakeryId = UUID.randomUUID();
        Bakery bakery = Bakery.builder()
                .id(bakeryId)
                .name("Tenant Bakery")
                .slug("tenant-bakery")
                .build();
        CreateEmployeeRequest request = new CreateEmployeeRequest("employee@test.com", "temporary123", role);
        when(userRepository.existsByEmail("employee@test.com")).thenReturn(false);
        when(bakeryRepository.findById(bakeryId)).thenReturn(Optional.of(bakery));
        when(passwordEncoder.encode("temporary123")).thenReturn("encoded-temporary");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userManagementService.createEmployee(bakeryId, request);

        assertThat(created.getEmail()).isEqualTo("employee@test.com");
        assertThat(created.getPasswordHash()).isEqualTo("encoded-temporary");
        assertThat(created.getRole()).isEqualTo(role);
        assertThat(created.getBakery()).isSameAs(bakery);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getBakery()).isSameAs(bakery);
    }

    @Test
    void createEmployee_rejectsDuplicateEmail() {
        CreateEmployeeRequest request = new CreateEmployeeRequest("employee@test.com", "temporary123", Role.DRIVER);
        when(userRepository.existsByEmail("employee@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userManagementService.createEmployee(UUID.randomUUID(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already in use");

        verify(bakeryRepository, never()).findById(any(UUID.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createEmployee_rejectsMissingBakery() {
        UUID bakeryId = UUID.randomUUID();
        CreateEmployeeRequest request = new CreateEmployeeRequest("employee@test.com", "temporary123", Role.DRIVER);
        when(userRepository.existsByEmail("employee@test.com")).thenReturn(false);
        when(bakeryRepository.findById(bakeryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.createEmployee(bakeryId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bakery not found");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createEmployeeRequest_rejectsSuperAdminRole() {
        assertThatThrownBy(() -> new CreateEmployeeRequest("employee@test.com", "temporary123", Role.SUPER_ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot create employee with role SUPER_ADMIN");
    }

    @Test
    void createEmployeeRequest_rejectsCustomerRole() {
        assertThatThrownBy(() -> new CreateEmployeeRequest("employee@test.com", "temporary123", Role.CUSTOMER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot create employee with role CUSTOMER");
    }

    @Test
    void createEmployeeRequest_allowsValidationToHandleNullRole() {
        CreateEmployeeRequest request = new CreateEmployeeRequest("employee@test.com", "temporary123", null);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Set<ConstraintViolation<CreateEmployeeRequest>> violations = factory.getValidator().validate(request);

            assertThat(violations)
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("role"));
        }
    }
}
