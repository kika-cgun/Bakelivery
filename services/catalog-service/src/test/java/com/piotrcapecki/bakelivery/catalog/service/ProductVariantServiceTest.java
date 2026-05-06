package com.piotrcapecki.bakelivery.catalog.service;

import com.piotrcapecki.bakelivery.catalog.dto.CreateProductRequest;
import com.piotrcapecki.bakelivery.catalog.dto.CreateVariantRequest;
import com.piotrcapecki.bakelivery.catalog.dto.UpdateVariantRequest;
import com.piotrcapecki.bakelivery.catalog.dto.VariantResponse;
import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ProductVariantServiceTest {

    @Autowired ProductVariantService variantService;
    @Autowired ProductService productService;

    @Test
    void variantCrudWithConflictsAndTenancy() {
        UUID bakery = UUID.randomUUID();
        UUID otherBakery = UUID.randomUUID();

        // Create a product
        var product = productService.create(bakery,
                new CreateProductRequest(null, null, "test-prod-" + UUID.randomUUID(),
                        "Test Product", null, new BigDecimal("10.00"), null));

        // Create variant
        var req = new CreateVariantRequest("Large", null, new BigDecimal("2.00"), 1);
        var variant = variantService.create(bakery, product.id(), req);
        assertThat(variant.name()).isEqualTo("Large");
        assertThat(variant.priceDelta()).isEqualByComparingTo("2.00");

        // Duplicate name throws ConflictException
        assertThatThrownBy(() -> variantService.create(bakery, product.id(), req))
                .isInstanceOf(ConflictException.class);

        // Update
        var updated = variantService.update(bakery, variant.id(),
                new UpdateVariantRequest("Extra Large", null, new BigDecimal("3.50"), null));
        assertThat(updated.name()).isEqualTo("Extra Large");

        // Other bakery can't access variant
        assertThatThrownBy(() -> variantService.update(otherBakery, variant.id(),
                new UpdateVariantRequest("Hijacked", null, null, null)))
                .isInstanceOf(NotFoundException.class);

        // Delete
        variantService.delete(bakery, variant.id());
        assertThat(variantService.listForProduct(bakery, product.id(), PageRequest.of(0, 20)).getContent()).isEmpty();

        // Delete non-existent
        assertThatThrownBy(() -> variantService.delete(bakery, UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }
}
