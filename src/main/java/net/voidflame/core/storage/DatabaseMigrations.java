package net.voidflame.core.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

final class DatabaseMigrations {
    private DatabaseMigrations() {}

    static void apply(Connection connection) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            ensureTable(connection);
        List<Migration> migrations = List.of(
                new Migration(1, """
                    CREATE TABLE IF NOT EXISTS server_meta (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    );
                    CREATE TABLE IF NOT EXISTS player_data (
                        uuid TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        first_seen TEXT NOT NULL,
                        last_seen TEXT NOT NULL
                    );
                    CREATE TABLE IF NOT EXISTS module_data (
                        module TEXT NOT NULL,
                        data_key TEXT NOT NULL,
                        data_value TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        PRIMARY KEY (module, data_key)
                    );
                    """),
                new Migration(2, """
                    CREATE TABLE IF NOT EXISTS player_profiles (
                        uuid TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        first_join INTEGER NOT NULL,
                        last_join INTEGER NOT NULL,
                        coins INTEGER NOT NULL DEFAULT 0,
                        wins INTEGER NOT NULL DEFAULT 0,
                        losses INTEGER NOT NULL DEFAULT 0,
                        winstreak INTEGER NOT NULL DEFAULT 0,
                        best_winstreak INTEGER NOT NULL DEFAULT 0,
                        elo REAL NOT NULL DEFAULT 1000,
                        rank TEXT NOT NULL DEFAULT 'member',
                        preferences_json TEXT NOT NULL DEFAULT '{}',
                        statistics_json TEXT NOT NULL DEFAULT '{}'
                    );
                    CREATE TABLE IF NOT EXISTS server_settings (
                        server_id TEXT PRIMARY KEY,
                        settings_json TEXT NOT NULL DEFAULT '{}',
                        metadata_json TEXT NOT NULL DEFAULT '{}'
                    );
                    CREATE TABLE IF NOT EXISTS duel_matches (
                        match_id TEXT PRIMARY KEY,
                        player_a TEXT NOT NULL,
                        player_b TEXT,
                        kit TEXT NOT NULL,
                        arena TEXT,
                        mode TEXT NOT NULL,
                        winner TEXT,
                        loser TEXT,
                        duration_ms INTEGER NOT NULL DEFAULT 0,
                        result TEXT NOT NULL,
                        elo_change_a REAL NOT NULL DEFAULT 0,
                        elo_change_b REAL NOT NULL DEFAULT 0,
                        timestamp INTEGER NOT NULL
                    );
                    CREATE TABLE IF NOT EXISTS ffa_stats (
                        uuid TEXT PRIMARY KEY,
                        kills INTEGER NOT NULL DEFAULT 0,
                        deaths INTEGER NOT NULL DEFAULT 0,
                        streak INTEGER NOT NULL DEFAULT 0,
                        position INTEGER NOT NULL DEFAULT 0,
                        statistics_json TEXT NOT NULL DEFAULT '{}'
                    );
                    CREATE TABLE IF NOT EXISTS kit_definitions (
                        kit_id TEXT PRIMARY KEY,
                        definition_json TEXT NOT NULL,
                        permissions_json TEXT NOT NULL DEFAULT '[]'
                    );
                    CREATE TABLE IF NOT EXISTS player_kit_layouts (
                        uuid TEXT NOT NULL,
                        kit_id TEXT NOT NULL,
                        layout_json TEXT NOT NULL,
                        PRIMARY KEY(uuid, kit_id)
                    );
                    CREATE TABLE IF NOT EXISTS ranks (
                        rank_id TEXT PRIMARY KEY,
                        parent_rank TEXT,
                        weight INTEGER NOT NULL,
                        prefix TEXT NOT NULL,
                        suffix TEXT NOT NULL,
                        permissions_json TEXT NOT NULL DEFAULT '[]'
                    );
                    CREATE TABLE IF NOT EXISTS player_ranks (
                        uuid TEXT PRIMARY KEY,
                        rank_id TEXT NOT NULL
                    );
                    CREATE TABLE IF NOT EXISTS reports (
                        report_id TEXT PRIMARY KEY,
                        reporter TEXT NOT NULL,
                        target TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        status TEXT NOT NULL,
                        staff TEXT,
                        timestamp INTEGER NOT NULL
                    );
                    CREATE TABLE IF NOT EXISTS audit_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        actor TEXT,
                        action TEXT NOT NULL,
                        target TEXT,
                        timestamp INTEGER NOT NULL,
                        metadata_json TEXT NOT NULL DEFAULT '{}'
                    );
                    CREATE TABLE IF NOT EXISTS replays (
                        match_id TEXT PRIMARY KEY,
                        players_json TEXT NOT NULL,
                        arena TEXT,
                        kit TEXT,
                        timestamp INTEGER NOT NULL,
                        replay_data BLOB
                    );
                    """),
                new Migration(3, """
                    CREATE INDEX IF NOT EXISTS idx_player_profiles_elo ON player_profiles(elo DESC);
                    CREATE INDEX IF NOT EXISTS idx_player_profiles_last_join ON player_profiles(last_join DESC);
                    CREATE INDEX IF NOT EXISTS idx_duel_matches_timestamp ON duel_matches(timestamp DESC);
                    CREATE INDEX IF NOT EXISTS idx_duel_matches_players ON duel_matches(player_a, player_b);
                    CREATE INDEX IF NOT EXISTS idx_reports_status_timestamp ON reports(status, timestamp DESC);
                    CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
                    CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor);
                    CREATE INDEX IF NOT EXISTS idx_module_data_updated_at ON module_data(updated_at DESC);
                    """),
                new Migration(4, """
                    UPDATE player_profiles SET rank='player' WHERE rank='member';
                    UPDATE module_data SET data_value='player'
                    WHERE module='ranks' AND data_key LIKE 'player.%' AND lower(data_value)='member';
                    """)
,                new Migration(6, """
                    CREATE TABLE IF NOT EXISTS plugin_registry (
                        plugin_id TEXT PRIMARY KEY,
                        version TEXT NOT NULL,
                        enabled INTEGER NOT NULL DEFAULT 1,
                        registered_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        metadata_json TEXT NOT NULL DEFAULT '{}'
                    );
                    CREATE TABLE IF NOT EXISTS auth_accounts (
                        uuid TEXT PRIMARY KEY,
                        password_hash TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        failed_attempts INTEGER NOT NULL DEFAULT 0,
                        locked_until INTEGER NOT NULL DEFAULT 0
                    );
                    CREATE TABLE IF NOT EXISTS security_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT,
                        ip_hash TEXT,
                        event_type TEXT NOT NULL,
                        risk_score INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL,
                        metadata_json TEXT NOT NULL DEFAULT '{}'
                    );
                    CREATE INDEX IF NOT EXISTS idx_plugin_registry_updated ON plugin_registry(updated_at DESC);
                    CREATE INDEX IF NOT EXISTS idx_security_events_created ON security_events(created_at DESC);
                    CREATE INDEX IF NOT EXISTS idx_security_events_uuid ON security_events(uuid, created_at DESC);
                    """),
                new Migration(5, """
                    CREATE TABLE IF NOT EXISTS shop_products (
                        product_id TEXT PRIMARY KEY,
                        display_name TEXT NOT NULL,
                        material TEXT NOT NULL,
                        price INTEGER NOT NULL CHECK(price >= 0),
                        enabled INTEGER NOT NULL DEFAULT 1,
                        metadata_json TEXT NOT NULL DEFAULT '{}'
                    );
                    CREATE TABLE IF NOT EXISTS player_shop_purchases (
                        uuid TEXT NOT NULL,
                        product_id TEXT NOT NULL,
                        purchased_at INTEGER NOT NULL,
                        PRIMARY KEY (uuid, product_id)
                    );
                    CREATE TABLE IF NOT EXISTS training_sessions (
                        session_id TEXT PRIMARY KEY,
                        uuid TEXT NOT NULL,
                        drill TEXT NOT NULL,
                        started_at INTEGER NOT NULL,
                        finished_at INTEGER,
                        score INTEGER NOT NULL DEFAULT 0
                    );
                    CREATE INDEX IF NOT EXISTS idx_reports_target_status ON reports(target, status);
                    CREATE INDEX IF NOT EXISTS idx_replays_timestamp ON replays(timestamp DESC);
                    CREATE INDEX IF NOT EXISTS idx_ffa_stats_kills ON ffa_stats(kills DESC);
                    CREATE INDEX IF NOT EXISTS idx_training_uuid_started ON training_sessions(uuid, started_at DESC);
                    """),
                new Migration(7, """
                    UPDATE player_profiles SET rank='member' WHERE lower(rank)='player';
                    UPDATE module_data SET data_value='member'
                    WHERE module='ranks' AND data_key LIKE 'player.%' AND lower(data_value)='player';
                    CREATE TABLE IF NOT EXISTS punishments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        type TEXT NOT NULL,
                        actor_uuid TEXT NOT NULL,
                        actor_name TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        expires_at INTEGER,
                        active INTEGER NOT NULL DEFAULT 1
                    );
                    CREATE INDEX IF NOT EXISTS idx_punishments_target_active
                        ON punishments(uuid, active, type);
                    CREATE INDEX IF NOT EXISTS idx_punishments_created
                        ON punishments(created_at DESC);
                    CREATE INDEX IF NOT EXISTS idx_punishments_expires
                        ON punishments(expires_at);
                    """)
        );

        for (Migration migration : migrations) {
            if (isApplied(connection, migration.version())) continue;
            try (var statement = connection.createStatement()) {
                for (String sql : migration.sql().split(";")) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty()) statement.executeUpdate(trimmed);
                }
            }
            try (var statement = connection.prepareStatement(
                    "INSERT INTO schema_migrations(version, applied_at) VALUES (?, ?)")) {
                statement.setInt(1, migration.version());
                statement.setString(2, Instant.now().toString());
                statement.executeUpdate();
            }
        }
            connection.commit();
        } catch (SQLException error) {
            try { connection.rollback(); } catch (SQLException rollback) { error.addSuppressed(rollback); }
            throw error;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private static void ensureTable(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                        version INTEGER PRIMARY KEY,
                        applied_at TEXT NOT NULL
                    )
                    """);
        }
    }

    private static boolean isApplied(Connection connection, int version) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT 1 FROM schema_migrations WHERE version=?")) {
            statement.setInt(1, version);
            try (var result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private record Migration(int version, String sql) {}
}
