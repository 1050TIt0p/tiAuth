package ru.matveylegenda.tiauth.premium;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class PremiumVerifier {
    private final URI profileEndpoint;
    private final AsyncLoadingCache<String, Optional<UUID>> profileCache;

    public PremiumVerifier(String apiUrl) {
        this(apiUrl, new HttpProfileLookup(URI.create(apiUrl)));
    }

    PremiumVerifier(String apiUrl, ProfileLookup profileLookup) {
        this.profileEndpoint = URI.create(apiUrl);
        profileCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .buildAsync((username, executor) -> profileLookup.findUuid(username));
    }

    public CompletableFuture<Boolean> isPremium(String username, UUID claimedUuid) {
        if (claimedUuid == null) {
            return CompletableFuture.completedFuture(false);
        }

        return findUuid(username)
                .thenApply(profileUuid -> profileUuid.map(claimedUuid::equals).orElse(false));
    }

    public CompletableFuture<Optional<UUID>> findUuid(String username) {
        return profileCache.get(username.toLowerCase(Locale.ROOT));
    }

    @FunctionalInterface
    interface ProfileLookup {
        CompletableFuture<Optional<UUID>> findUuid(String username);
    }

    private static final class HttpProfileLookup implements ProfileLookup {
        private final Gson gson = new Gson();
        private final URI profileEndpoint;
        private final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpProfileLookup(URI profileEndpoint) {
            this.profileEndpoint = profileEndpoint;
        }

        @Override
        public CompletableFuture<Optional<UUID>> findUuid(String username) {
            HttpRequest request = HttpRequest.newBuilder(profileEndpoint.resolve(username))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> parseResponse(response.statusCode(), response.body()));
        }

        private Optional<UUID> parseResponse(int statusCode, String body) {
            if (statusCode == 204 || statusCode == 404) {
                return Optional.empty();
            }
            if (statusCode != 200) {
                throw new IllegalStateException("Profile lookup returned HTTP " + statusCode);
            }

            try {
                ProfileResponse profile = gson.fromJson(body, ProfileResponse.class);
                if (profile == null || profile.id == null || profile.id.length() != 32) {
                    throw new IllegalStateException("Profile lookup returned an invalid profile");
                }

                return Optional.of(UUID.fromString(profile.id.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                        "$1-$2-$3-$4-$5"
                )));
            } catch (JsonParseException | IllegalArgumentException exception) {
                throw new IllegalStateException("Profile lookup returned invalid JSON", exception);
            }
        }

        @SuppressWarnings("unused")
        private static final class ProfileResponse {
            private String id;
        }
    }
}
