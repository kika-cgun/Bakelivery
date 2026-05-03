package com.piotrcapecki.bakelivery.auth.service;

import com.piotrcapecki.bakelivery.auth.dto.CreateBakeryRequest;
import com.piotrcapecki.bakelivery.auth.model.Bakery;
import com.piotrcapecki.bakelivery.auth.model.Role;
import com.piotrcapecki.bakelivery.auth.model.User;
import com.piotrcapecki.bakelivery.auth.repository.BakeryRepository;
import com.piotrcapecki.bakelivery.auth.repository.UserRepository;
import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BakeryService {

    private final BakeryRepository bakeryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Bakery createBakeryWithFirstAdmin(CreateBakeryRequest req) {
        if (bakeryRepository.existsBySlug(req.slug())) {
            throw new ConflictException("Slug already in use");
        }
        if (userRepository.existsByEmail(req.firstAdminEmail())) {
            throw new ConflictException("Email already in use");
        }

        Bakery bakery = bakeryRepository.save(Bakery.builder()
                .name(req.name())
                .slug(req.slug())
                .contactEmail(req.contactEmail())
                .contactPhone(req.contactPhone())
                .build());

        userRepository.save(User.builder()
                .email(req.firstAdminEmail())
                .passwordHash(passwordEncoder.encode(req.firstAdminPassword()))
                .role(Role.BAKERY_ADMIN)
                .bakery(bakery)
                .build());

        return bakery;
    }
}
