# VoidFlame-Core

The shared persistence and integration foundation for **VoidFlameMC SMP** on Paper 26.2 / Java 25.

## Core responsibilities

- One authoritative SQLite database at `plugins/VoidFlame-Core/database.db`; feature plugins must never create separate databases.
- Transaction-safe schema migrations, WAL, foreign keys, busy timeout and integrity checks.
- Shared database, player profile, player preference, server settings, economy and plugin-registry APIs.
- SMP player profiles with kills, deaths and playtime, tracked centrally by Core.
- Shared economy accounts and transaction ledger for balances and shards.
- Central schemas for sell prices, Auction House listings, funded buyer orders, order deliveries, friends/following, homes and player preferences.
- Preserved legacy Practice/Duels tables for non-destructive migrations; those tables do not define the active SMP gameplay direction.
- Automatic database backups (30-minute default, 10 retained) and validated restore.
- Spawn/world settings and restricted administrative world management.

## SMP feature plugins that consume Core

- `VoidFlame-Economy`: balances, shards and player-to-player payments.
- `VoidFlame-Shop`: retained `/shop` Quick Buy entry point.
- `VoidFlame-Sell`: `/sell` with buyer-order routing.
- `VoidFlame-AH`: player Auction House.
- `VoidFlame-Order`: funded buyer orders and claimable deliveries.
- `VoidFlame-Social`: friends, following, homes, random teleport and player teleport requests.
- `VoidFlame-UI`: native Minecraft Dialog navigation and SMP settings/stats.
- `VoidFlame-Security` and `VoidFlame-Logs`: security and audit integrations where installed.
- `VoidFlame-TAB`: the custom player-list presentation.

LuckPerms is the permission/rank authority for the SMP setup. Do not install the legacy custom Practice Ranks module as a second permission authority.

## Data and compatibility

Core is infrastructure, not a feature-plugin collection. All SMP modules use the same Core database and shared economy service. Money mutations must use Core's economy API so balance checks and ledger entries remain consistent. Schema changes belong in Core migrations; feature plugins must not silently create their own database or independent copies of shared state.

The current configured persistent world is `duelpvp`, set by `spawn.world` and `worlds.enabled`. Keep that name if it contains the existing world data; do not rename or replace the world during a normal plugin update. Players use `/spawn`; administrative world management uses `/vfworld`.

Historical tables and player data are retained during migrations. Never reset, delete or overwrite existing player, market, home, economy or settings data during a normal upgrade. Database restore requires a server restart.

## Build

- Paper API: 26.2
- Java toolchain: 25
- Build: `./gradlew build`
- Output: `build/libs/VoidFlame-Core.jar`

Every change must pass Java 25 / Paper 26.2 CI and storage tests before being treated as release-ready.
