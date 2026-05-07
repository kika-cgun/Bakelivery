package com.piotrcapecki.bakelivery.invoice.repository;

import com.piotrcapecki.bakelivery.invoice.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {}
