package com.piotrcapecki.bakelivery.catalog.service;

import com.piotrcapecki.bakelivery.catalog.dto.CreateCategoryRequest;
import com.piotrcapecki.bakelivery.catalog.dto.UpdateCategoryRequest;
import com.piotrcapecki.bakelivery.catalog.repository.CategoryRepository;
import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CategoryServiceTest {

    @Autowired CategoryService service;
    @Autowired CategoryRepository repo;

    @Test
    void createListUpdateDelete() {
        UUID bakery = UUID.randomUUID();
        var created = service.create(bakery, new CreateCategoryRequest("Bread", "bread", 1));
        assertThat(created.slug()).isEqualTo("bread");

        assertThatThrownBy(() -> service.create(bakery, new CreateCategoryRequest("Bread2", "bread", 2)))
                .isInstanceOf(ConflictException.class);

        var updated = service.update(bakery, created.id(), new UpdateCategoryRequest("Sourdough", null, 5));
        assertThat(updated.name()).isEqualTo("Sourdough");
        assertThat(updated.sortOrder()).isEqualTo(5);

        service.delete(bakery, created.id());
        assertThat(service.list(bakery, PageRequest.of(0, 20)).getContent()).isEmpty();

        assertThatThrownBy(() -> service.delete(bakery, created.id())).isInstanceOf(NotFoundException.class);
    }
}
