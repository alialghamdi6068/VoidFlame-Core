# VoidFlameMC Practice & Duels — Active Architecture

This is the active project specification for VoidFlameMC. The server is a Minecraft Java Practice & Duels server, not SMP, survival, or a traditional lobby network.

## Repository boundaries

Only these four custom plugin repositories are active:

1. VoidFlame-Core — the single shared SQLite database, schema migrations, backups, shared APIs, player profiles, settings, service registry and common infrastructure.
2. VoidFlame-Duels — all practice gameplay: direct duels, ranked/unranked queues, matchmaking, ELO result flow, parties, FFA, arenas, arena reset, kits, personal kit layouts, spectators, reconnects, rematches, replays and match history.
3. VoidFlame-Admin — staff tools, moderation, administrative dialogs, analytics and audit workflows.
4. VoidFlame-Security — AntiBot, verification, rate limits, abuse controls and plugin-level exploit protection.

Do not create or restore standalone Kits, Arenas, Ranks, Stats, Menus, Logs, or Network plugin repositories. Their relevant features belong in the four repositories above.

## Platform and branch

- Paper API 26.2
- Java 25
- main is the working branch.
- Preserve the repository names and existing player data.

## Data ownership

Core owns the only custom database at plugins/VoidFlame-Core/database.db. Feature plugins consume Core APIs and must not create separate SQLite databases. Migrations must be transactional and non-destructive; retain historical tables and existing player, kit, match and settings data. Backups remain enabled by default (30-minute interval, 10 retained) with validation before restore.

## User interface

- Java clients use modern native Minecraft Dialogs for primary navigation and staff workflows; do not replace them with chest/inventory GUIs.
- Bedrock clients use a separate Bedrock-appropriate form UI.
- Keep the existing features and actions available when migrating screens to Dialogs.
- The server identity and copy remain Practice & Duels; no SMP or survival gameplay is introduced.

## Operational invariants

- Core must load before the three feature plugins.
- Duels owns all arena, kit, queue and match lifecycle behavior.
- Admin must not perform destructive staff actions on players actively participating in queues, matches, FFA or parties.
- Security handles plugin-level protection; actual network DDoS mitigation remains the responsibility of the hosting provider, proxy and firewall.
- Every plugin must build against Java 25 and Paper 26.2, and integration changes must be verified across all four repositories.