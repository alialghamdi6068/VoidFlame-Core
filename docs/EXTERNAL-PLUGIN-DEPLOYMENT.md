# VoidFlameMC External Plugin Deployment Manifest

This manifest records the external plugins requested for the VoidFlameMC Practice/Duels server. It is a deployment checklist, not a claim that third-party JARs have already been installed on a running host.

## Custom plugins (required)

Install the four custom plugins built from the official repositories:

- VoidFlame-Core — https://github.com/alialghamdi6068/VoidFlame-Core
- VoidFlame-Admin — https://github.com/alialghamdi6068/VoidFlame-Admin
- VoidFlame-Security — https://github.com/alialghamdi6068/VoidFlame-Security
- VoidFlame-Duels — https://github.com/alialghamdi6068/VoidFlame-Duels

Use Paper 26.2 and Java 25. Keep the four repositories on main; do not rename repositories or create replacement branches.

## Requested external plugins

| Plugin | Intended role | Integration note |
|---|---|---|
| FancyNpcs | Decorative and interactive NPCs | Keep NPC actions pointed at supported VoidFlame commands. |
| Chunky | World pre-generation | Pre-generate worlds before opening the server to players. |
| DecentHolograms | Holograms and server information | Use for static information; do not depend on holograms for core gameplay. |
| DeluxeHub | Spawn/hub utilities | Avoid duplicate spawn items, menus, or command overrides. |
| Essentials | General utility commands | Check command aliases against VoidFlame commands. |
| EssentialsChat | Chat formatting | Configure only one authoritative chat formatter. |
| Multiverse-Core | World management | Avoid creating a second competing source of truth for active match arenas. |
| Multiverse-Inventories | Per-world inventory policies | Do not apply inventory separation to active Duels/FFA worlds unless explicitly configured. |
| Multiverse-Portals | World portals | Keep portals away from reserved arena and active match regions. |
| PacketEvents | Packet-level compatibility for plugins that require it | Install the version required by the selected dependent plugin. |
| PlaceholderAPI | External placeholders | VoidFlame-Duels exposes the %voidflame_* placeholders documented below. |
| TAB | Player list and scoreboard presentation | Use the VoidFlame placeholders; do not create a competing scoreboard from another plugin. |
| Vault | Economy/permission bridge for compatible plugins | VoidFlame competitive coins remain owned by VoidFlame-Duels; do not assume Vault balance synchronization. |
| ViaBackwards | Older-client compatibility | Pair with compatible ViaVersion builds. |
| ViaVersion | Client-version compatibility | Verify supported clients against the server's actual Paper build. |
| Simple Voice Chat (voicechat) | Optional proximity voice chat | Configure its UDP port/firewall separately from Minecraft TCP. |
| VoidGen | Void-world generation | Use when generating a new void spawn or arena world. |
| WorldEdit | Arena/world editing | Restrict permissions and avoid editing reserved or active arenas. |
| WorldGuard | Region protection | Configure protections so they do not block legitimate match mechanics. |

## Explicit exclusions and ownership

- Do not install EssentialsSpawn. The server's intended spawn entry point is /spawn through the VoidFlame setup.
- Do not install ZonePracticePro alongside VoidFlame-Duels. The current architecture assigns queues, ranked/unranked matchmaking, ELO, kits, arenas, FFA, parties, spectators, match results, and practice statistics to VoidFlame-Duels. Installing another full Practice system would duplicate ownership and can cause command, inventory, arena, and matchmaking conflicts.
- Do not add separate custom VoidFlame Ranks, Logs, Menus, Arenas, Kits, or Stats repositories. Their current responsibilities belong to the four official custom plugins.
- Do not grant staff access based on client edition. Java and Bedrock staff permissions must be assigned explicitly on the server.

## PlaceholderAPI identifiers

VoidFlame-Duels provides these identifiers for TAB or other PlaceholderAPI-compatible plugins:

- %voidflame_wins%
- %voidflame_losses%
- %voidflame_winstreak% / %voidflame_streak%
- %voidflame_beststreak%
- %voidflame_elo%
- %voidflame_coins%
- %voidflame_rank%
- %voidflame_status% / %voidflame_mode%
- %voidflame_queue%
- %voidflame_party%

## Deployment verification

Before opening the server:

1. Install compatible builds of the requested external plugins into the server's plugins/ directory.
2. Start the server once and review the full startup log for missing dependencies, command conflicts, and configuration errors.
3. Confirm that only VoidFlame-Duels owns Practice matchmaking, kits, arenas, FFA, and party matches.
4. Verify PlaceholderAPI values and TAB output with online test accounts.
5. Verify Java Dialogs and Bedrock Forms independently.
6. Test /spawn, world management, arena reservation/reset, inventory restoration, reconnect handling, punishments, and security controls.
7. Back up the server and Core database before changing world or inventory-management configuration.

A successful GitHub build validates the custom plugin source. It does not install third-party plugins on the hosting provider or replace in-game integration testing.
