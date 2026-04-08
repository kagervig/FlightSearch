package com.kristian.flightsearch;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window rate limiter keyed by an arbitrary string (typically IP + endpoint group).
 * Thread-safe; no external dependencies.
 */
public class RateLimiter {

    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Deque<Long>> requestTimes = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    /**
     * Returns true if the request is within the rate limit, false if it should be rejected.
     * Counts as a consumed request only when allowed.
     */
    public boolean isAllowed(String key) {
        long now = System.currentTimeMillis();
        long cutoff = now - windowMillis;
        boolean[] allowed = {false};

        requestTimes.compute(key, (k, times) -> {
            if (times == null) times = new ArrayDeque<>();
            while (!times.isEmpty() && times.peekFirst() < cutoff) {
                times.pollFirst();
            }
            if (times.size() < maxRequests) {
                times.addLast(now);
                allowed[0] = true;
            }
            return times;
        });

        return allowed[0];
    }
}
