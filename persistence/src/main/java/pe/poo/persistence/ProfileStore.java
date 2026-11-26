package pe.poo.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Stores profiles and run results in a local SQLite database through JDBC. */
public final class ProfileStore implements AutoCloseable {
    public record Profile(long id, String alias) {}
    public record Score(int level, int enemies, long seconds, String date) {}
    private final Connection connection;

    public ProfileStore(Path database) throws SQLException, IOException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
        try (var statement = connection.createStatement();
             var schema = ProfileStore.class.getResourceAsStream("/schema.sql")) {
            if (schema == null) throw new IOException("Falta schema.sql");
            statement.execute("PRAGMA foreign_keys = ON");
            for (String sql : new String(schema.readAllBytes(), StandardCharsets.UTF_8).split(";")) {
                if (!sql.isBlank()) statement.execute(sql);
            }
        } catch (SQLException | IOException failure) {
            try {
                connection.close();
            } catch (SQLException closing) {
                failure.addSuppressed(closing);
            }
            throw failure;
        }
    }

    public void create(String alias) throws SQLException {
        try (var statement = connection.prepareStatement("INSERT INTO profiles(alias) VALUES (?)")) {
            statement.setString(1, alias(alias));
            statement.executeUpdate();
        }
    }

    public List<Profile> profiles() throws SQLException {
        try (var statement = connection.prepareStatement("SELECT id, alias FROM profiles ORDER BY alias");
             var rows = statement.executeQuery()) {
            List<Profile> profiles = new ArrayList<>();
            while (rows.next()) profiles.add(new Profile(rows.getLong(1), rows.getString(2)));
            return List.copyOf(profiles);
        }
    }

    public void rename(long id, String alias) throws SQLException {
        try (var statement = connection.prepareStatement("UPDATE profiles SET alias = ? WHERE id = ?")) {
            statement.setString(1, alias(alias));
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (var statement = connection.prepareStatement("DELETE FROM profiles WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    public void save(long id, int level, int enemies, long seconds) throws SQLException {
        try (var statement = connection.prepareStatement(
                "INSERT INTO runs(profile_id, level, enemies, seconds) VALUES (?, ?, ?, ?)")) {
            statement.setLong(1, id);
            statement.setInt(2, level);
            statement.setInt(3, enemies);
            statement.setLong(4, seconds);
            statement.executeUpdate();
        }
    }

    public List<Score> history(long id) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT level, enemies, seconds, recorded_at FROM runs WHERE profile_id = ? ORDER BY id DESC")) {
            statement.setLong(1, id);
            try (var rows = statement.executeQuery()) {
                List<Score> scores = new ArrayList<>();
                while (rows.next()) {
                    scores.add(new Score(rows.getInt(1), rows.getInt(2), rows.getLong(3), rows.getString(4)));
                }
                return List.copyOf(scores);
            }
        }
    }

    private String alias(String input) {
        String alias = input.strip();
        if (alias.isEmpty() || alias.length() > 32) {
            throw new IllegalArgumentException("El alias debe tener entre 1 y 32 caracteres");
        }
        return alias;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
