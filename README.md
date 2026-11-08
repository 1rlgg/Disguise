# FyreDisguise

An **experimental** Java 8 / Spigot 1.8.8 disguise API for Minecraft 1.8.9 clients.

## Implemented

- Global/per-viewer alias state with ProtocolLib 4.8 outgoing `PLAYER_INFO ADD_PLAYER` rewriting.
- Optional tab-list-only refresh (disabled by default, not yet live-tested).
- Permission-gated `/fyredisguise set|clear|status` commands for online player aliases.
- Bounded Mojang skin lookup queue with concurrent request coalescing, expiring LRU cache, and shutdown cancellation.
- Unit tests and Maven CI checks.

## Build and install

```sh
mvn clean verify
```

Use a Spigot 1.8.8 server with Java 8 and ProtocolLib 4.8 installed. The JAR does not bundle ProtocolLib.

`plugins/FyreDisguise/config.yml` defaults to:

```yaml
experimental-tab-refresh: false
```

**Do not enable on a live PvP server** until tested. For a disposable test server only, set `experimental-tab-refresh: true`, restart, and use the commands below.

## Operator-only test commands

- `/fyredisguise set <online-player> <alias>`
- `/fyredisguise clear <online-player>`
- `/fyredisguise status <online-player>`

Requires `fyredisguise.admin` (defaults to server operators). These commands modify global state; existing viewers receive an **experimental tab-list refresh only when enabled**. They do not change in-world entity names or skins.

## Known limitations

**NOT production-ready.** World entity disguises, skin switching for existing entities, scoreboard nameplates, respawn/reconnect synchronization, and reliable full restore are unfinished. Live Spigot 1.8.8/1.8.9 client testing is required, particularly around tab-list ordering, vanish, scoreboard plugins, and combat consistency. No open-source license has been selected yet.

Next priorities: safe live entity refresh in a test environment, restore behavior, per-viewer nameplates, protocol integration tests and performance profiling.
