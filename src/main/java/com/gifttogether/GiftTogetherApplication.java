package com.gifttogether;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GiftTogetherApplication {

    public static void main(String[] args) {
        SpringApplication.run(GiftTogetherApplication.class, args);
    }
}