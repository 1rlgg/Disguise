# FyreDisguise

Experimental Java 8 / Spigot 1.8.8 disguise framework for Minecraft 1.8.9 clients.

## What is implemented

- Per-viewer and global alias state, with per-viewer lookup and cleanup.
- ProtocolLib 4.8.0 rewrite of outgoing `PLAYER_INFO ADD_PLAYER` entries.
- Optional experimental live tab-list-only remove/add updates, **disabled by default**.
- Admin-only testing commands for global and viewer-specific aliases.
- Bounded, coalesced asynchronous Mojang skin retrieval with timeout, caching, and shutdown cleanup.
- Unit tests and GitHub Actions build verification.

## Commands (operator or `fyredisguise.admin`)

```
/fyredisguise set <online-player> <alias>
/fyredisguise clear <online-player>
/fyredisguise status <online-player>
/fyredisguise view <viewer> <online-player> <alias>
/fyredisguise unview <viewer> <online-player>
/fyredisguise viewstatus <viewer> <online-player>
```

These manipulate **desired identity state**, not complete player entity disguises.

## Build and install

Run `mvn clean verify` on Java 8. Requires Spigot 1.8.8 and ProtocolLib 4.8.0 installed on the server.

`plugins/FyreDisguise/config.yml`:

```yaml
experimental-tab-refresh: false
```

This is an opt-in testing feature only, not intended for competitive production servers without real-client verification.

## What is NOT complete

- Live player entity hide/respawn or appearance refresh.
- Correct overhead nametag appearance across scoreboard/team plugins.
- Full skin-switching and client identity restoration while entities are visible.
- Respawn, world transition, NPC, vanish and spectator compatibility validation.
- End-to-end 1.8.9 client tests, packet diagnostics and load benchmarks.
- An explicitly chosen open-source license.

No production release is recommended until these limitations are resolved and tested.
