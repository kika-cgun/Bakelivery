package com.piotrcapecki.bakelivery.auth.service;

import com.piotrcapecki.bakelivery.auth.dto.CreateEmployeeRequest;
import com.piotrcapecki.bakelivery.auth.model.Bakery;
import com.piotrcapecki.bakelivery.auth.model.User;
import com.piotrcapecki.bakelivery.auth.repository.BakeryRepository;
import com.piotrcapecki.bakelivery.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final BakeryRepository bakeryRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> listEmployees(UUID bakeryId) {
        return userRepository.findAllByBakeryId(bakeryId);
    }

    @Transactional
    public User createEmployee(UUID actingBakeryId, CreateEmployeeRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already in use");
        }
        Bakery bakery = actingBakeryId == null
                ? null
                : bakeryRepository.findById(actingBakeryId).orElse(null);
        if (bakery == null) {
            throw new IllegalArgumentException("Bakery not found");
        }
        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.temporaryPassword()))
                .role(req.role())
                .bakery(bakery)
                .build();
        return userRepository.save(user);
    }
}
