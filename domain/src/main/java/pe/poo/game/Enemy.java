package pe.poo.game;

import java.util.List;
import java.util.Random;

public abstract class Enemy {
    protected static final List<Cell> DIRECTIONS = List.of(
        new Cell(0, -1), new Cell(1, 0), new Cell(0, 1), new Cell(-1, 0));

    public record Cell(int x, int y) {
        public Cell plus(Cell other) { return new Cell(x + other.x, y + other.y); }
        public int distance(Cell other) { return Math.abs(x - other.x) + Math.abs(y - other.y); }
    }

    private Cell cell;

    protected Enemy(Cell cell) { this.cell = cell; }
    public Cell cell() { return cell; }

    public void move(List<Cell> options, Cell player, Random random) {
        if (!options.isEmpty()) {
            cell = choose(options, player, random);
        }
    }

    protected abstract Cell choose(List<Cell> options, Cell player, Random random);

    public static final class Patrol extends Enemy {
        public Patrol(Cell cell) { super(cell); }

        protected Cell choose(List<Cell> options, Cell player, Random random) {
            return options.get(random.nextInt(options.size()));
        }
    }

    public static final class Chaser extends Enemy {
        public Chaser(Cell cell) { super(cell); }

        protected Cell choose(List<Cell> options, Cell player, Random random) {
            return options.stream().min(java.util.Comparator.comparingInt(player::distance))
                .orElseThrow();
        }
    }
}
