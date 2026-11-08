# FyreDisguise

Experimental Java 8 / Spigot 1.8.8 disguise framework for Minecraft 1.8.9 clients.

## Currently implemented

- Global and per-viewer disguise aliases, safe synchronous mutations and logout cleanup.
- ProtocolLib 4.8.0 rewriting of outgoing `PLAYER_INFO ADD_PLAYER` packets.
- Experimental tab-list-only refresh/restore, queued, bounded and disabled by default.
- Asynchronous bounded Mojang texture lookups, LRU caching, deduplication and stale-result protection.
- Global **and per-viewer** signed texture-property assignment for existing disguise state.
- Admin-only test commands, Maven CI, JUnit tests, downloadable development JAR.

## Build

Java 8 + Maven: `mvn clean verify`. Requires a Spigot 1.8.8 server and ProtocolLib 4.8.0, installed separately.

## Operator-only test commands

```text
/fyredisguise set <target> <alias>
/fyredisguise clear <target>
/fyredisguise status <target>
/fyredisguise view <viewer> <target> <alias>
/fyredisguise unview <viewer> <target>
/fyredisguise viewstatus <viewer> <target>
/fyredisguise skin <target> <skin-source>
/fyredisguise viewskin <viewer> <target> <skin-source>
```

All named users must be online. Permission: `fyredisguise.admin` (defaults to ops). For `skin`, first set a global alias. For `viewskin`, first set a viewer-specific alias using `view`; it never silently creates an override from the global fallback. Mojang profile-property signatures must be present; cryptographic verification is not implemented. Texture lookups do not block the server thread and will not overwrite a newer alias.

The option `experimental-tab-refresh: false` in `plugins/FyreDisguise/config.yml` remains disabled by default. Only enable on a disposable test server. Tab-list updates do not make existing world entities redraw their skins or nameplates.

## Remaining before a full production release

1. Reliable world-entity name and skin rendering using tested 1.8 packet behavior.
2. Safe entity refresh and identity restoration without harming PvP tracking, hit registration or visibility.
3. Scoreboard nametag integration, vanish/NPC/spectator compatibility, reconnect/respawn validation.
4. Live Spigot 1.8.8 and Minecraft 1.8.9 multi-client integration, soak and load tests.
5. Owner-selected open-source license and final production packaging.

**Green CI proves only Maven compilation and tests, not working in-world disguises.**
