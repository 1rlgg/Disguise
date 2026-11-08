# FyreDisguise

Experimental Java 8 / Spigot 1.8.8 disguise framework for Minecraft 1.8.9 clients.

## Implemented and testable

- Public disguise state API with global/per-viewer aliases and lifecycle cleanup.
- Main-thread enforcement on mutation methods, including retained service references.
- ProtocolLib 4.8.0 rewriting of outgoing `PLAYER_INFO ADD_PLAYER` packet identities, with fail-open circuit breaker.
- Optional experimental live tab-list update (disabled by default).
- Admin commands for global and per-viewer testing.
- Async Mojang skin-property requests with bounded workers, coalescing, cache expiry and safe shutdown.
- Java 8 Maven verification and development JAR artifact in GitHub Actions.

## Build

```sh
mvn clean verify
```

Runtime requires Spigot 1.8.8 and ProtocolLib 4.8.0 installed separately. This is not a tested production plugin.

## Test commands (requires `fyredisguise.admin`, defaults to operators)

```text
/fyredisguise set <online-player> <alias>
/fyredisguise clear <online-player>
/fyredisguise status <online-player>
/fyredisguise view <viewer> <online-player> <alias>
/fyredisguise unview <viewer> <online-player>
/fyredisguise viewstatus <viewer> <online-player>
```

For isolated testing only, set `experimental-tab-refresh: true` in `plugins/FyreDisguise/config.yml` and restart. Leave the option **false** on PvP production servers. Tab-list changes are not full entity disguises.

## Missing before release

1. True client-visible world entity name/skin changes, with safe entity tracking and refresh.
2. Full restoration on undisguise/disable, reconnect, respawn and cross-world transitions.
3. Compatibility with vanish, scoreboard-team nametags, NPC and spectator plugins.
4. Integration tests on Spigot 1.8.8 and Minecraft 1.8.9 clients, including combat visibility and performance.
5. Owner-approved open-source license and release documentation.

Unit test success does **not** establish working world disguises or production safety.
