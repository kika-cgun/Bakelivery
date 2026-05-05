package com.piotrcapecki.bakelivery.customer.repository;

import com.piotrcapecki.bakelivery.customer.model.Customer;
import com.piotrcapecki.bakelivery.customer.model.CustomerType;
import com.piotrcapecki.bakelivery.customer.model.DeliveryAddress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class DeliveryAddressRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired CustomerRepository customerRepository;
    @Autowired DeliveryAddressRepository addressRepository;

    @Test
    void findAllByCustomerId_returnsOnlyThatCustomerAddresses() {
        UUID bakeryId = UUID.randomUUID();
        Customer c1 = customerRepository.save(Customer.builder().userId(UUID.randomUUID()).bakeryId(bakeryId).type(CustomerType.INDIVIDUAL).build());
        Customer c2 = customerRepository.save(Customer.builder().userId(UUID.randomUUID()).bakeryId(bakeryId).type(CustomerType.INDIVIDUAL).build());
        addressRepository.save(DeliveryAddress.builder().customerId(c1.getId()).bakeryId(bakeryId).street("ul. Pierwsza 1").postalCode("00-001").city("Warszawa").build());
        addressRepository.save(DeliveryAddress.builder().customerId(c1.getId()).bakeryId(bakeryId).street("ul. Druga 2").postalCode("00-002").city("Warszawa").build());
        addressRepository.save(DeliveryAddress.builder().customerId(c2.getId()).bakeryId(bakeryId).street("ul. Trzecia 3").postalCode("00-003").city("Warszawa").build());

        List<DeliveryAddress> c1Addresses = addressRepository.findAllByCustomerId(c1.getId());

        assertThat(c1Addresses).hasSize(2);
        assertThat(c1Addresses).extracting(DeliveryAddress::getStreet).containsExactlyInAnyOrder("ul. Pierwsza 1", "ul. Druga 2");
    }

    @Test
    void findByIdAndCustomerId_filtersByOwner() {
        UUID bakeryId = UUID.randomUUID();
        Customer c1 = customerRepository.save(Customer.builder().userId(UUID.randomUUID()).bakeryId(bakeryId).type(CustomerType.INDIVIDUAL).build());
        Customer c2 = customerRepository.save(Customer.builder().userId(UUID.randomUUID()).bakeryId(bakeryId).type(CustomerType.INDIVIDUAL).build());
        DeliveryAddress saved = addressRepository.save(DeliveryAddress.builder().customerId(c1.getId()).bakeryId(bakeryId).street("ul. Pierwsza 1").postalCode("00-001").city("Warszawa").build());

        assertThat(addressRepository.findByIdAndCustomerId(saved.getId(), c1.getId())).isPresent();
        assertThat(addressRepository.findByIdAndCustomerId(saved.getId(), c2.getId())).isEmpty();
    }

    @Test
    void deleteAddress_removesOnlyTargetAddress() {
        UUID bakeryId = UUID.randomUUID();
        Customer c1 = customerRepository.save(Customer.builder().userId(UUID.randomUUID()).bakeryId(bakeryId).type(CustomerType.INDIVIDUAL).build());
        DeliveryAddress a1 = addressRepository.save(DeliveryAddress.builder().customerId(c1.getId()).bakeryId(bakeryId).street("ul. Pierwsza 1").postalCode("00-001").city("Warszawa").build());
        DeliveryAddress a2 = addressRepository.save(DeliveryAddress.builder().customerId(c1.getId()).bakeryId(bakeryId).street("ul. Druga 2").postalCode("00-002").city("Warszawa").build());

        addressRepository.delete(a1);

        assertThat(addressRepository.findAllByCustomerId(c1.getId())).hasSize(1)
                .extracting(DeliveryAddress::getId).containsExactly(a2.getId());
    }
}
