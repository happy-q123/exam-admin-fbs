package com.gateaway;

import jakarta.annotation.Resource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication

public class GateAwayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GateAwayApplication.class, args);
    }

}
