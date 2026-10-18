package com.handgelus;

import com.handgelus.infrastructure.config.HandgelusProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * HandgelusApp · Sistema de gestión de tatuaje de autor y arte por comisión.
 * Arquitectura hexagonal: domain (núcleo) ← application (casos de uso) ← infrastructure (adaptadores).
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(HandgelusProperties.class)
public class HandgelusApplication {

    public static void main(String[] args) {
        SpringApplication.run(HandgelusApplication.class, args);
    }
}
