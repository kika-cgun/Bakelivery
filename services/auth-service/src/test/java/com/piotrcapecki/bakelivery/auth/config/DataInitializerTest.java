package com.piotrcapecki.bakelivery.auth.config;

import com.piotrcapecki.bakelivery.auth.model.Bakery;
import com.piotrcapecki.bakelivery.auth.repository.BakeryRepository;
import com.piotrcapecki.bakelivery.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class DataInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private BakeryRepository bakeryRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private DataInitializer dataInitializer;

    @Test
    void runDoesNotLogPlaintextSeedPasswords(CapturedOutput output) {
        when(bakeryRepository.findBySlug("test-bakery"))
                .thenReturn(Optional.of(Bakery.builder()
                        .name("Test Bakery")
                        .slug("test-bakery")
                        .build()));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        dataInitializer.run(null);

        assertThat(output)
                .doesNotContain("super123")
                .doesNotContain("admin123")
                .doesNotContain("dispatch123")
                .doesNotContain("driver123")
                .doesNotContain("customer123");
    }
}
