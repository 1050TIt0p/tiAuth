package ru.matveylegenda.tiauth.premium;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PremiumVerifierTest {
    private static final UUID NOTCH_UUID = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    @Test
    void acceptsMatchingUuid() {
        PremiumVerifier verifier = verifierReturning(Optional.of(NOTCH_UUID));

        assertTrue(verifier.isPremium("Notch", NOTCH_UUID).join());
    }

    @Test
    void rejectsDifferentUuid() {
        PremiumVerifier verifier = verifierReturning(Optional.of(NOTCH_UUID));

        assertFalse(verifier.isPremium("Notch", UUID.randomUUID()).join());
    }

    @Test
    void cachesNameLookup() {
        AtomicInteger lookups = new AtomicInteger();
        PremiumVerifier verifier = new PremiumVerifier("http://localhost/", username -> {
            lookups.incrementAndGet();
            return CompletableFuture.completedFuture(Optional.of(NOTCH_UUID));
        });

        verifier.isPremium("Notch", NOTCH_UUID).join();
        verifier.isPremium("notch", NOTCH_UUID).join();

        assertEquals(1, lookups.get());
    }

    @Test
    void doesNotLookupWithoutUuid() {
        AtomicInteger lookups = new AtomicInteger();
        PremiumVerifier verifier = new PremiumVerifier("http://localhost/", username -> {
            lookups.incrementAndGet();
            return CompletableFuture.completedFuture(Optional.empty());
        });

        assertFalse(verifier.isPremium("Notch", null).join());
        assertEquals(0, lookups.get());
    }

    @Test
    void propagatesMojangErrors() {
        PremiumVerifier verifier = new PremiumVerifier("http://localhost/", username ->
                CompletableFuture.failedFuture(new IllegalStateException("Profile lookup unavailable"))
        );

        assertThrows(CompletionException.class, () -> verifier.isPremium("Notch", NOTCH_UUID).join());
    }

    private PremiumVerifier verifierReturning(Optional<UUID> uuid) {
        return new PremiumVerifier("http://localhost/", username -> CompletableFuture.completedFuture(uuid));
    }
}
