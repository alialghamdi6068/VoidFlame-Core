# VoidFlame-Core

Shared foundation and public API for the VoidFlame Minecraft server.

## Scope

VoidFlame-Core contains only cross-plugin infrastructure:

- Service registration and lookup.
- Shared configuration primitives.
- Centralized logging.
- Tracked scheduling and deterministic shutdown.
- A small stable API for the other VoidFlame plugins.

Duels, Arenas, Kits, Stats, Ranks, Menus, Logs, and Security remain in their owning plugins.

## Build

Run the Gradle build task. The plugin JAR is generated in build/libs.

## Compatibility

The project targets the current Paper 26.2 API and Java 25. It uses the conventional plugin.yml loader instead of the experimental Paper plugin manifest, keeping the Core compatible with the normal Bukkit/Paper plugin ecosystem.

## Architecture

Other VoidFlame plugins should depend on VoidFlame-Core and communicate through its public API. Core must not contain feature-specific game logic.

Repository: https://github.com/alialghamdi6068/VoidFlame-Core
