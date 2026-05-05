package com.piotrcapecki.bakelivery.catalog.service;

import com.piotrcapecki.bakelivery.catalog.dto.*;
import com.piotrcapecki.bakelivery.catalog.repository.ProductRepository;
import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ProductServiceTest {

    @Autowired ProductService service;
    @Autowired ProductRepository repo;

    @Test
    void crudFlow() {
        UUID bakery = UUID.randomUUID();
        var req = new CreateProductRequest(null, "SKU-1", "croissant",
                "Croissant", "Buttery", new BigDecimal("8.50"), (short) 127);
        var created = service.create(bakery, req);
        assertThat(created.slug()).isEqualTo("croissant");

        assertThatThrownBy(() -> service.create(bakery, req)).isInstanceOf(ConflictException.class);

        var updated = service.update(bakery, created.id(),
                new UpdateProductRequest(null, null, null, "Croissant XL", null,
                        new BigDecimal("9.00"), null, null));
        assertThat(updated.name()).isEqualTo("Croissant XL");
        assertThat(updated.basePrice()).isEqualByComparingTo("9.00");

        service.softDelete(bakery, created.id());
        assertThat(service.listActive(bakery)).isEmpty();
        assertThat(service.listAll(bakery)).hasSize(1);

        assertThatThrownBy(() -> service.softDelete(UUID.randomUUID(), created.id()))
                .isInstanceOf(NotFoundException.class);
    }
}
