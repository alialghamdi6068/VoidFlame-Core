# VoidFlameMC SMP — Installation

## Runtime
- Paper 26.2
- Java 25
- Use one persistent survival world named `world` unless `smp.persistent-world` is changed in VoidFlame-Core's config.
- Keep `online-mode=true` unless the server owner explicitly decides otherwise.

## Custom plugins
Install the built JARs from these repositories into the server's `plugins/` directory:

1. `VoidFlame-Core`
2. `VoidFlame-Security`
3. `VoidFlame-Economy`
4. `VoidFlame-AH`
5. `VoidFlame-Order`
6. `VoidFlame-Sell`
7. `VoidFlame-Shop`
8. `VoidFlame-Social`
9. `VoidFlame-UI`

Also install **LuckPerms** for groups and permissions. Do not install a custom VoidFlame-Ranks plugin.

Paper resolves the declared plugin dependencies automatically. Do not create separate databases for feature plugins. Core owns `plugins/VoidFlame-Core/database.db` and its backup directory.

## Main commands
- `/smp` — main Dialog dashboard
- `/spawn` — survival spawn
- `/shop [page]` — Quick Buy Dialog with exact item metadata matching and a 3x displayed unit-price ceiling
- `/shardshop [page]` — premium shard catalog
- `/shards` — shard balance
- `/sell` and `/sellhistory` — sell items and view recent sales
- `/ah [page <number>]` — auction house; `/ah sell <price>` lists the held item
- `/orders` — funded buyer orders and claimable deliveries
- `/order <material-or-buyer>` — search open orders
- `/orders claim` — claim purchased items and returned listings
- `/friend`, `/friends`, `/follow`, `/following` — social features
- `/sethome [name]`, `/home [name]`, `/delhome <name>`, `/rtp`, `/tpa <player>`
- `/profile`, `/stats [player]`, `/baltop`, `/settings`

## Full-loot rules
Full loot is enabled in the configured survival world and its conventional `_nether` and `_the_end` dimensions. Player raiding and ordinary block breaking are allowed. Security features must prevent exploits without disabling normal survival behavior.

## Verification
Core and Security have successful Java 25 / Paper 26.2 CI builds. The remaining feature repositories must have GitHub Actions enabled and a successful build run before their JARs are considered release-ready. Do not install unverified artifacts on a live server.
