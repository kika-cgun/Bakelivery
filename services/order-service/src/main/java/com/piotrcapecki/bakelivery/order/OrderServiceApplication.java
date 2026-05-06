package com.piotrcapecki.bakelivery.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "com.piotrcapecki.bakelivery.order",
        "com.piotrcapecki.bakelivery.common"
})
@EnableFeignClients(basePackages = "com.piotrcapecki.bakelivery.order.client")
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
