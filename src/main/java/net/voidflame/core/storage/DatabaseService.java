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
    /**
     * Executes a group of statements atomically and returns each statement's affected-row count.
     * This is useful for optimistic concurrency and inventory/economy escrow workflows.
     */
    default CompletableFuture<List<Integer>> transactionWithCounts(List<Statement> statements) {
        return transaction(statements).thenApply(ignored -> List.of());
    }
    /**
     * Executes statements atomically and rolls back if any expected affected-row count differs.
     * Use -1 for a statement whose affected-row count is not constrained.
     */
    default CompletableFuture<List<Integer>> transactionExpectingCounts(List<Statement> statements, List<Integer> expectedRows) {
        return transactionWithCounts(statements).thenApply(counts -> {
            if (expectedRows != null) {
                for (int index = 0; index < expectedRows.size(); index++) {
                    int expected = expectedRows.get(index);
                    if (expected >= 0 && (index >= counts.size() || counts.get(index) != expected)) {
                        throw new IllegalStateException("Transaction row-count expectation failed at statement " + index + ".");
                    }
                }
            }
            return counts;
        });
    }
    CompletableFuture<Path> backup(Path directory);
    CompletableFuture<Void> restore(Path backupFile);
    boolean isOpen();
    String databasePath();
    record Statement(String sql, Object... parameters) {}
    @Override void close();
}
