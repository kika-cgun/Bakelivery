package com.piotrcapecki.bakelivery.dispatching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.piotrcapecki.bakelivery.dispatching",
        "com.piotrcapecki.bakelivery.common"
})
@EnableScheduling
public class DispatchingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DispatchingServiceApplication.class, args);
    }
}
