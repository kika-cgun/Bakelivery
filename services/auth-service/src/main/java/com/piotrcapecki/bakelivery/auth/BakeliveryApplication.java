package com.piotrcapecki.bakelivery.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.piotrcapecki.bakelivery.auth", "com.piotrcapecki.bakelivery.common"})
public class BakeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(BakeliveryApplication.class, args);
    }

}
