package pe.poo.game;

import java.util.Random;

public final class Arena {
    public static final int WIDTH = 15;
    public static final int HEIGHT = 11;
    public static final int EXIT_X = 13;
    public static final int EXIT_Y = 9;

    public enum Tile { FLOOR, WALL, CRATE }

    private final Tile[][] tiles = new Tile[HEIGHT][WIDTH];

    public Arena(long seed, double crates) {
        Random random = new Random(seed);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                boolean wall = x == 0 || y == 0 || x == WIDTH - 1 || y == HEIGHT - 1
                    || (x % 2 == 0 && y % 2 == 0);
                boolean reserved = x == 1 || y == 1 || x == EXIT_X || y == EXIT_Y;
                tiles[y][x] = wall ? Tile.WALL
                    : !reserved && random.nextDouble() < crates ? Tile.CRATE : Tile.FLOOR;
            }
        }
    }

    public Tile at(int x, int y) {
        if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) {
            return Tile.WALL;
        }
        return tiles[y][x];
    }

    public void clearCrate(int x, int y) {
        if (at(x, y) == Tile.CRATE) {
            tiles[y][x] = Tile.FLOOR;
        }
    }
}
