package net.voidflame.core.storage;

import net.voidflame.core.api.AuditLogService;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class SqliteAuditLogService implements AuditLogService {
    private final DatabaseService database;

    public SqliteAuditLogService(DatabaseService database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public CompletableFuture<Void> log(String actor, String action, String target, String metadata) {
        String safeActor = actor == null ? "SYSTEM" : actor;
        String safeAction = action == null || action.isBlank() ? "UNKNOWN" : action;
        String safeTarget = target == null ? "" : target;
        String safeMetadata = metadata == null ? "" : metadata;
        String metadataJson = "{\"detail\":\"" + jsonEscape(safeMetadata) + "\"}";
        return database.execute(
                "INSERT INTO audit_logs(actor, action, target, timestamp, metadata_json) VALUES (?, ?, ?, ?, ?)",
                safeActor, safeAction, safeTarget, Instant.now().toEpochMilli(), metadataJson
        );
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
