package com.piotrcapecki.bakelivery.messaging.repository;

import com.piotrcapecki.bakelivery.messaging.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByThreadIdOrderByCreatedAtAsc(UUID threadId, Pageable pageable);
}
