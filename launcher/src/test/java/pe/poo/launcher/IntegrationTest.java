package pe.poo.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.poo.adaptive.DifficultyConfig;
import pe.poo.adaptive.LevelSelector;
import pe.poo.game.LevelRequest;
import pe.poo.persistence.ProfileStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    @TempDir Path directory;

    @Test
    void profileCrudAndHistoryPersistAcrossConnections() throws Exception {
        Path database = directory.resolve("test.db");
        long id;
        try (var store = new ProfileStore(database)) {
            store.create("Lucía");
            id = store.profiles().getFirst().id();
            store.rename(id, "Luci");
            store.save(id, 3, 8, 60);
        }
        try (var store = new ProfileStore(database)) {
            assertEquals("Luci", store.profiles().getFirst().alias());
            var score = store.history(id).getFirst();
            assertEquals(3, score.level());
            assertEquals(8, score.enemies());
            assertEquals(60, score.seconds());
            store.delete(id);
            assertTrue(store.profiles().isEmpty());
            assertTrue(store.history(id).isEmpty());
            assertThrows(SQLException.class, () -> store.save(id, 1, 0, 0));
        }
    }

    @Test
    void configOverridesRulesAndRejectsInvalidThresholds() throws IOException {
        Path json = directory.resolve("difficulty.json");
        assertEquals(new DifficultyConfig(2, 45), DifficultyConfig.load(json));
        Files.writeString(json, """
            {"recoveryDamage":1,"challengeSeconds":10}
            """);
        var selector = new LevelSelector(DifficultyConfig.load(json));
        assertEquals("recuperación", selector.generate(new LevelRequest(2, 0, 1, 3)).id());
        assertEquals("desafío", selector.generate(new LevelRequest(2, 0, 0, 9)).id());
        assertEquals("equilibrado", selector.generate(new LevelRequest(2, 0, 0, 10)).id());
        Files.writeString(json, """
            {"recoveryDamage":0,"challengeSeconds":10}
            """);
        assertThrows(IOException.class, () -> DifficultyConfig.load(json));
    }
}
