package com.aquarium.identity.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chống dò mật khẩu (brute-force / credential stuffing) và spam đăng ký.
 * - Sai mật khẩu 5 lần / 15 phút cho cùng email  -> khóa email 15 phút.
 * - Sai 20 lần / 15 phút từ cùng IP                -> khóa IP 15 phút.
 * - Tối đa 10 lần đăng ký / giờ / IP.
 * Lưu trong bộ nhớ (phù hợp 1 instance); khi scale nhiều instance nên chuyển sang Redis.
 */
@Component
public class LoginAttemptService {

    static final int MAX_FAILURES_PER_EMAIL = 5;
    static final int MAX_FAILURES_PER_IP = 20;
    static final int MAX_REGISTRATIONS_PER_IP = 10;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration REGISTRATION_WINDOW = Duration.ofHours(1);
    private static final int MAX_TRACKED_KEYS = 100_000;

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    private static final class Counter {
        int count;
        Instant windowStart;
        Instant lockedUntil;

        Counter(Instant now) {
            this.windowStart = now;
        }
    }

    public void checkLoginAllowed(String email, String ip) {
        Instant now = Instant.now();
        if (isLocked("email:" + email, now) || isLocked("ip:" + ip, now)) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    public void recordLoginFailure(String email, String ip) {
        Instant now = Instant.now();
        increment("email:" + email, now, FAILURE_WINDOW, MAX_FAILURES_PER_EMAIL, LOCK_DURATION);
        increment("ip:" + ip, now, FAILURE_WINDOW, MAX_FAILURES_PER_IP, LOCK_DURATION);
    }

    public void recordLoginSuccess(String email) {
        counters.remove("email:" + email);
    }

    public void checkRegistrationAllowed(String ip) {
        Counter counter = counters.get("reg:" + ip);
        Instant now = Instant.now();
        if (counter != null) {
            synchronized (counter) {
                if (counter.windowStart.plus(REGISTRATION_WINDOW).isAfter(now) && counter.count >= MAX_REGISTRATIONS_PER_IP) {
                    throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
                }
            }
        }
    }

    public void recordRegistration(String ip) {
        increment("reg:" + ip, Instant.now(), REGISTRATION_WINDOW, Integer.MAX_VALUE, Duration.ZERO);
    }

    private boolean isLocked(String key, Instant now) {
        Counter counter = counters.get(key);
        if (counter == null) {
            return false;
        }
        synchronized (counter) {
            return counter.lockedUntil != null && counter.lockedUntil.isAfter(now);
        }
    }

    private void increment(String key, Instant now, Duration window, int threshold, Duration lock) {
        if (counters.size() >= MAX_TRACKED_KEYS) {
            purgeStale();
            if (counters.size() >= MAX_TRACKED_KEYS && !counters.containsKey(key)) {
                return; // tránh bị làm đầy bộ nhớ; giới hạn theo IP ở gateway vẫn còn hiệu lực
            }
        }
        Counter counter = counters.computeIfAbsent(key, k -> new Counter(now));
        synchronized (counter) {
            if (counter.windowStart.plus(window).isBefore(now)) {
                counter.count = 0;
                counter.windowStart = now;
            }
            counter.count++;
            if (counter.count >= threshold && !lock.isZero()) {
                counter.lockedUntil = now.plus(lock);
                counter.count = 0;
                counter.windowStart = now;
            }
        }
    }

    @Scheduled(fixedDelay = 600_000)
    public void purgeStale() {
        Instant now = Instant.now();
        Duration maxAge = REGISTRATION_WINDOW.compareTo(FAILURE_WINDOW) > 0 ? REGISTRATION_WINDOW : FAILURE_WINDOW;
        counters.entrySet().removeIf(entry -> {
            Counter c = entry.getValue();
            synchronized (c) {
                boolean lockExpired = c.lockedUntil == null || c.lockedUntil.isBefore(now);
                return lockExpired && c.windowStart.plus(maxAge).isBefore(now);
            }
        });
    }
}
