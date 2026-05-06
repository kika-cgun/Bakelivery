package com.piotrcapecki.bakelivery.catalog.repository;

import com.piotrcapecki.bakelivery.catalog.model.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoryRepositoryTest {

    @Autowired CategoryRepository repo;

    @Test
    void uniquenessIsScopedToBakery() {
        UUID b1 = UUID.randomUUID();
        UUID b2 = UUID.randomUUID();
        repo.save(Category.builder().bakeryId(b1).name("Bread").slug("bread").build());
        repo.save(Category.builder().bakeryId(b2).name("Bread").slug("bread").build());

        assertThat(repo.findAllByBakeryIdOrderBySortOrderAscNameAsc(b1)).hasSize(1);
        assertThat(repo.existsBySlugAndBakeryId("bread", b1)).isTrue();
        assertThat(repo.existsBySlugAndBakeryId("missing", b1)).isFalse();
    }
}
