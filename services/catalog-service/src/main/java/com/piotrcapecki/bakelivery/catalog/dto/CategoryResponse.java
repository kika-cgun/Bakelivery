package com.piotrcapecki.bakelivery.catalog.dto;

import com.piotrcapecki.bakelivery.catalog.model.Category;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, String slug, int sortOrder) {
    public static CategoryResponse of(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getSortOrder());
    }
}
