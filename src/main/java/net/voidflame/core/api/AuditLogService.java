package net.voidflame.core.api;

import java.util.concurrent.CompletableFuture;

public interface AuditLogService {
    CompletableFuture<Void> log(String actor, String action, String target, String metadata);
}
