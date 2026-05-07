package com.piotrcapecki.bakelivery.customer.service;

import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.customer.client.MapsClient;
import com.piotrcapecki.bakelivery.customer.dto.CreateAddressRequest;
import com.piotrcapecki.bakelivery.customer.dto.UpdateAddressRequest;
import com.piotrcapecki.bakelivery.customer.model.Customer;
import com.piotrcapecki.bakelivery.customer.model.CustomerType;
import com.piotrcapecki.bakelivery.customer.model.DeliveryAddress;
import com.piotrcapecki.bakelivery.customer.repository.CustomerRepository;
import com.piotrcapecki.bakelivery.customer.repository.DeliveryAddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryAddressServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock DeliveryAddressRepository addressRepository;
    @Mock MapsClient mapsClient;
    @InjectMocks DeliveryAddressService service;

    @Test
    void list_throwsWhenProfileMissing() {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        when(customerRepository.findByUserIdAndBakeryId(uid, bid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list(uid, bid)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void list_returnsAddressesForCustomer() {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        Customer c = Customer.builder().id(UUID.randomUUID()).userId(uid).bakeryId(bid).type(CustomerType.INDIVIDUAL).build();
        when(customerRepository.findByUserIdAndBakeryId(uid, bid)).thenReturn(Optional.of(c));
        DeliveryAddress a = DeliveryAddress.builder().customerId(c.getId()).bakeryId(bid).street("ul. Pierwsza 1").postalCode("00-001").city("Warszawa").build();
        when(addressRepository.findAllByCustomerId(c.getId())).thenReturn(List.of(a));

        List<DeliveryAddress> result = service.list(uid, bid);

        assertThat(result).hasSize(1).contains(a);
    }

    @Test
    void create_attachesCustomerIdAndBakeryId() {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        when(customerRepository.findByUserIdAndBakeryId(uid, bid)).thenReturn(
                Optional.of(Customer.builder().id(cid).userId(uid).bakeryId(bid).type(CustomerType.INDIVIDUAL).build()));
        when(addressRepository.save(any(DeliveryAddress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapsClient.geocode(any())).thenReturn(new MapsClient.GeocodeResult(52.229, 21.012, "Warszawa, PL", false));

        DeliveryAddress result = service.create(uid, bid, new CreateAddressRequest("Dom", "ul. Pierwsza 1", "00-001", "Warszawa", null, null));

        assertThat(result.getCustomerId()).isEqualTo(cid);
        assertThat(result.getBakeryId()).isEqualTo(bid);
        assertThat(result.getStreet()).isEqualTo("ul. Pierwsza 1");
    }

    @Test
    void create_setsLatLonFromGeocoding() {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        when(customerRepository.findByUserIdAndBakeryId(uid, bid)).thenReturn(
                Optional.of(Customer.builder().id(cid).userId(uid).bakeryId(bid).type(CustomerType.INDIVIDUAL).build()));
        when(addressRepository.save(any(DeliveryAddress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapsClient.geocode(any())).thenReturn(new MapsClient.GeocodeResult(52.229, 21.012, "Warszawa, PL", false));

        DeliveryAddress result = service.create(uid, bid, new CreateAddressRequest("Dom", "ul. Pierwsza 1", "00-001", "Warszawa", null, null));

        assertThat(result.getLatitude()).isEqualTo(52.229);
        assertThat(result.getLongitude()).isEqualTo(21.012);
    }

    @Test
    void create_geocodingFails_saveAddressWithoutCoordinates() {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        when(customerRepository.findByUserIdAndBakeryId(uid, bid)).thenReturn(
                Optional.of(Customer.builder().id(cid).userId(uid).bakeryId(bid).type(CustomerType.INDIVIDUAL).build()));
        when(addressRepository.save(any(DeliveryAddress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapsClient.geocode(any())).thenThrow(new RuntimeException("maps-service unavailable"));

        DeliveryAddress result = service.create(uid, bid, new CreateAddressRequest("Dom", "ul. Pierwsza 1", "00-001", "Warszawa", null, null));

        assertThat(result.getCustomerId()).isEqualTo(cid);
        assertThat(result.getLatitude()).isNull();
        assertThat(result.getLongitude()).isNull();
        verify(addressRepository).save(any(DeliveryAddress.class));
    }

    @Test
    void update_throwsWhenAddressBelongsToDifferentCustomer() {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        when(customerRepository.findByUserIdAndBakeryId(uid, bid)).thenReturn(
                Optional.of(Customer.builder().id(cid).userId(uid).bakeryId(bid).type(CustomerType.INDIVIDUAL).build()));
        UUID addressId = UUID.randomUUID();
        when(addressRepository.findByIdAndCustomerId(addressId, cid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(uid, bid, addressId,
                new UpdateAddressRequest(null, "ul. X 1", "00-000", "Warszawa", null, null)))
                .isInstanceOf(NotFoundException.class);

        verify(addressRepository, never()).save(any());
    }

    @Test
    void delete_callsRepositoryDelete() {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        when(customerRepository.findByUserIdAndBakeryId(uid, bid)).thenReturn(
                Optional.of(Customer.builder().id(cid).userId(uid).bakeryId(bid).type(CustomerType.INDIVIDUAL).build()));
        DeliveryAddress a = DeliveryAddress.builder().id(UUID.randomUUID()).customerId(cid).bakeryId(bid).street("x").postalCode("00-001").city("Warszawa").build();
        when(addressRepository.findByIdAndCustomerId(a.getId(), cid)).thenReturn(Optional.of(a));

        service.delete(uid, bid, a.getId());

        verify(addressRepository).delete(a);
    }
}
