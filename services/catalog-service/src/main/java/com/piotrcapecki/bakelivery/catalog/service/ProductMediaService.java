package com.piotrcapecki.bakelivery.catalog.service;

import com.piotrcapecki.bakelivery.catalog.dto.MediaResponse;
import com.piotrcapecki.bakelivery.catalog.model.MediaAsset;
import com.piotrcapecki.bakelivery.catalog.model.Product;
import com.piotrcapecki.bakelivery.catalog.repository.MediaAssetRepository;
import com.piotrcapecki.bakelivery.catalog.repository.ProductRepository;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductMediaService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final ProductRepository productRepo;
    private final MediaAssetRepository mediaRepo;
    private final MinioStorageService storage;

    @Transactional
    public MediaResponse upload(UUID bakeryId, UUID productId, MultipartFile file,
                                Integer sortOrder, boolean makePrimary) throws IOException {
        Product p = productRepo.findByIdAndBakeryId(productId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        if (file.getSize() > MAX_BYTES) throw new IllegalArgumentException("File exceeds 10MB limit");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }
        UUID id = UUID.randomUUID();
        String ext = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
        String key = "products/" + p.getBakeryId() + "/" + p.getId() + "/" + id + ext;
        storage.upload(key, contentType, file.getSize(), file.getInputStream());

        MediaAsset asset = MediaAsset.builder()
                .id(id)
                .bakeryId(p.getBakeryId())
                .productId(p.getId())
                .objectKey(key)
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .originalName(file.getOriginalFilename())
                .sortOrder(sortOrder == null ? 0 : sortOrder)
                .primary(makePrimary)
                .build();
        MediaAsset saved = mediaRepo.save(asset);
        if (makePrimary) {
            mediaRepo.unsetPrimaryExcept(p.getId(), saved.getId());
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> list(UUID bakeryId, UUID productId) {
        productRepo.findByIdAndBakeryId(productId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        return mediaRepo.findAllByProductIdOrderBySortOrderAscCreatedAtAsc(productId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(UUID bakeryId, UUID mediaId) {
        MediaAsset asset = mediaRepo.findByIdAndBakeryId(mediaId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Media not found"));
        storage.delete(asset.getObjectKey());
        mediaRepo.delete(asset);
    }

    private MediaResponse toResponse(MediaAsset m) {
        return new MediaResponse(
                m.getId(),
                storage.presignedGetUrl(m.getObjectKey()),
                m.getContentType(),
                m.getSizeBytes(),
                m.getSortOrder(),
                m.isPrimary());
    }
}
