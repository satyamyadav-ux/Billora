package com.billora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BilloraApplication {

    public static void main(String[] args) {
        SpringApplication.run(BilloraApplication.class, args);
    }
}
