package ru.matveylegenda.tiauth.database.repository;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PremiumIdentityRepositoryTest {
    private JdbcConnectionSource connectionSource;
    private ExecutorService executor;
    private PremiumIdentityRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        connectionSource = new JdbcConnectionSource("jdbc:h2:mem:" + UUID.randomUUID());
        executor = Executors.newSingleThreadExecutor();
        repository = new PremiumIdentityRepository(connectionSource, executor);
    }

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdownNow();
        connectionSource.close();
    }

    @Test
    void bindsNameCaseInsensitivelyAndRefusesAnotherUuid() {
        UUID boundUuid = UUID.randomUUID();

        repository.bind("PremiumPlayer", boundUuid).join();

        assertEquals(boundUuid, repository.getUuid("premiumplayer").join());
        repository.bind("PREMIUMPLAYER", boundUuid).join();
        assertThrows(
                CompletionException.class,
                () -> repository.bind("PremiumPlayer", UUID.randomUUID()).join()
        );
        assertEquals(boundUuid, repository.getUuid("PremiumPlayer").join());
    }
}
