package pe.poo.launcher;

import pe.poo.adaptive.LevelSelector;
import pe.poo.game.Arena;
import pe.poo.game.Enemy;
import pe.poo.game.Enemy.Cell;
import pe.poo.game.LevelRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;

/** Owns the state and rules of a single playable run. */
final class Run {
    enum Card {
        EXTRA("Carga extra", "+1 bomba simultánea"),
        RANGE("Onda expansiva", "+1 casilla de alcance"),
        HEAL("Botiquín", "Recupera 1 punto de vida"),
        SPEED("Paso ligero", "Muévete más rápido"),
        SHIELD("Escudo", "Bloquea el siguiente golpe"),
        HEART("Núcleo reforzado", "+1 de vida máxima y actual");

        final String title;
        final String detail;
        Card(String title, String detail) {
            this.title = title;
            this.detail = detail;
        }
    }

    enum Phase { PLAY, CARDS, LOST }

    private static final List<Cell> DIRECTIONS = List.of(
        new Cell(0, -1), new Cell(1, 0), new Cell(0, 1), new Cell(-1, 0));

    final Random random = new Random();
    final List<Enemy> enemies = new ArrayList<>();
    final Map<Cell, Long> bombs = new HashMap<>();
    final Map<Cell, Long> flames = new HashMap<>();
    final Set<Cell> blast = new HashSet<>();
    final Cell exit = new Cell(Arena.EXIT_X, Arena.EXIT_Y);
    final LevelSelector selector;

    Arena arena;
    Cell player;
    Phase phase;
    List<Card> offers;
    String generator;
    String reason;
    int level;
    int health;
    int maxHealth;
    int damage;
    int defeated;
    long totalMillis;
    int capacity;
    int range;
    int moveDelay;
    boolean shield;
    long clock;
    long nextMove;
    long nextEnemy;
    long protectedUntil;
    long elapsedSeconds;

    Run() { this(new LevelSelector()); }

    Run(LevelSelector selector) {
        this.selector = selector;
        restart();
    }

    void restart() {
        health = maxHealth = 4;
        capacity = 1;
        range = 2;
        moveDelay = 150;
        shield = false;
        level = 0;
        defeated = 0;
        totalMillis = 0;
        damage = 0;
        elapsedSeconds = 0;
        nextLevel();
    }

    private void nextLevel() {
        level++;
        long seed = random.nextLong();
        var selected = selector.generate(new LevelRequest(level, seed, damage, elapsedSeconds));
        generator = selected.id();
        reason = selected.reason();
        System.out.printf("Nivel %d | %s | %s | daño=%d | tiempo=%ds | semilla=%d%n",
            level, generator, reason, damage, elapsedSeconds, seed);
        arena = selected.layout().arena();
        player = new Cell(1, 1);
        enemies.clear();
        bombs.clear();
        flames.clear();
        List<Cell> spawns = new ArrayList<>();
        for (int y = 1; y <= Arena.EXIT_Y; y++) {
            for (int x = 1; x <= Arena.EXIT_X; x++) {
                Cell cell = new Cell(x, y);
                boolean onCorridor = x == 1 || y == 1 || x == exit.x() || y == exit.y();
                if (onCorridor && player.distance(cell) >= 8 && !cell.equals(exit)) {
                    spawns.add(cell);
                }
            }
        }
        Collections.shuffle(spawns, random);
        for (int i = 0; i < selected.layout().enemyCount(); i++) {
            Cell cell = spawns.get(i);
            enemies.add(i % 2 == 0 ? new Enemy.Patrol(cell) : new Enemy.Chaser(cell));
        }
        phase = Phase.PLAY;
        offers = List.of();
        damage = 0;
        clock = nextMove = protectedUntil = 0;
        nextEnemy = 430;
    }

    void move(Cell direction) {
        if (phase != Phase.PLAY || clock < nextMove) {
            return;
        }
        Cell target = player.plus(direction);
        if (floor(target) && !bombs.containsKey(target)) {
            player = target;
            nextMove = clock + moveDelay;
        }
    }

