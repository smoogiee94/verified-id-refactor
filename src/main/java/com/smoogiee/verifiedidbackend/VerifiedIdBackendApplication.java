package com.smoogiee.verifiedidbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class VerifiedIdBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(VerifiedIdBackendApplication.class, args);
    }
}
