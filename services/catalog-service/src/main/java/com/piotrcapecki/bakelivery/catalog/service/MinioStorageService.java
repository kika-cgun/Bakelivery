package com.piotrcapecki.bakelivery.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${minio.bucket}") private String bucket;
    @Value("${minio.presign-ttl-seconds}") private long presignTtl;

    public long getPresignTtlSeconds() { return presignTtl; }

    public void upload(String objectKey, String contentType, long size, InputStream content) throws IOException {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(contentType)
                        .contentLength(size)
                        .build(),
                RequestBody.fromInputStream(content, size));
    }

    public void delete(String objectKey) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
    }

    public String presignedGetUrl(String objectKey) {
        var req = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignTtl))
                .getObjectRequest(b -> b.bucket(bucket).key(objectKey))
                .build();
        return presigner.presignGetObject(req).url().toString();
    }
}
