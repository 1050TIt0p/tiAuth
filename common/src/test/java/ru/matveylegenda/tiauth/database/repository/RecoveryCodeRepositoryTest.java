package ru.matveylegenda.tiauth.database.repository;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class RecoveryCodeRepositoryTest {
    @TempDir Path tempDir;

    @Test
    void recoveryCodeCanOnlyBeConsumedOnce() throws Exception {
        try (JdbcConnectionSource connection = new JdbcConnectionSource("jdbc:sqlite:" + tempDir.resolve("recovery.db"))) {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                RecoveryCodeRepository repository = new RecoveryCodeRepository(connection, executor);
                repository.addCodes(new String[]{"sample-hash"}, "Alice").join();

                var first = repository.consumeCode("sample-hash", "Alice");
                var second = repository.consumeCode("sample-hash", "Alice");
                assertEquals(1, (first.join() ? 1 : 0) + (second.join() ? 1 : 0));
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void recoveryCodeMustBelongToAccount() throws Exception {
        try (JdbcConnectionSource connection = new JdbcConnectionSource("jdbc:sqlite:" + tempDir.resolve("ownership.db"))) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                RecoveryCodeRepository repository = new RecoveryCodeRepository(connection, executor);
                repository.addCodes(new String[]{"another-hash"}, "Alice").join();

                assertFalse(repository.consumeCode("another-hash", "Bob").join());
                assertTrue(repository.consumeCode("another-hash", "ALICE").join());
                assertFalse(repository.consumeCode("another-hash", "Alice").join());
            } finally {
                executor.shutdownNow();
            }
        }
    }
}
