package com.tiger.paper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.tiger.paper"})
public class PaperApplication {

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(PaperApplication.class, args);
    }

}
