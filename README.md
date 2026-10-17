# VoidFlame-Core

The shared persistence and integration foundation for **VoidFlameMC Practice & Duels**. This repository is for the Practice/Duels server direction, not SMP or survival gameplay.

## Core owns

- One SQLite database at `plugins/VoidFlame-Core/database.db`.
- Transaction-safe schema migrations, WAL, foreign keys, busy timeout and integrity checks.
- Shared storage APIs, player profiles/preferences, server settings and plugin registry.
- Practice data contracts for player profiles, ELO, wins/losses, streaks, duel matches, FFA statistics, kits/layouts, ranks, reports, audit logs and replays.
- Automatic database backups (30-minute default, 10 retained) and validated restore.
- Common scheduling, logging and service registration.
- Spawn/world settings and restricted administrative world management.

## Practice modules that consume Core

- VoidFlame-Duels
- VoidFlame-Admin
- VoidFlame-Duels (owns arenas, kits, queues, matchmaking and match lifecycle)
- VoidFlame-Security

Feature plugins must consume Core services and must not create per-plugin SQLite databases. LuckPerms may be used to grant permissions; it does not replace the Practice/Ranks feature requirements.

## Data and compatibility

Core is infrastructure only. Matchmaking, arena allocation/reset, kit editing, duels, FFA, and practice gameplay belong to VoidFlame-Duels. Administration belongs to VoidFlame-Admin; security enforcement belongs to VoidFlame-Security. Core owns shared persistence and service contracts. Shared data schemas and APIs stay centralized in Core. Historical tables are retained during migrations to avoid destructive data loss. Never delete, reset or overwrite existing player, kit, match, or settings data during a normal upgrade.

The default spawn world is `duelpvp`; configure `spawn.world` and `worlds.enabled` for the actual arena/spawn world names on your server. Public world-listing and world-teleport commands are not exposed; players use `/spawn`, while staff manage worlds through `/vfworld`.

## Build

- Paper API: 26.2
- Java toolchain: 25
- Build: `./gradlew build`
- Output: `build/libs/VoidFlame-Core.jar`

Every change must pass Java 25 / Paper 26.2 CI and storage tests before being treated as release-ready.
