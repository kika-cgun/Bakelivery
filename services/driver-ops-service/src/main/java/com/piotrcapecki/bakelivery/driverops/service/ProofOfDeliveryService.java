package com.piotrcapecki.bakelivery.driverops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProofOfDeliveryService {

    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${minio.bucket}") String bucket;
    @Value("${minio.presign-ttl-seconds}") long presignTtl;

    public String upload(UUID bakeryId, UUID driverId, LocalDate date,
                         UUID stopProgressId, MultipartFile file) throws IOException {
        String objectKey = buildObjectKey(bakeryId, driverId, date, stopProgressId);

        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(file.getContentType() != null ? file.getContentType() : "image/jpeg")
                        .contentLength(file.getSize())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        log.info("Uploaded proof of delivery: bucket={}, key={}", bucket, objectKey);
        return objectKey;
    }

    public String presignedGetUrl(String objectKey) {
        var req = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignTtl))
                .getObjectRequest(b -> b.bucket(bucket).key(objectKey))
                .build();
        return presigner.presignGetObject(req).url().toString();
    }

    private String buildObjectKey(UUID bakeryId, UUID driverId, LocalDate date, UUID stopProgressId) {
        return String.format("proofs/%s/%s/%s/%s.jpg", bakeryId, driverId, date, stopProgressId);
    }
}
