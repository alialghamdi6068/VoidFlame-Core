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
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class SqliteDatabaseService implements DatabaseService {
    private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");

    private final Path file;
    private final SQLiteDataSource dataSource;
    private final ExecutorService executor;
    private volatile boolean open;
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock(true);

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
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("PRAGMA integrity_check")) {
                if (!result.next() || !"ok".equalsIgnoreCase(result.getString(1))) {
                    throw new IllegalStateException("VoidFlame-Core database integrity check failed.");
                }
            }
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
            try { connection.close(); } catch (SQLException ignored) { }
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
            lifecycleLock.readLock().lock();
            try (Connection connection = openConnection();
                 PreparedStatement statement = prepare(connection, sql, parameters)) {
                statement.execute();
            } catch (SQLException e) {
                throw new IllegalStateException("Database execution failed.", e);
            } finally {
                lifecycleLock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> update(String sql, Object... parameters) {
        ensureOpen();
        return CompletableFuture.supplyAsync(() -> {
            lifecycleLock.readLock().lock();
            try (Connection connection = openConnection();
                 PreparedStatement statement = prepare(connection, sql, parameters)) {
                return statement.executeUpdate();
            } catch (SQLException e) {
                throw new IllegalStateException("Database update failed.", e);
            } finally {
                lifecycleLock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> query(String sql, Object... parameters) {
        ensureOpen();
        return CompletableFuture.supplyAsync(() -> {
            lifecycleLock.readLock().lock();
            try (Connection connection = openConnection();
                 PreparedStatement statement = prepare(connection, sql, parameters);
                 ResultSet result = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData meta = result.getMetaData();
                int columns = meta.getColumnCount();
                while (result.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columns; i++) row.put(meta.getColumnLabel(i), result.getObject(i));
                    rows.add(row);
                }
                return rows;
            } catch (SQLException e) {
                throw new IllegalStateException("Database query failed.", e);
            } finally {
                lifecycleLock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> transaction(List<Statement> statements) {
        return transactionWithCounts(statements).thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<List<Integer>> transactionWithCounts(List<Statement> statements) {
        ensureOpen();
        if (statements == null || statements.isEmpty()) return CompletableFuture.completedFuture(List.of());

        return CompletableFuture.supplyAsync(() -> {
            lifecycleLock.readLock().lock();
            try (Connection connection = openConnection()) {
                connection.setAutoCommit(false);
                try {
                    List<Integer> affectedRows = new ArrayList<>(statements.size());
                    for (Statement item : statements) {
                        try (PreparedStatement statement = prepare(connection, item.sql(), item.parameters())) {
                            affectedRows.add(statement.executeUpdate());
                        }
                    }
                    connection.commit();
                    return List.copyOf(affectedRows);
                } catch (SQLException e) {
                    try { connection.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Database transaction failed.", e);
            } finally {
                lifecycleLock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Path> backup(Path directory) {
        ensureOpen();
        return CompletableFuture.supplyAsync(() -> {
            lifecycleLock.readLock().lock();
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
                try (Connection connection = openConnection(); var statement = connection.createStatement()) {
                    statement.execute("VACUUM INTO '" + escaped + "'");
                }
                return uniqueTarget;
            } catch (SQLException | IOException e) {
                throw new IllegalStateException("Unable to create database backup.", e);
            } finally {
                lifecycleLock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> restore(Path backupFile) {
        ensureOpen();
        return CompletableFuture.runAsync(() -> {
            lifecycleLock.writeLock().lock();
            try {
                if (backupFile == null || !Files.isRegularFile(backupFile)) {
                    throw new IllegalArgumentException("Backup file does not exist: " + backupFile);
                }
                Path source = backupFile.toAbsolutePath().normalize();
                if (source.equals(file.toAbsolutePath().normalize())) {
                    throw new IllegalArgumentException("Cannot restore the live database file onto itself.");
                }

                // Verify the backup in read-only mode. Never run migrations against a backup,
                // because validation must not alter the only recoverable copy of user data.
                try (Connection verification = java.sql.DriverManager.getConnection("jdbc:sqlite:" + source);
                     var guard = verification.createStatement()) {
                    guard.execute("PRAGMA query_only=ON");
                    try (var integrity = guard.executeQuery("PRAGMA integrity_check")) {
                        if (!integrity.next() || !"ok".equalsIgnoreCase(integrity.getString(1))) {
                            throw new IllegalStateException("Backup integrity check failed.");
                        }
                    }
                    try (var schema = guard.executeQuery(
                            "SELECT 1 FROM sqlite_master WHERE type='table' AND name='schema_migrations'")) {
                        if (!schema.next()) throw new IllegalStateException("Backup is not a recognized VoidFlame-Core database.");
                    }
                }

                // Always snapshot the live database before replacing it.
                Path safetyDir = file.getParent().resolve("backups");
                Files.createDirectories(safetyDir);
                String safetyName = "database-pre-restore-" + System.currentTimeMillis() + ".db";
                Path safety = safetyDir.resolve(safetyName);
                String escapedSafety = safety.toAbsolutePath().toString().replace("'", "''");
                try (Connection current = openConnection(); var snapshot = current.createStatement()) {
                    snapshot.execute("VACUUM INTO '" + escapedSafety + "'");
                }

                Path temp = file.resolveSibling(file.getFileName() + ".restore.tmp");
                Files.copy(source, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                try {
                    Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                            java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                    Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                Files.deleteIfExists(file.resolveSibling(file.getFileName() + "-wal"));
                Files.deleteIfExists(file.resolveSibling(file.getFileName() + "-shm"));
            } catch (SQLException | IOException e) {
                throw new IllegalStateException("Unable to restore database backup.", e);
            } finally {
                lifecycleLock.writeLock().unlock();
            }
        }, executor);
    }

    private static PreparedStatement prepare(Connection connection, String sql, Object... parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < parameters.length; i++) statement.setObject(i + 1, parameters[i]);
        return statement;
    }

    @Override
    public boolean isOpen() { return open; }

    @Override
    public String databasePath() { return file.toAbsolutePath().toString(); }

    @Override
    public void close() {
        if (!open) return;
        lifecycleLock.writeLock().lock();
        try {
            if (!open) return;
            executor.shutdown();
        } finally {
            lifecycleLock.writeLock().unlock();
        }
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                executor.awaitTermination(2, TimeUnit.SECONDS);
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            open = false;
        }
    }
}
