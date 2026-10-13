# VoidFlameMC Practice & Duels — Core Installation

## Runtime
- Paper 26.2
- Java 25
- This is a Practice/Duels server, not SMP. Configure `spawn.world` and `worlds.enabled` in Core's config to match the main spawn and arena worlds.
- Keep `online-mode=true` unless the server owner explicitly decides otherwise.

## Custom plugins
Install the verified JARs from the existing repositories into the server's `plugins/` directory:

1. `VoidFlame-Core`
2. `VoidFlame-Duels`
3. `VoidFlame-Arenas`
4. `VoidFlame-Kits`
5. `VoidFlame-Stats`
6. `VoidFlame-Ranks`
7. `VoidFlame-Menus`
8. `VoidFlame-Logs`
9. `VoidFlame-Security`

Core owns `plugins/VoidFlame-Core/database.db` and its backup directory. Do not create separate databases for feature plugins. Do not install SMP-only feature modules as replacements for the Practice modules.

## Core commands
- `/spawn` — return to the configured main Practice spawn.
- `/vfworld <list|info|enable|disable|load|unload|tp|setspawn|reload>` — staff-only world management.
- `/vfrestore <backup-file>` — restore a validated database backup; requires `voidflame.database.restore` and a stopped/restarted-safe maintenance window.

## Permissions
- `voidflame.spawn` defaults to everyone.
- `voidflame.world.manage` defaults to operators.
- `voidflame.database.restore` defaults to operators.

## Verification
Run `./gradlew build` using Java 25. Do not install an artifact until its GitHub Actions build and tests succeed. Back up the Core data folder before upgrading an existing server; normal upgrades must not erase or replace the existing database.
