package com.piotrcapecki.bakelivery.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.piotrcapecki.bakelivery.messaging",
        "com.piotrcapecki.bakelivery.common"
})
public class MessagingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessagingServiceApplication.class, args);
    }
}
