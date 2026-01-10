package com.karaoke;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class KaraokeBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KaraokeBeApplication.class, args);
    }
}
