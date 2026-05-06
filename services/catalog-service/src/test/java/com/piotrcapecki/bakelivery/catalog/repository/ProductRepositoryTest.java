package com.piotrcapecki.bakelivery.catalog.repository;

import com.piotrcapecki.bakelivery.catalog.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired ProductRepository repo;

    @Test
    void slugUniquenessIsScopedToBakery() {
        UUID b1 = UUID.randomUUID();
        UUID b2 = UUID.randomUUID();
        repo.save(Product.builder().bakeryId(b1).slug("bread").name("Bread").basePrice(new BigDecimal("5.00")).build());
        repo.save(Product.builder().bakeryId(b2).slug("bread").name("Bread").basePrice(new BigDecimal("5.00")).build());

        assertThat(repo.existsBySlugAndBakeryId("bread", b1)).isTrue();
        assertThat(repo.existsBySlugAndBakeryId("bread", b2)).isTrue();
        assertThat(repo.existsBySlugAndBakeryId("bread", UUID.randomUUID())).isFalse();
    }

    @Test
    void activeFilterWorksCorrectly() {
        UUID bakery = UUID.randomUUID();
        repo.save(Product.builder().bakeryId(bakery).slug("active-p").name("Active").basePrice(new BigDecimal("5.00")).active(true).build());
        repo.save(Product.builder().bakeryId(bakery).slug("inactive-p").name("Inactive").basePrice(new BigDecimal("5.00")).active(false).build());

        assertThat(repo.findAllByBakeryIdAndActiveTrueOrderByNameAsc(bakery)).hasSize(1);
        assertThat(repo.findAllByBakeryIdOrderByNameAsc(bakery)).hasSize(2);
    }

    @Test
    void availableDaysDefaultIs127() {
        UUID bakery = UUID.randomUUID();
        Product p = repo.save(Product.builder().bakeryId(bakery).slug("default-days").name("P").basePrice(new BigDecimal("1.00")).build());
        assertThat(p.getAvailableDays()).isEqualTo((short) 127);
    }
}
