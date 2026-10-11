# VoidFlame-Core

The shared persistence and integration foundation for **VoidFlameMC SMP**. This repository replaces the old Practice/Duels product direction; new features must be designed for persistent survival and the current DonutSMP-inspired gameplay target, with VoidFlameMC branding and its own /shop.

## Core owns

- One SQLite database at plugins/VoidFlame-Core/database.db.
- Transaction-safe schema migrations, WAL, foreign keys, busy timeout and integrity checks.
- Shared storage APIs, player profiles/preferences, server settings and plugin registry.
- Central economy/market/social persistence schema used by the custom modules.
- Automatic database backups (30-minute default, 10 retained) and validated restore.
- Common scheduling, logging and service registration.
- Persistent single-world SMP configuration and administrative world tooling.

## Modules that consume Core

- VoidFlame-Security
- VoidFlame-Economy
- VoidFlame-Shop
- VoidFlame-Sell
- VoidFlame-AH
- VoidFlame-Order
- VoidFlame-Social
- VoidFlame-UI

Ranks and permissions are managed by **LuckPerms**, not a custom ranks plugin.

## Data and compatibility

Custom VoidFlame modules must use the Core database and must not create per-plugin SQLite databases. Module tables are centrally migrated. Historical Practice/Duels tables are retained during the transition to avoid destructive data loss, but they are not part of the new SMP feature design. Do not drop or overwrite existing user data as part of an ordinary upgrade.

## Build

- Paper API: 26.2
- Java toolchain: 25
- Build: ./gradlew build
- Output: build/libs/VoidFlame-Core.jar

Core is infrastructure only. Economy, selling, auctions, orders, social features and player-facing modern UI belong in their owning plugins.