    void placeBomb() {
        if (phase == Phase.PLAY && bombs.size() < capacity && !flames.containsKey(player)) {
            bombs.putIfAbsent(player, clock + 1700);
        }
    }

    void tick(int millis) {
        if (phase != Phase.PLAY) {
            return;
        }
        clock += millis;
        totalMillis += millis;
        int enemiesBefore = enemies.size();
        flames.values().removeIf(expiry -> expiry <= clock);
        blast.clear();
        var pending = new java.util.ArrayDeque<>(bombs.entrySet().stream()
            .filter(entry -> entry.getValue() <= clock).map(Map.Entry::getKey).toList());
        while (!pending.isEmpty()) {
            Cell source = pending.removeFirst();
            if (bombs.remove(source) == null) {
                continue;
            }
            blast.add(source);
            for (Cell direction : DIRECTIONS) {
                Cell target = source;
                for (int step = 0; step < range; step++) {
                    target = target.plus(direction);
                    Arena.Tile tile = arena.at(target.x(), target.y());
                    if (tile == Arena.Tile.WALL) {
                        break;
                    }
                    blast.add(target);
                    if (tile == Arena.Tile.CRATE) {
                        arena.clearCrate(target.x(), target.y());
                        break;
                    }
                    if (bombs.containsKey(target)) {
                        pending.add(target);
                        break;
                    }
                }
            }
        }
        blast.forEach(cell -> flames.put(cell, clock + 280));
        enemies.removeIf(enemy -> flames.containsKey(enemy.cell()));
        if (clock >= nextEnemy) {
            nextEnemy = clock + Math.max(220, 475 - Math.min(45, level) * 5);
            for (Enemy enemy : enemies) {
                List<Cell> options = DIRECTIONS.stream().map(enemy.cell()::plus)
                    .filter(this::floor).filter(cell -> !bombs.containsKey(cell))
                    .filter(cell -> enemies.stream().noneMatch(other -> other.cell().equals(cell)))
                    .toList();
                enemy.move(options, player, random);
            }
            enemies.removeIf(enemy -> flames.containsKey(enemy.cell()));
        }
        if (clock >= protectedUntil && (flames.containsKey(player)
                || enemies.stream().anyMatch(enemy -> enemy.cell().equals(player)))) {
            if (shield) {
                shield = false;
            } else {
                health--;
                damage++;
            }
            protectedUntil = clock + 1200;
        }
        defeated += enemiesBefore - enemies.size();
        if (health == 0) {
            phase = Phase.LOST;
        } else if (enemies.isEmpty() && player.equals(exit)) {
            elapsedSeconds = clock / 1000;
            List<Card> available = new ArrayList<>(List.of(Card.values()));
            if (capacity == 5) available.remove(Card.EXTRA);
            if (range == 6) available.remove(Card.RANGE);
            if (moveDelay == 75) available.remove(Card.SPEED);
            if (health == maxHealth) available.remove(Card.HEAL);
            // The shield can refresh, and the maximum-life card remains available.
            Collections.shuffle(available, random);
            offers = List.copyOf(available.subList(0, Math.min(3, available.size())));
            if (offers.size() < 3) {
                var padded = new ArrayList<>(offers);
                padded.add(Card.HEAL);
                offers = List.copyOf(padded);
            }
            phase = Phase.CARDS;
        }
    }

    void choose(Card card) {
        if (phase != Phase.CARDS || !offers.contains(card)) {
            return;
        }
        switch (card) {
            case EXTRA -> capacity++;
            case RANGE -> range++;
            case HEAL -> health = Math.min(health + 1, maxHealth);
            case SPEED -> moveDelay = Math.max(75, moveDelay - 25);
            case SHIELD -> shield = true;
            case HEART -> {
                maxHealth++;
                health++;
            }
        }
        nextLevel();
    }

    private boolean floor(Cell cell) {
        return arena.at(cell.x(), cell.y()) == Arena.Tile.FLOOR;
    }
}
