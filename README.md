# FyreDisguise

Experimental Java 8 / Spigot 1.8.8 disguise framework targeting Minecraft 1.8.9 clients.

## Current functionality

- Global and per-viewer aliases backed by a validated state API.
- ProtocolLib 4.8.0 rewriting of outgoing `PLAYER_INFO ADD_PLAYER` identities.
- Experimental **tab-list-only** changes, resynchronization, and best-effort restoration, disabled by default.
- Signed Mojang texture-property lookup by UUID, asynchronous bounded I/O, caching and request coalescing.
- New permission-gated **skin assignment command**, with stale-result protection.
- Java 8 Maven CI, regression tests and a downloadable development JAR.

## Build

```sh
mvn clean verify
```

Runtime dependencies: Spigot 1.8.8 and ProtocolLib 4.8.0. Java 8.

## Admin testing

Requires the `fyredisguise.admin` permission (operators by default):

```text
/fyredisguise set <online-player> <alias>
/fyredisguise clear <online-player>
/fyredisguise status <online-player>
/fyredisguise view <viewer> <online-player> <alias>
/fyredisguise unview <viewer> <online-player>
/fyredisguise viewstatus <viewer> <online-player>
/fyredisguise skin <online-target> <online-skin-source>
```

First set a global alias for the target. Then `skin` asynchronously fetches the source player's Mojang-signed textures and associates them with the target's global disguise. A concurrent alias change, target logout, or plugin shutdown discards the result. The source player must be online so the command can determine the authenticated UUID. **Signed-property presence is checked, but cryptographic signature verification is not implemented.**

`experimental-tab-refresh: false` in `plugins/FyreDisguise/config.yml` remains the safe default. Enable only on a disposable test server. Setting aliases or skins does **not** immediately change the appearance of existing world entities.

## Remaining release-critical work

1. Complete, client-visible player entity name and skin refresh, with no combat/tracking regressions.
2. Correct client restoration for all disguise transitions, disconnect/reconnect and plugin disable.
3. Scoreboard/team nametag interoperability, vanish and NPC compatibility.
4. Real Spigot 1.8.8 and Minecraft 1.8.9 multi-client tests and load benchmarks.
5. Select an open-source license and prepare an audited release.

**A green Maven CI run is not equivalent to working in-world disguises.** Not suitable for production PvP use yet.
