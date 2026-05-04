package com.piotrcapecki.bakelivery.customer.service;

import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.customer.dto.UpsertProfileRequest;
import com.piotrcapecki.bakelivery.customer.model.Customer;
import com.piotrcapecki.bakelivery.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerProfileService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Customer getProfile(UUID userId, UUID bakeryId) {
        return customerRepository.findByUserIdAndBakeryId(userId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
    }

    @Transactional
    public Customer upsertProfile(UUID userId, UUID bakeryId, UpsertProfileRequest req) {
        Customer customer = customerRepository.findByUserIdAndBakeryId(userId, bakeryId)
                .orElseGet(() -> Customer.builder().userId(userId).bakeryId(bakeryId).build());

        customer.setType(req.type());
        customer.setFirstName(req.firstName());
        customer.setLastName(req.lastName());
        customer.setPhone(req.phone());
        customer.setCompanyName(req.companyName());
        customer.setVatId(req.vatId());
        customer.setBillingAddress(req.billingAddress());

        return customerRepository.save(customer);
    }
}
