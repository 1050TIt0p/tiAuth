package ru.matveylegenda.tiauth.util;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class ServerAvailabilityChecker {
    private static final Map<SocketAddress, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private ServerAvailabilityChecker() {
    }

    public static CompletableFuture<Boolean> isReachable(SocketAddress address, long timeoutSeconds, long cacheSeconds) {
        long now = System.nanoTime();
        long timeoutMillis = TimeUnit.SECONDS.toMillis(Math.max(1, timeoutSeconds));
        long cacheTtlNanos = TimeUnit.SECONDS.toNanos(Math.max(0, cacheSeconds));

        return CACHE.compute(address, (key, cached) -> {
            if (cached == null || cached.hasDifferentSettings(timeoutMillis, cacheTtlNanos) || cached.isExpired(now)) {
                return new CacheEntry(probe(key, timeoutMillis), timeoutMillis, cacheTtlNanos);
            }
            return cached;
        }).result;
    }

    private static CompletableFuture<Boolean> probe(SocketAddress address, long timeoutMillis) {
        AsynchronousSocketChannel channel;
        try {
            channel = AsynchronousSocketChannel.open();
        } catch (IOException exception) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        try {
            channel.connect(address, null, new CompletionHandler<>() {
                @Override
                public void completed(Void unused, Object attachment) {
                    result.complete(true);
                }

                @Override
                public void failed(Throwable throwable, Object attachment) {
                    result.complete(false);
                }
            });
        } catch (RuntimeException exception) {
            result.complete(false);
        }

        return result
                .completeOnTimeout(false, timeoutMillis, TimeUnit.MILLISECONDS)
                .whenComplete((reachable, throwable) -> close(channel));
    }

    private static void close(AsynchronousSocketChannel channel) {
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }

    private static final class CacheEntry {
        private final CompletableFuture<Boolean> result = new CompletableFuture<>();
        private final long timeoutMillis;
        private final long cacheTtlNanos;
        private volatile long expiresAtNanos;

        private CacheEntry(CompletableFuture<Boolean> probe, long timeoutMillis, long cacheTtlNanos) {
            this.timeoutMillis = timeoutMillis;
            this.cacheTtlNanos = cacheTtlNanos;

            probe.whenComplete((reachable, throwable) -> {
                expiresAtNanos = System.nanoTime() + cacheTtlNanos;
                if (throwable == null) {
                    result.complete(reachable);
                } else {
                    result.completeExceptionally(throwable);
                }
            });
        }

        private boolean hasDifferentSettings(long timeoutMillis, long cacheTtlNanos) {
            return this.timeoutMillis != timeoutMillis || this.cacheTtlNanos != cacheTtlNanos;
        }

        private boolean isExpired(long now) {
            return result.isDone() && now - expiresAtNanos >= 0;
        }
    }
}
