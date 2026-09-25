package net.voidflame.core.storage;

import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SqliteDatabaseService implements DatabaseService {
    private final Path file;
    private final SQLiteDataSource dataSource;
    private final ExecutorService executor;
    private volatile boolean open;

    public SqliteDatabaseService(Path dataFolder) {
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create VoidFlame database directory.", e);
        }
        this.file = dataFolder.resolve("database.db");
        this.dataSource = new SQLiteDataSource();
        this.dataSource.setUrl("jdbc:sqlite:" + file.toAbsolutePath());
        this.executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "VoidFlame-Core-Database");
            thread.setDaemon(true);
            return thread;
        });
        initialize();
        open = true;
    }

    private void initialize() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=5000");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                        version INTEGER PRIMARY KEY,
                        applied_at TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS server_meta (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_data (
                        uuid TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        first_seen TEXT NOT NULL,
                        last_seen TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS module_data (
                        module TEXT NOT NULL,
                        data_key TEXT NOT NULL,
                        data_value TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        PRIMARY KEY (module, data_key)
                    )
                    """);
            statement.execute("""
                    INSERT OR IGNORE INTO schema_migrations(version, applied_at)
                    VALUES (1, datetime('now'))
                    """);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize VoidFlame database.", e);
        }
    }

    @Override
    public CompletableFuture<Void> execute(String sql, Object... parameters) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = prepare(connection, sql, parameters)) {
                statement.execute();
            } catch (SQLException e) {
                throw new IllegalStateException("Database execution failed: " + sql, e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> update(String sql, Object... parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = prepare(connection, sql, parameters)) {
                return statement.executeUpdate();
            } catch (SQLException e) {
                throw new IllegalStateException("Database update failed: " + sql, e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> query(String sql, Object... parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = prepare(connection, sql, parameters);
                 ResultSet result = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData meta = result.getMetaData();
                int columns = meta.getColumnCount();
                while (result.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columns; i++) {
                        row.put(meta.getColumnLabel(i), result.getObject(i));
                    }
                    rows.add(row);
                }
                return rows;
            } catch (SQLException e) {
                throw new IllegalStateException("Database query failed: " + sql, e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> transaction(List<Statement> statements) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    for (Statement item : statements) {
                        try (PreparedStatement statement = prepare(connection, item.sql(), item.parameters())) {
                            statement.executeUpdate();
                        }
                    }
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Database transaction failed.", e);
            }
        }, executor);
    }

    private static PreparedStatement prepare(Connection connection, String sql, Object... parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
        return statement;
    }

    @Override public boolean isOpen() { return open; }
    @Override public String databasePath() { return file.toAbsolutePath().toString(); }

    @Override
    public void close() {
        if (!open) return;
        open = false;
        executor.shutdown();
    }
}
