package com.tech_mel.tech_mel.application.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private final Map<String, Bucket> regularBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    public Bucket getRegularBucket(String clientIp) {
        return regularBuckets.computeIfAbsent(clientIp, this::createRegularBucket);
    }

    private Bucket createRegularBucket(String key) {
        // 10 requisições a cada minuto
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    public Bucket getAuthBucket(String clientIp) {
        return authBuckets.computeIfAbsent(clientIp, this::createAuthBucket);
    }

    private Bucket createAuthBucket(String key) {
        // 5 requisições a cada 10 a cada minuto
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
