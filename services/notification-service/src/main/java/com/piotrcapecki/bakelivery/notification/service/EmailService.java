package com.piotrcapecki.bakelivery.notification.service;

import com.piotrcapecki.bakelivery.notification.dto.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOrderConfirmation(OrderPlacedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.customerEmail());
        message.setFrom("no-reply@bakelivery.local");
        message.setSubject("Zamówienie #" + event.orderId() + " złożone");
        message.setText("Dziękujemy! Kwota: " + event.totalAmount() + " PLN.\n\n" +
                "Adres dostawy: " + event.deliveryAddress() + "\n" +
                "Data złożenia: " + event.placedAt());
        try {
            mailSender.send(message);
            log.info("Sent order confirmation email to {} for order {}", event.customerEmail(), event.orderId());
        } catch (Exception e) {
            log.error("Failed to send email for order {}: {}", event.orderId(), e.getMessage());
            throw e;
        }
    }
}
