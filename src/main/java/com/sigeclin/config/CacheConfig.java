package com.sigeclin.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // Este archivo actúa como un Switch maestro.
    // Al colocar @EnableCaching, Spring Boot buscará automáticamente cualquier
    // método con @Cacheable y memorizará sus resultados en RAM.
}
