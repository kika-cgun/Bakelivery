package com.piotrcapecki.bakelivery.driverops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DriverOpsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DriverOpsServiceApplication.class, args);
    }
}
