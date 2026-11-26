package pe.poo.launcher;

import pe.poo.game.Arena;
import pe.poo.adaptive.DifficultyConfig;
import pe.poo.adaptive.LevelSelector;
import pe.poo.persistence.ProfileStore;
import java.nio.file.Path;
import java.sql.SQLException;
import pe.poo.game.Enemy;
import pe.poo.game.Enemy.Cell;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

public final class GameWindow {
    private static final Color BACKGROUND = new Color(17, 22, 29);
    private static final Color MINT = new Color(122, 237, 196);
    private static final Color WHITE = new Color(238, 243, 246);
    private static final int STEP = 25;
    private final Run run;
    private final ProfileStore store;
    private ProfilesPanel profiles;
    private ProfileStore.Profile activeProfile;
    private final JFrame frame = new JFrame("Untitled Bomberman Game");
    private final JPanel screen = new JPanel(new BorderLayout());
    private final JPanel rewards = new JPanel(new GridBagLayout());
    private final Board board = new Board();
    private final Map<Integer, Cell> held = new LinkedHashMap<>();
    private final Map<Enemy, Motion> enemyMotion = new IdentityHashMap<>();
    private Motion playerMotion;
    private long lastFrame;
    private long simulationDebt;
    private long animationTime;
    private final Timer timer = new Timer(16, event -> update());
    private boolean bombHeld;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                var config = DifficultyConfig.load(Path.of(System.getenv()
                    .getOrDefault("BOMBERMAN_CONFIG", "config/difficulty.json")));
                try {
                    var path = Path.of(System.getenv().getOrDefault("BOMBERMAN_DB", "bomberman.db"));
                    var store = new ProfileStore(path);
                    try {
                        new GameWindow(config, store).open();
                    } catch (SQLException | RuntimeException failure) {
                        try { store.close(); } catch (SQLException closing) { failure.addSuppressed(closing); }
                        throw failure;
                    }
                } catch (SQLException | java.io.IOException failure) {
                    throw new IllegalStateException("No se pudo abrir la base de datos", failure);
                }
            } catch (Exception error) {
                error.printStackTrace();
                JOptionPane.showMessageDialog(null, error.toString(), "Error al iniciar", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private GameWindow(DifficultyConfig config, ProfileStore store) {
        this.run = new Run(new LevelSelector(config));
        this.store = store;
    }

    private void open() throws SQLException {
        screen.setBackground(BACKGROUND);
        profiles = new ProfilesPanel(store, profile -> {
            activeProfile = profile;
            run.restart();
            resetMotion();
            showScreen(board);
        });
        screen.add(profiles, BorderLayout.CENTER);
        rewards.setBackground(BACKGROUND);
        rewards.setOpaque(true);
        frame.setContentPane(screen);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) {
                timer.stop();
                try { store.close(); } catch (SQLException error) { error.printStackTrace(); }
            }
        });
        frame.addWindowFocusListener(new WindowAdapter() {
            public void windowLostFocus(WindowEvent event) {
                held.clear();
                bombHeld = false;
            }
        });
        frame.setMinimumSize(new Dimension(700, 590));
        frame.pack();
        frame.setLocationRelativeTo(null);
        int[] keys = {38, 39, 40, 37, 'W', 'D', 'S', 'A'};
        Cell[] directions = {new Cell(0, -1), new Cell(1, 0), new Cell(0, 1), new Cell(-1, 0)};
        for (int i = 0; i < keys.length; i++) {
            int key = keys[i];
            Cell direction = directions[i % directions.length];
            bind(key, false, () -> held.putIfAbsent(key, direction));
            bind(key, true, () -> held.remove(key));
        }
        bind(32, false, () -> {
            if (!bombHeld && activeProfile != null) run.placeBomb();
            bombHeld = true;
        });
        bind(32, true, () -> bombHeld = false);
        resetMotion();
        frame.setVisible(true);
        timer.start();
    }

    private void bind(int key, boolean release, Runnable action) {
        String name = key + "-" + release;
        screen.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(key, 0, release), name);
        screen.getActionMap().put(name, new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent event) { action.run(); }
        });
    }

    private void update() {
        long now = System.nanoTime();
        long elapsed = lastFrame == 0 ? 0 : Math.min(now - lastFrame, 100_000_000L);
        lastFrame = now;
        if (!frame.isActive() || activeProfile == null || run.phase != Run.Phase.PLAY) return;
        animationTime += elapsed;
        simulationDebt += elapsed;
        while (simulationDebt >= STEP * 1_000_000L && run.phase == Run.Phase.PLAY) {
            simulationDebt -= STEP * 1_000_000L;
            if (!held.isEmpty()) run.move(held.values().stream().reduce((a, b) -> b).orElseThrow());
            run.tick(STEP);
            syncMotion();
        }
        if (run.phase == Run.Phase.PLAY) {
            board.repaint();
        } else {
            showOverlay();
        }
    }

    private void syncMotion() {
        playerMotion.moveTo(run.player, animationTime, run.moveDelay);
        enemyMotion.keySet().retainAll(run.enemies);
        for (Enemy enemy : run.enemies) {
            enemyMotion.computeIfAbsent(enemy, actor -> new Motion(actor.cell()))
                .moveTo(enemy.cell(), animationTime, 155);
        }
    }

    private void resetMotion() {
        playerMotion = new Motion(run.player);
        enemyMotion.clear();
        simulationDebt = 0;
        syncMotion();
    }

    private static final class Motion {
        private Cell target;
        private double startX;
        private double startY;
        private long startTime;
        private long duration = 1;

        Motion(Cell cell) {
            target = cell;
            startX = cell.x();
            startY = cell.y();
        }

        void moveTo(Cell cell, long now, int millis) {
            if (cell.equals(target)) return;
            startX = x(now);
            startY = y(now);
            target = cell;
            startTime = now;
            duration = millis * 1_000_000L;
        }

        double x(long now) {
            return startX + (target.x() - startX) * progress(now);
        }

        double y(long now) {
            return startY + (target.y() - startY) * progress(now);
        }

        private double progress(long now) {
            return Math.max(0, Math.min(1, (now - startTime) / (double) duration));
        }
    }

    private void showOverlay() {
        held.clear();
        rewards.removeAll();
        boolean lost = run.phase == Run.Phase.LOST;
        if (lost) {
            try {
                store.save(activeProfile.id(), run.level, run.defeated, run.totalMillis / 1000);
                profiles.refresh(activeProfile);
            } catch (SQLException error) {
                JOptionPane.showMessageDialog(frame, "Error al guardar: " + error.getMessage(),
                    "Historial", JOptionPane.ERROR_MESSAGE);
            }
        }
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(lost ? "FIN DE LA PARTIDA" : "NIVEL " + run.level + " COMPLETADO");
        title.setForeground(MINT);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(title);
        body.add(Box.createVerticalStrut(14));
        JLabel subtitle = new JLabel(lost ? "Llegaste al nivel " + run.level : "Elige una mejora");
        subtitle.setForeground(WHITE);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(subtitle);
        body.add(Box.createVerticalStrut(28));
        JPanel options = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        options.setOpaque(false);
        if (lost) {
            options.add(button("Nueva partida", "Vuelve al nivel 1", "↻", MINT, run::restart));
            var menu = new SprayCard("Perfiles", "Consultar historial", "≡", new Color(96, 213, 255));
            menu.addActionListener(event -> {
                activeProfile = null;
                showScreen(profiles);
            });
            options.add(menu);
        } else {
                        run.offers.forEach(card -> {
                Color ink = switch (card) {
                    case EXTRA -> new Color(255, 196, 94);
                    case RANGE -> new Color(255, 128, 88);
                    case HEAL -> new Color(248, 126, 157);
                    case SPEED -> new Color(96, 213, 255);
                    case SHIELD -> new Color(194, 154, 255);
                    case HEART -> MINT;
                };
                String mark = switch (card) {
                    case EXTRA -> "+1";
                    case RANGE -> "✦";
                    case HEAL -> "+";
                    case SPEED -> "»";
                    case SHIELD -> "◇";
                    case HEART -> "♥";
                };
                options.add(button(card.title, card.detail, mark, ink, () -> run.choose(card)));
            });
        }
        body.add(options);
        rewards.add(body);
        showScreen(rewards);
    }

    private void showScreen(JPanel panel) {
        screen.removeAll();
        screen.add(panel, BorderLayout.CENTER);
        screen.revalidate();
        screen.repaint();
    }

    private JButton button(String title, String detail, String mark, Color ink, Runnable select) {
        JButton button = new SprayCard(title, detail, mark, ink);
        button.addActionListener(event -> {
            select.run();
            resetMotion();
            showScreen(board);
            board.requestFocusInWindow();
        });
        return button;
    }

    private static final class SprayCard extends JButton {
        private final String detail;
        private final String mark;
        private final Color ink;

        SprayCard(String title, String detail, String mark, Color ink) {
            super(title);
            this.detail = detail;
            this.mark = mark;
            this.ink = ink;
            setPreferredSize(new Dimension(200, 242));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleDescription(detail);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            int center = width / 2;
            boolean highlighted = getModel().isRollover() || hasFocus();
            g.setColor(highlighted ? new Color(39, 45, 55) : new Color(28, 33, 42));
            g.fillRoundRect(2, 2, width - 4, height - 4, 17, 17);

            Random spray = new Random(getText().hashCode());
            for (int i = 0; i < 370; i++) {
                int x = center + (int) (spray.nextGaussian() * 30);
                int y = 79 + (int) (spray.nextGaussian() * 26);
                if (x < 12 || x > width - 12 || y < 12 || y > 133) continue;
                int dot = 1 + spray.nextInt(4);
                g.setColor(new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 25 + spray.nextInt(115)));
                g.fillOval(x, y, dot, dot);
            }
            g.setColor(ink);
            g.setStroke(new BasicStroke(highlighted ? 2.5f : 1.2f));
            g.drawRoundRect(2, 2, width - 5, height - 5, 17, 17);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 66));
            centerText(g, mark, center, 100);
            g.setColor(new Color(28, 33, 42));
            g.fillRect(center - 41, 77, 82, 3);
            g.setColor(ink);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 16));
            centerText(g, getText().toUpperCase(java.util.Locale.ROOT), center, 161);
            g.setColor(WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            centerText(g, detail, center, 185);
            g.dispose();
        }

        private void centerText(Graphics2D g, String text, int center, int baseline) {
            g.drawString(text, center - g.getFontMetrics().stringWidth(text) / 2, baseline);
        }
    }

    private final class Board extends JPanel {
        Board() {
            setPreferredSize(new Dimension(930, 720));
            setBackground(BACKGROUND);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min((getWidth() - 50) / Arena.WIDTH, (getHeight() - 168) / Arena.HEIGHT);
            int left = (getWidth() - size * Arena.WIDTH) / 2;
            int top = 100;
            g.setColor(MINT);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            g.drawString("UNTITLED BOMBERMAN GAME", left, 38);
            g.setColor(WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            g.drawString("Nivel " + run.level + "   •   Vida " + run.health + "/" + run.maxHealth
                + "   •   Bombas " + run.bombs.size() + "/" + run.capacity + "   •   Alcance " + run.range
                + (run.shield ? "   •   Escudo activo" : ""), left, 65);
            g.setColor(new Color(171, 190, 200));
            g.drawString(run.generator + " / " + run.reason, left, 87);
            g.translate(left, top);
            for (int y = 0; y < Arena.HEIGHT; y++) {
                for (int x = 0; x < Arena.WIDTH; x++) {
                    Cell cell = new Cell(x, y);
                    Color color = switch (run.arena.at(x, y)) {
                        case FLOOR -> (x + y) % 2 == 0 ? new Color(31, 43, 53) : new Color(34, 48, 59);
                        case WALL -> new Color(59, 77, 92);
                        case CRATE -> new Color(140, 93, 62);
                    };
                    g.setColor(color);
                    g.fillRoundRect(x * size + 1, y * size + 1, size - 2, size - 2, 8, 8);
                    if (run.flames.containsKey(cell)) {
                        token(g, cell, size, new Color(255, 154, 69), "+");
                    }
                }
            }
            token(g, run.exit, size, run.enemies.isEmpty() ? MINT : new Color(137, 151, 166), "↗");
            run.bombs.keySet().forEach(cell -> token(g, cell, size, new Color(250, 213, 91), "●"));
            for (Enemy enemy : run.enemies) {
                Motion motion = enemyMotion.get(enemy);
                boolean chaser = enemy instanceof Enemy.Chaser;
                token(g, motion.x(animationTime), motion.y(animationTime), size,
                    chaser ? new Color(242, 105, 131) : new Color(190, 145, 248), chaser ? "C" : "P");
            }
            double playerX = playerMotion.x(animationTime);
            double playerY = playerMotion.y(animationTime);
            token(g, playerX, playerY, size, MINT, "T");
            if (run.clock < run.protectedUntil) {
                g.setColor(new Color(255, 189, 91));
                g.setStroke(new BasicStroke(2.5f));
                g.drawRoundRect((int) Math.round(playerX * size) + 3,
                    (int) Math.round(playerY * size) + 3, size - 6, size - 6, 10, 10);
            }
            g.setColor(WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.drawString("WASD o flechas para moverte  •  Espacio para colocar bomba",
                0, Arena.HEIGHT * size + 23);
            g.drawString(run.enemies.isEmpty() ? "Salida abierta" : "Enemigos restantes: " + run.enemies.size(),
                0, Arena.HEIGHT * size + 44);
            g.dispose();
        }

        private void token(Graphics2D g, Cell cell, int size, Color tint, String mark) {
            token(g, cell.x(), cell.y(), size, tint, mark);
        }

        private void token(Graphics2D g, double column, double row, int size, Color tint, String mark) {
            int x = (int) Math.round(column * size);
            int y = (int) Math.round(row * size);
            int pad = Math.max(3, size / 7);
            g.setColor(tint);
            g.fillRoundRect(x + pad, y + pad, size - 2 * pad, size - 2 * pad, pad * 2, pad * 2);
            g.setColor(BACKGROUND);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size / 2));
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(mark, x + (size - metrics.stringWidth(mark)) / 2,
                y + (size - metrics.getHeight()) / 2 + metrics.getAscent());
        }
    }
}
