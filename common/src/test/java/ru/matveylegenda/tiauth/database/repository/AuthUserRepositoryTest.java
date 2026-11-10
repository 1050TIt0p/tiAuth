package ru.matveylegenda.tiauth.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import ru.matveylegenda.tiauth.database.model.AuthUser;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AuthUserRepositoryTest {
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private JdbcConnectionPool pool;
    private DataSourceConnectionSource connection;
    private AuthUserRepository repository;
    private Dao<AuthUser, String> observer;

    @BeforeEach
    void setUp() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + "";
        pool = JdbcConnectionPool.create(url, "sa", "");
        connection = new DataSourceConnectionSource(pool, url);
        repository = new AuthUserRepository(connection, executor);
        observer = DaoManager.createDao(connection, AuthUser.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
            }
        } finally {
            connection.close();
            pool.dispose();
        }
    }

    static Stream<Update[]> independentUpdates() {
        Update[] updates = Update.values();
        return IntStream.range(0, updates.length).boxed()
                .flatMap(i -> IntStream.range(i + 1, updates.length)
                        .mapToObj(j -> new Update[]{updates[i], updates[j]}));
    }

    @ParameterizedTest(name = "{0} and {1} preserve both changes")
    @MethodSource("independentUpdates")
    void concurrentUpdatesPreserveOtherFields(Update first, Update second) throws Exception {
        AuthUser initial = new AuthUser("alice", "Alice", "old-password", false, "192.0.2.1");
        initial.setLastLogin(1);
        repository.registerUser(initial).get(10, TimeUnit.SECONDS);

        synchronizeReads();
        CompletableFuture<Void> firstResult = first.apply(repository, "ALICE");
        CompletableFuture<Void> secondResult = second.apply(repository, "Alice");
        awaitUpdates();
        firstResult.get(10, TimeUnit.SECONDS);
        secondResult.get(10, TimeUnit.SECONDS);

        AuthUser actual = observer.queryForId("alice");
        for (Update update : Update.values()) {
            boolean changed = update == first || update == second;
            switch (update) {
                case PASSWORD -> assertEquals(changed ? "new-password" : "old-password", actual.getPassword());
                case LAST_LOGIN -> {
                    if (changed) assertTrue(actual.getLastLogin() > 1);
                    else assertEquals(1, actual.getLastLogin());
                }
                case LAST_IP -> assertEquals(changed ? "192.0.2.2" : "192.0.2.1", actual.getLastIp());
                case TOTP -> assertEquals(changed ? "encrypted-token" : "", actual.getTotpToken());
                case PREMIUM -> assertEquals(changed, actual.isPremium());
            }
        }
        assertEquals("alice", actual.getUsername());
        assertEquals("Alice", actual.getRealName());
        assertEquals(initial.getRegIp(), actual.getRegIp());
        assertEquals(initial.getRegDate(), actual.getRegDate());
    }

    @ParameterizedTest
    @EnumSource(Update.class)
    void updatingMissingAccountDoesNotCreateIt(Update update) throws Exception {
        CompletableFuture<Void> result = update.apply(repository, "Missing");
        awaitUpdates();
        result.get(10, TimeUnit.SECONDS);
        assertTrue(observer.queryForAll().isEmpty());
    }

    private void awaitUpdates() throws InterruptedException {
        // Also waits for the fire-and-forget login/IP updates.
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Both updates must finish");
    }

    @SuppressWarnings("unchecked")
    private void synchronizeReads() throws Exception {
        Field field = AuthUserRepository.class.getDeclaredField("authUserDao");
        field.setAccessible(true);
        Dao<AuthUser, String> delegate = (Dao<AuthUser, String>) field.get(repository);
        CyclicBarrier barrier = new CyclicBarrier(2);
        Dao<AuthUser, String> synchronizedDao = (Dao<AuthUser, String>) Proxy.newProxyInstance(
                Dao.class.getClassLoader(), new Class<?>[]{Dao.class}, (proxy, method, args) -> {
                    Object result;
                    try {
                        result = method.invoke(delegate, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                    // Force both snapshots to be read before either can be saved.
                    // Direct column updates do not need a preceding read.
                    if (method.getName().equals("queryForId")) barrier.await(5, TimeUnit.SECONDS);
                    return result;
                });
        field.set(repository, synchronizedDao);
    }

    enum Update {
        PASSWORD, LAST_LOGIN, LAST_IP, TOTP, PREMIUM;

        CompletableFuture<Void> apply(AuthUserRepository repository, String username) {
            return switch (this) {
                case PASSWORD -> repository.updatePassword(username, "new-password");
                case LAST_LOGIN -> {
                    repository.updateLastLogin(username);
                    yield CompletableFuture.completedFuture(null);
                }
                case LAST_IP -> {
                    repository.updateLastIp(username, "192.0.2.2");
                    yield CompletableFuture.completedFuture(null);
                }
                case TOTP -> repository.updateTotpToken(username, "encrypted-token");
                case PREMIUM -> repository.setPremium(username, true);
            };
        }
    }
}
