package com.piotrcapecki.bakelivery.invoice.service;

import com.piotrcapecki.bakelivery.invoice.dto.OrderPlacedEvent;
import com.piotrcapecki.bakelivery.invoice.model.Invoice;
import com.piotrcapecki.bakelivery.invoice.model.InvoiceStatus;
import com.piotrcapecki.bakelivery.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepo;
    private final PdfGenerationService pdfGenerationService;
    private final S3Client s3Client;

    @Value("${minio.bucket}")
    private String bucket;

    public void processOrderPlaced(OrderPlacedEvent event) {
        Invoice invoice = Invoice.builder()
                .bakeryId(event.bakeryId())
                .orderId(event.orderId())
                .customerId(event.customerId())
                .status(InvoiceStatus.PENDING)
                .build();
        invoice = invoiceRepo.save(invoice);

        String objectKey = "invoices/" + event.bakeryId() + "/" + event.orderId() + ".pdf";

        try {
            byte[] pdfBytes = pdfGenerationService.generate(event);

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .contentType("application/pdf")
                            .contentLength((long) pdfBytes.length)
                            .build(),
                    RequestBody.fromBytes(pdfBytes));

            invoice.setStatus(InvoiceStatus.GENERATED);
            invoice.setObjectKey(objectKey);
            invoiceRepo.save(invoice);
            log.info("Invoice generated for order {} at {}", event.orderId(), objectKey);

        } catch (Exception e) {
            log.error("Failed to generate invoice for order {}: {}", event.orderId(), e.getMessage());
            invoice.setStatus(InvoiceStatus.FAILED);
            invoiceRepo.save(invoice);
            throw e;
        }
    }
}
