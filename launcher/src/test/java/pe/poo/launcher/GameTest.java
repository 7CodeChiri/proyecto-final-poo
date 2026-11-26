package pe.poo.launcher;

import org.junit.jupiter.api.Test;
import pe.poo.adaptive.LevelSelector;
import pe.poo.game.Arena;
import pe.poo.game.Enemy.Cell;
import pe.poo.game.LevelRequest;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    @Test
    void choosesAllThreeGeneratorsAndPrioritizesRecovery() {
        var selector = new LevelSelector();
        assertEquals("equilibrado", selector.generate(new LevelRequest(1, 42, 0, 0)).id());
        assertEquals("equilibrado", selector.generate(new LevelRequest(2, 42, 1, 45)).id());
        assertEquals("desafío", selector.generate(new LevelRequest(2, 42, 1, 44)).id());
        assertEquals("recuperación", selector.generate(new LevelRequest(2, 42, 2, 10)).id());
    }

    @Test
    void keepsACorridorToTheExitAcrossSeeds() {
        for (int seed = 0; seed < 50; seed++) {
            var arena = new Arena(seed, 0.5);
            for (int x = 1; x <= Arena.EXIT_X; x++) {
                assertEquals(Arena.Tile.FLOOR, arena.at(x, 1));
            }
            for (int y = 1; y <= Arena.EXIT_Y; y++) {
                assertEquals(Arena.Tile.FLOOR, arena.at(Arena.EXIT_X, y));
            }
        }
    }

    @Test
    void finishesLevelOffersThreeCardsAndAppliesOne() {
        var run = new Run();
        run.enemies.clear();
        run.player = run.exit;
        run.tick(25);
        assertEquals(Run.Phase.CARDS, run.phase);
        assertEquals(3, run.offers.stream().distinct().count());
        var selected = run.offers.getFirst();
        run.choose(selected);
        assertEquals(2, run.level);
        assertEquals(Run.Phase.PLAY, run.phase);
        run.choose(selected);
        assertEquals(2, run.level);
    }

    @Test
    void bombDamagesPlayerOnlyOnceDuringExposure() {
        var run = new Run();
        run.enemies.clear();
        run.placeBomb();
        run.tick(1700);
        assertTrue(run.bombs.isEmpty());
        assertTrue(run.flames.containsKey(new Cell(1, 1)));
        assertEquals(3, run.health);
        run.tick(100);
        assertEquals(3, run.health);
    }
}
