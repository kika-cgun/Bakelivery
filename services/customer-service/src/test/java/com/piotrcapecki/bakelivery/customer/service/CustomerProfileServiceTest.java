package com.piotrcapecki.bakelivery.customer.service;

import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.customer.dto.UpsertProfileRequest;
import com.piotrcapecki.bakelivery.customer.model.Customer;
import com.piotrcapecki.bakelivery.customer.model.CustomerType;
import com.piotrcapecki.bakelivery.customer.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {

    @Mock CustomerRepository customerRepository;
    @InjectMocks CustomerProfileService service;

    @Test
    void getProfile_throwsWhenMissing() {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        when(customerRepository.findByUserIdAndBakeryId(uid, bid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(uid, bid))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void upsertProfile_createsNewWhenMissing() {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        when(customerRepository.findByUserIdAndBakeryId(uid, bid)).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer result = service.upsertProfile(uid, bid,
                new UpsertProfileRequest(CustomerType.INDIVIDUAL, "Anna", "Kowalska", "+48 600 000 000", null, null, null));

        ArgumentCaptor<Customer> cap = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(cap.capture());
        Customer saved = cap.getValue();
        assertThat(saved.getUserId()).isEqualTo(uid);
        assertThat(saved.getBakeryId()).isEqualTo(bid);
        assertThat(saved.getType()).isEqualTo(CustomerType.INDIVIDUAL);
        assertThat(saved.getFirstName()).isEqualTo("Anna");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void upsertProfile_updatesWhenExists() {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        Customer existing = Customer.builder().id(UUID.randomUUID()).userId(uid).bakeryId(bid).type(CustomerType.INDIVIDUAL).firstName("Stary").build();
        when(customerRepository.findByUserIdAndBakeryId(uid, bid)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        service.upsertProfile(uid, bid,
                new UpsertProfileRequest(CustomerType.INDIVIDUAL, "Nowy", "Kowalski", null, null, null, null));

        assertThat(existing.getFirstName()).isEqualTo("Nowy");
    }

    @Test
    void upsertProfileRequest_companyTypeRequiresCompanyNameAndVatId() {
        assertThatThrownBy(() -> new UpsertProfileRequest(CustomerType.COMPANY, null, null, null, null, "PL123", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("companyName required");
        assertThatThrownBy(() -> new UpsertProfileRequest(CustomerType.COMPANY, null, null, null, "Acme", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vatId required");
    }
}
