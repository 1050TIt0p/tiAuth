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
    private static final long CONNECT_TIMEOUT_MILLIS = 3000;
    private static final long CACHE_TTL_NANOS = TimeUnit.SECONDS.toNanos(3);
    private static final Map<SocketAddress, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private ServerAvailabilityChecker() {
    }

    public static CompletableFuture<Boolean> isReachable(SocketAddress address) {
        long now = System.nanoTime();
        return CACHE.compute(address, (key, cached) -> {
            if (cached == null || cached.isExpired(now)) {
                return new CacheEntry(probe(key));
            }
            return cached;
        }).result;
    }

    private static CompletableFuture<Boolean> probe(SocketAddress address) {
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
                .completeOnTimeout(false, CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
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
        private volatile long expiresAtNanos;

        private CacheEntry(CompletableFuture<Boolean> probe) {
            probe.whenComplete((reachable, throwable) -> {
                expiresAtNanos = System.nanoTime() + CACHE_TTL_NANOS;
                if (throwable == null) {
                    result.complete(reachable);
                } else {
                    result.completeExceptionally(throwable);
                }
            });
        }

        private boolean isExpired(long now) {
            return result.isDone() && now - expiresAtNanos >= 0;
        }
    }
}
