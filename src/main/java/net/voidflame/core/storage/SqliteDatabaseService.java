package net.voidflame.core.storage;

import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class SqliteDatabaseService implements DatabaseService {
    private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");

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
        this.executor = Executors.newFixedThreadPool(
                Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors())),
                runnable -> {
                    Thread thread = new Thread(runnable, "VoidFlame-Core-Database");
                    thread.setDaemon(true);
                    return thread;
                }
        );

        initialize();
        open = true;
    }

    private void initialize() {
        try (Connection connection = openConnection()) {
            DatabaseMigrations.apply(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize VoidFlame database.", e);
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = dataSource.getConnection();
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=5000");
            statement.execute("PRAGMA synchronous=NORMAL");
        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }
        return connection;
    }

    private void ensureOpen() {
        if (!open) throw new IllegalStateException("VoidFlame-Core database is closed.");
    }

    @Override
    public CompletableFuture<Void> execute(String sql, Object... parameters) {
        ensureOpen();
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = openConnection();
                 PreparedStatement statement = prepare(connection, sql, parameters)) {
                statement.execute();
            } catch (SQLException e) {
                throw new IllegalStateException("Database execution failed.", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> update(String sql, Object... parameters) {
        ensureOpen();
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = openConnection();
                 PreparedStatement statement = prepare(connection, sql, parameters)) {
                return statement.executeUpdate();
            } catch (SQLException e) {
                throw new IllegalStateException("Database update failed.", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> query(String sql, Object... parameters) {
        ensureOpen();
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = openConnection();
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
                throw new IllegalStateException("Database query failed.", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> transaction(List<Statement> statements) {
        ensureOpen();
        if (statements == null || statements.isEmpty()) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            try (Connection connection = openConnection()) {
                connection.setAutoCommit(false);
                try {
                    for (Statement item : statements) {
                        try (PreparedStatement statement = prepare(connection, item.sql(), item.parameters())) {
                            statement.executeUpdate();
                        }
                    }
                    connection.commit();
                } catch (SQLException e) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackError) {
                        e.addSuppressed(rollbackError);
                    }
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Database transaction failed.", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Path> backup(Path directory) {
        ensureOpen();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(directory);
                String timestamp = LocalDateTime.now().format(BACKUP_FORMAT);
                Path target = directory.resolve("database-" + timestamp + ".db");
                Path uniqueTarget = target;
                int suffix = 1;
                while (Files.exists(uniqueTarget)) {
                    uniqueTarget = directory.resolve("database-" + timestamp + "-" + suffix++ + ".db");
                }

                String escaped = uniqueTarget.toAbsolutePath().toString().replace("'", "''");
                try (Connection connection = openConnection();
                     var statement = connection.createStatement()) {
                    statement.execute("VACUUM INTO '" + escaped + "'");
                }
                return uniqueTarget;
            } catch (SQLException | IOException e) {
                throw new IllegalStateException("Unable to create database backup.", e);
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

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public String databasePath() {
        return file.toAbsolutePath().toString();
    }

    @Override
    public void close() {
        if (!open) return;
        open = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                executor.awaitTermination(2, TimeUnit.SECONDS);
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
