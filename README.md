# FyreDisguise

Experimental Java 8 / Spigot 1.8.8 disguise framework, targeting Minecraft 1.8.9 clients.

## Implemented

- In-memory global and per-viewer disguise API.
- ProtocolLib 4.8.0 rewriting of outgoing `PLAYER_INFO ADD_PLAYER` entries.
- Optional **experimental tab-list live refresh**, using targeted `REMOVE_PLAYER` and `ADD_PLAYER` messages.
- Viewer visibility checks; no `hidePlayer/showPlayer` calls that could disrupt combat or vanish plugins.
- Mojang skin-property lookups with bounded asynchronous workers and expiring cache.
- Unit tests and Maven CI.

**Important:** This is not a full player disguise plugin. World entities, overhead nameplates, entity spawns, and client-side skin reloads are not consistently updated. No production release is recommended.

## Build

Requires Java 8, Maven, Spigot 1.8.8, and **ProtocolLib 4.8.0 installed on the server**.

```sh
mvn clean verify
```

## Configuration

`plugins/FyreDisguise/config.yml`:

```yaml
experimental-tab-refresh: false
```

The tab refresh feature is disabled by default. Only opt in on a disposable test server. It updates tab-list metadata without forcing entity respawns. To change desired identity state, another plugin must call the API; no admin command has been added yet.

## Manual testing required

1. Connect two Minecraft 1.8.9 clients to a Spigot 1.8.8 test server with ProtocolLib 4.8.0.
2. Confirm normal tab and world appearance with the option disabled.
3. Set global alias, change alias, remove alias, and check the viewer's tab entry.
4. Set a viewer-only alias and confirm unrelated viewers retain the global identity.
5. Test a vanished target, spectator mode, world changes, death/respawn, and reconnect.
6. Confirm combat reach, hit registration, entity visibility and tab sync are unaffected.
7. Observe CPU, tick time, network traffic and packet exceptions under realistic player counts.

## Remaining blockers

- Safe live entity refresh and nameplate replacement without altering combat visibility.
- Safe skin/profile restoration after reconnect, respawn, world changes and plugin disable.
- Vanish, scoreboard, NPC, and third-party plugin compatibility.
- Integration tests on the actual 1.8.8 server and 1.8.9 clients.
- Select an open-source license before public distribution.
