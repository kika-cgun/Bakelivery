package com.piotrcapecki.bakelivery.catalog.dto;

import java.time.Instant;
import java.util.UUID;

public record MediaResponse(UUID id, String url, String contentType, long sizeBytes, int sortOrder, boolean primary, Instant expiresAt) {}
