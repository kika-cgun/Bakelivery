package com.piotrcapecki.bakelivery.maps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.piotrcapecki.bakelivery.maps", "com.piotrcapecki.bakelivery.common"})
public class MapsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MapsServiceApplication.class, args);
    }
}
