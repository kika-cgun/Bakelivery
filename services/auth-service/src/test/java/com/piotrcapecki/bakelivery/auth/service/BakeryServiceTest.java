package com.piotrcapecki.bakelivery.auth.service;

import com.piotrcapecki.bakelivery.auth.dto.CreateBakeryRequest;
import com.piotrcapecki.bakelivery.auth.model.Bakery;
import com.piotrcapecki.bakelivery.auth.model.Role;
import com.piotrcapecki.bakelivery.auth.model.User;
import com.piotrcapecki.bakelivery.auth.repository.BakeryRepository;
import com.piotrcapecki.bakelivery.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.piotrcapecki.bakelivery.common.exception.ConflictException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BakeryServiceTest {

    @Mock BakeryRepository bakeryRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks BakeryService bakeryService;

    @Test
    void createBakeryWithFirstAdmin_createsBakeryAndAdmin() {
        CreateBakeryRequest req = new CreateBakeryRequest(
                "Sweet Rolls", "sweet-rolls",
                "contact@sweet-rolls.pl", "+48123456789",
                "admin@sweet-rolls.pl", "securePass1"
        );
        when(bakeryRepository.existsBySlug("sweet-rolls")).thenReturn(false);
        when(userRepository.existsByEmail("admin@sweet-rolls.pl")).thenReturn(false);
        when(passwordEncoder.encode("securePass1")).thenReturn("encoded-securePass1");
        when(bakeryRepository.save(any(Bakery.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Bakery result = bakeryService.createBakeryWithFirstAdmin(req);

        assertThat(result.getName()).isEqualTo("Sweet Rolls");
        assertThat(result.getSlug()).isEqualTo("sweet-rolls");
        assertThat(result.getContactEmail()).isEqualTo("contact@sweet-rolls.pl");
        assertThat(result.getContactPhone()).isEqualTo("+48123456789");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedAdmin = userCaptor.getValue();
        assertThat(savedAdmin.getEmail()).isEqualTo("admin@sweet-rolls.pl");
        assertThat(savedAdmin.getPasswordHash()).isEqualTo("encoded-securePass1");
        assertThat(savedAdmin.getRole()).isEqualTo(Role.BAKERY_ADMIN);
        assertThat(savedAdmin.getBakery()).isNotNull();
        assertThat(savedAdmin.getBakery().getSlug()).isEqualTo("sweet-rolls");
    }

    @Test
    void createBakeryWithFirstAdmin_rejectsDuplicateSlug() {
        CreateBakeryRequest req = new CreateBakeryRequest(
                "Sweet Rolls", "sweet-rolls",
                "contact@sweet-rolls.pl", null,
                "admin@sweet-rolls.pl", "securePass1"
        );
        when(bakeryRepository.existsBySlug("sweet-rolls")).thenReturn(true);

        assertThatThrownBy(() -> bakeryService.createBakeryWithFirstAdmin(req))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Slug already in use");

        verify(bakeryRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createBakeryWithFirstAdmin_rejectsDuplicateAdminEmail() {
        CreateBakeryRequest req = new CreateBakeryRequest(
                "Sweet Rolls", "sweet-rolls",
                "contact@sweet-rolls.pl", null,
                "admin@sweet-rolls.pl", "securePass1"
        );
        when(bakeryRepository.existsBySlug("sweet-rolls")).thenReturn(false);
        when(userRepository.existsByEmail("admin@sweet-rolls.pl")).thenReturn(true);

        assertThatThrownBy(() -> bakeryService.createBakeryWithFirstAdmin(req))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already in use");

        verify(bakeryRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

}
