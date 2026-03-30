package com.example.DoAn.util;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class RateLimitWindowStore {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000L; // 1 minute

    private final ConcurrentHashMap<String, Deque<Long>> store = new ConcurrentHashMap<>();

    /**
     * Returns true if request is allowed, false if rate limit exceeded.
     */
    public synchronized boolean isAllowed(String userId) {
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_MS;

        Deque<Long> timestamps = store.computeIfAbsent(userId, k -> new LinkedList<>());

        // Remove expired entries
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= MAX_REQUESTS) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    public void reset(String userId) {
        store.remove(userId);
    }

    public int getRemainingRequests(String userId) {
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_MS;

        Deque<Long> timestamps = store.getOrDefault(userId, new LinkedList<>());
        long validCount = timestamps.stream()
                .filter(ts -> ts >= cutoff)
                .count();
        return Math.max(0, MAX_REQUESTS - (int) validCount);
    }
}
