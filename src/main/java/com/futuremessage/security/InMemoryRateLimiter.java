package com.futuremessage.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.window = window;
    }

    public boolean tryAcquire(String key, Instant now) {
        long nowMs = now.toEpochMilli();
        long windowMs = window.toMillis();
        Window current = windows.compute(key, (ignored, existing) -> {
            if (existing == null || nowMs - existing.startMs() >= windowMs) {
                return new Window(nowMs, 1);
            }
            return new Window(existing.startMs(), existing.count() + 1);
        });
        return current.count() <= maxRequests;
    }

    public Duration window() {
        return window;
    }

    private record Window(long startMs, int count) {
    }
}
