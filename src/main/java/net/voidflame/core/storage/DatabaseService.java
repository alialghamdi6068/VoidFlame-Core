package net.voidflame.core.storage;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface DatabaseService extends AutoCloseable {
    CompletableFuture<Void> execute(String sql, Object... parameters);
    CompletableFuture<List<Map<String, Object>>> query(String sql, Object... parameters);
    CompletableFuture<Integer> update(String sql, Object... parameters);
    CompletableFuture<Void> transaction(List<Statement> statements);
    CompletableFuture<Path> backup(Path directory);
    boolean isOpen();
    String databasePath();
    record Statement(String sql, Object... parameters) {}
    @Override void close();
}
