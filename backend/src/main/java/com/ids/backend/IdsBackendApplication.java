package com.ids.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IdsBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdsBackendApplication.class, args);
    }
}
