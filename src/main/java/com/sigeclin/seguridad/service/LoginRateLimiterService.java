package com.sigeclin.seguridad.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LoginRateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    
    // Expira después de 15 minutos desde el último intento fallido
    private final LoadingCache<String, Integer> attemptsCache;

    public LoginRateLimiterService() {
        this.attemptsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }

    public void loginFailed(String ip) {
        int attempts;
        try {
            attempts = attemptsCache.get(ip);
        } catch (ExecutionException e) {
            attempts = 0;
        }
        attempts++;
        attemptsCache.put(ip, attempts);
        log.warn("[RATE-LIMIT] Intento de login fallido desde IP: {}. Intentos acumulados: {}/{}", ip, attempts, MAX_ATTEMPTS);
    }

    public boolean isBlocked(String ip) {
        try {
            boolean blocked = attemptsCache.get(ip) >= MAX_ATTEMPTS;
            if (blocked) {
                log.warn("[RATE-LIMIT] IP bloqueada temporalmente intentando acceder: {}", ip);
            }
            return blocked;
        } catch (ExecutionException e) {
            return false;
        }
    }

    public void loginSucceeded(String ip) {
        attemptsCache.invalidate(ip);
        log.info("[RATE-LIMIT] Login exitoso. Reseteando intentos para IP: {}", ip);
    }
}
