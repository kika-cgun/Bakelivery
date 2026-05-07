package com.piotrcapecki.bakelivery.customer.service;

import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.customer.client.MapsClient;
import com.piotrcapecki.bakelivery.customer.dto.CreateAddressRequest;
import com.piotrcapecki.bakelivery.customer.dto.UpdateAddressRequest;
import com.piotrcapecki.bakelivery.customer.model.Customer;
import com.piotrcapecki.bakelivery.customer.model.DeliveryAddress;
import com.piotrcapecki.bakelivery.customer.repository.CustomerRepository;
import com.piotrcapecki.bakelivery.customer.repository.DeliveryAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryAddressService {

    private final CustomerRepository customerRepository;
    private final DeliveryAddressRepository addressRepository;
    private final MapsClient mapsClient;

    @Transactional(readOnly = true)
    public List<DeliveryAddress> list(UUID userId, UUID bakeryId) {
        Customer c = requireCustomer(userId, bakeryId);
        return addressRepository.findAllByCustomerId(c.getId());
    }

    @Transactional
    public DeliveryAddress create(UUID userId, UUID bakeryId, CreateAddressRequest req) {
        Customer c = requireCustomer(userId, bakeryId);
        DeliveryAddress a = DeliveryAddress.builder()
                .customerId(c.getId())
                .bakeryId(bakeryId)
                .label(req.label())
                .street(req.street())
                .postalCode(req.postalCode())
                .city(req.city())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .build();
        enrichWithGeocode(a, req.street(), req.city(), req.postalCode());
        return addressRepository.save(a);
    }

    @Transactional
    public DeliveryAddress update(UUID userId, UUID bakeryId, UUID addressId, UpdateAddressRequest req) {
        Customer c = requireCustomer(userId, bakeryId);
        DeliveryAddress a = addressRepository.findByIdAndCustomerId(addressId, c.getId())
                .orElseThrow(() -> new NotFoundException("Address not found"));
        a.setLabel(req.label());
        a.setStreet(req.street());
        a.setPostalCode(req.postalCode());
        a.setCity(req.city());
        a.setLatitude(req.latitude());
        a.setLongitude(req.longitude());
        enrichWithGeocode(a, req.street(), req.city(), req.postalCode());
        return addressRepository.save(a);
    }

    @Transactional
    public void delete(UUID userId, UUID bakeryId, UUID addressId) {
        Customer c = requireCustomer(userId, bakeryId);
        DeliveryAddress a = addressRepository.findByIdAndCustomerId(addressId, c.getId())
                .orElseThrow(() -> new NotFoundException("Address not found"));
        addressRepository.delete(a);
    }

    private void enrichWithGeocode(DeliveryAddress address, String street, String city, String postalCode) {
        try {
            String addressString = buildAddressString(street, city, postalCode);
            MapsClient.GeocodeResult result = mapsClient.geocode(new MapsClient.GeocodePayload(addressString));
            address.setLatitude(result.lat());
            address.setLongitude(result.lon());
        } catch (Exception e) {
            log.warn("Geocoding failed for address, saving without coordinates: {}", e.getMessage());
        }
    }

    private String buildAddressString(String street, String city, String postalCode) {
        return String.join(", ", street, city, postalCode);
    }

    private Customer requireCustomer(UUID userId, UUID bakeryId) {
        return customerRepository.findByUserIdAndBakeryId(userId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Profile not found — create it via PUT /api/customer/profile first"));
    }
}
