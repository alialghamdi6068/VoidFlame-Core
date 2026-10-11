# VoidFlameMC SMP — Canonical Project Specification

This file is the source of truth for the current project direction. The old Practice/Duels design is retired and must not be used as the product specification for new work.

## Identity and platform

- Server: VoidFlameMC
- Product: persistent survival SMP inspired by the modern DonutSMP gameplay and June 2026 update.
- Branding, copy, configuration defaults, names, icons and custom implementation belong to VoidFlameMC.
- Target runtime: Paper 26.2, Java 25.
- Active branch: main only.
- Preserve the single persistent survival world and player progress across restarts.
- Full loot is enabled: keepInventory must remain false in the persistent survival world.
- Player raiding and block breaking are allowed by default. Do not add land claims or blanket protection unless explicitly requested.

## Active custom plugin repositories

1. VoidFlame-Core — central storage, migrations, shared APIs, backup/restore, world configuration.
2. VoidFlame-Security — AntiBot, exploit protection, anti-cheat and combat-logout punishment.
3. VoidFlame-Economy — balance accounts, /bal, /pay and transactional money operations.
4. VoidFlame-Shop — retained /shop Quick Buy using active player-market listings and live listing prices.
5. VoidFlame-Sell — /sell and routing to compatible buyer orders when they pay more than server sell prices.
6. VoidFlame-AH — /ah listings, browsing, buying, cancelling and automatic routing into better-paying orders.
7. VoidFlame-Order — funded /orders, escrow, matching against eligible auction listings, and claimable deliveries.
8. VoidFlame-Social — friends, requests, following and social state.
9. VoidFlame-UI — native Minecraft Dialogs, dashboard, settings, stats, scoreboard and tablist.

Use LuckPerms for ranks, groups, inheritance and permissions. Do not create or depend on a custom VoidFlame-Ranks plugin.

## Central data invariant

All custom VoidFlame modules use the single database at:
plugins/VoidFlame-Core/database.db

No feature plugin may create its own SQLite database. Core owns schema migrations, WAL, foreign keys, busy timeout, integrity checks, automatic backups (30 minutes, keep 10 by default) and validated restore. Keep historical tables during migration rather than destructively dropping existing data; legacy Practice/Duels tables are not part of the new feature design.

## Modern DonutSMP-inspired behavior

- Use native Paper/Minecraft Dialogs for primary navigation and settings on supported clients; do not recreate the primary dashboard as an old chest menu.
- Keep /shop, even though DonutSMP removed its former fixed shop. In VoidFlameMC it is a Quick Buy view over active player listings.
- /sell compares server sell value with compatible open buyer orders and fills a higher-paying order first.
- Auction listings can fill compatible higher-paying open orders.
- Creating a funded order reserves the buyer's money and automatically purchases matching auction listings at or below the order's unit price.
- Remaining order funds must be refunded when an order fills or is cancelled; expired orders must refund unused escrow.
- Orders create claimable item deliveries. Never silently delete a buyer's purchased items.
- Modern Friends & Following replaces a Teams-centered social design.
- Scoreboard information is intentionally minimal: balance, shards, kills, deaths and playtime.
- Staff use a colored star in the tab list instead of a large rank-name prefix.
- Random teleport should be immediate on command execution with a configurable 15-second cooldown.
- Native death drops and player-raiding mechanics remain enabled; Security may prevent exploits but must not disable ordinary survival behavior.

## Security and reliability requirements

- Prevent money creation through duplicate requests, repeated clicks, or invalid order/listing state transitions.
- Use conditional updates for market state transitions and preserve a transaction/audit trail for account changes.
- Use escrow for buy orders. Cancellation, expiry and failed creation must refund the correct remaining amount.
- Store item deliveries before marking a purchase complete. Avoid item duplication and avoid losing items when a delivery cannot be claimed.
- Combat logout while tagged is treated as a death and drops storage, armor and offhand items, unless a trusted permission bypass applies.
- Network DDoS mitigation requires the hosting provider, proxy and firewall; the plugin is not a replacement for network-layer protection.
- Keep messages, prices, menu text, timers, and security thresholds configurable.
- Do not claim visual parity with DonutSMP until the exact current reference UI has been checked. Implement the documented native Dialog direction and verify the final visuals in-game.

## Acceptance criteria

- Every active repository builds on Java 25 against Paper 26.2.
- All plugins use Core storage and LuckPerms; no per-plugin database files or custom ranks plugin.
- Full loot, persistent world, economy, sell routing, auction house, funded orders, claimable deliveries and social state work across restarts.
- Dialog dashboard, settings, statistics, scoreboard and tablist function in the target client/server version.
- Tests cover economy balance invariants, order escrow, partial fills, cancellation/expiry refunds, concurrent listing purchases, delivery claims and database recovery.
