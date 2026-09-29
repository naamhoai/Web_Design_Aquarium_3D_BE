package com.aquarium.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Giới hạn tần suất request theo IP (fixed window, lưu trong bộ nhớ) — không cần Redis.
 * Hai tầng: giới hạn chung cho mọi API và giới hạn chặt cho endpoint đăng nhập/đăng ký/làm mới token.
 * Trả 429 kèm header Retry-After.
 */
@Component
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final long SWEEP_EVERY_REQUESTS = 5_000;
    private static final long IDLE_EVICT_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final byte[] BODY = ("{\"success\":false,\"code\":1010,"
            + "\"message\":\"Bạn thao tác quá nhanh, vui lòng thử lại sau\"}").getBytes(StandardCharsets.UTF_8);

    private final RateLimitProperties properties;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();

    public RateLimitGlobalFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    private static final class Window {
        long start;
        long lastSeen;
        int count;

        Window(long now) {
            this.start = now;
            this.lastSeen = now;
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        long now = System.currentTimeMillis();
        String ip = clientIp(request);
        long retryAfter = consume("all:" + ip, properties.getDefaultLimit(), properties.getDefaultWindow(), now);
        if (retryAfter < 0 && properties.getAuthPaths().contains(request.getPath().value())) {
            retryAfter = consume("auth:" + ip, properties.getAuthLimit(), properties.getAuthWindow(), now);
        }
        sweepIfNeeded(now);

        if (retryAfter >= 0) {
            return reject(exchange.getResponse(), retryAfter);
        }
        return chain.filter(exchange);
    }

    /** @return -1 nếu được phép; ngược lại là số mili-giây cần chờ. */
    private long consume(String key, int limit, Duration window, long now) {
        Window w = windows.computeIfAbsent(key, k -> new Window(now));
        synchronized (w) {
            long windowMillis = window.toMillis();
            if (now - w.start >= windowMillis) {
                w.start = now;
                w.count = 0;
            }
            w.lastSeen = now;
            w.count++;
            if (w.count <= limit) {
                return -1;
            }
            return Math.max(0, windowMillis - (now - w.start));
        }
    }

    private void sweepIfNeeded(long now) {
        if (requestCounter.incrementAndGet() % SWEEP_EVERY_REQUESTS == 0) {
            windows.entrySet().removeIf(entry -> {
                Window w = entry.getValue();
                synchronized (w) {
                    return now - w.lastSeen > IDLE_EVICT_MILLIS;
                }
            });
        }
    }

    private String clientIp(ServerHttpRequest request) {
        if (properties.isTrustForwardedFor()) {
            String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] parts = forwarded.split(",");
                return parts[parts.length - 1].trim();
            }
        }
        InetSocketAddress remote = request.getRemoteAddress();
        return remote != null && remote.getAddress() != null ? remote.getAddress().getHostAddress() : "unknown";
    }

    private Mono<Void> reject(ServerHttpResponse response, long retryAfterMillis) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        HttpHeaders headers = response.getHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, (retryAfterMillis + 999) / 1000)));
        DataBuffer buffer = response.bufferFactory().wrap(BODY);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Chạy sớm, trước khi route request tới service phía sau
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
