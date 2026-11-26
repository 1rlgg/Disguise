# Disguise release validation checklist

This is a **manual acceptance plan**, not a record of passed live tests.
The current GitHub Actions pipeline compiles Java 8 code and runs JUnit tests,
but it does not launch a Spigot 1.8.8 server or Minecraft 1.8.9 clients.

## Environment and evidence

- [ ] Disposable Spigot 1.8.8 server with Java 8 and ProtocolLib 4.8.0
- [ ] Two or more vanilla Minecraft 1.8.9 clients, distinct authenticated profiles
- [ ] Record exact server build, ProtocolLib build, installed plugins and config
- [ ] Baseline server TPS, player count, packets/sec and relevant logs
- [ ] Keep last known-good CI JAR ready, along with world/plugin backups
- [ ] Confirm both experimental flags remain `false` in production

## Correctness

- [ ] Global alias visible in incoming player-list entries; original UUID preserved
- [ ] Viewer override affects only intended viewer and never local self-profile
- [ ] Signed skin applies to tab and existing world model where supported
- [ ] Alias change, `clear`, `unview`, `clearPlayer` and plugin shutdown restore expected identity
- [ ] Join, quit, death, respawn, teleport and world change do not leave ghost/duplicate entities
- [ ] Head orientation, held item, armor, skin layers and metadata remain correct after refresh
- [ ] Recovery from packet rejection does not strand a client-side entity

## Competitive gameplay and interoperability

- [ ] Hit registration, reach, knockback, attack cooldown/combos, potions and item use match baseline
- [ ] Vanished players remain invisible to unauthorized viewers
- [ ] Scoreboard team prefixes, suffixes, friendly visibility and collision behavior remain unchanged
- [ ] NPC, spectator, mounted, sleeping and dead player states are not broken
- [ ] Fast repeated aliases and reconnects do not create excess packet bursts
- [ ] No unexpected server tick spikes, off-thread Bukkit access or memory growth

## Load and failure testing

- [ ] Repeat alias/undisguise bursts with 10, 25, 50 and 100+ simulated player pairs
- [ ] Confirm `/disguise stats` reports queue saturation and displaced restoration work
- [ ] Exercise Mojang timeouts, concurrent requests, cancellation and executor saturation
- [ ] Inject packet failures in a disposable environment and inspect recovery logs
- [ ] Validate after 30+ minutes sustained load and several plugin restart cycles
- [ ] Compare packet traffic and TPS with both experiments disabled and enabled separately

## Release gate

Do **not** declare production-ready until all blocking checks are evidenced,
the owner approves gameplay-affecting behavior, and a release license is selected.
Any client invisibility, combat regression, or stranded identity is a no-go:
disable both experimental options immediately and revert the faulty commit normally.


## New hard gates: async session validity and disable load

- [ ] Rapid join/quit/rejoin during the 2-tick resync delay never applies an
      old session's work to the new session
- [ ] Multiple respawn/world-change events coalesce to the most recent
      delayed resync ticket
- [ ] A first failed experimental world refresh attempts tab fallback and
      leaves the world failure breaker accounting accurate
- [ ] On disable with over 256 applied pairs, packet restoration is bounded,
      unattempted pairs are reported, and client reconnect clears any
      stale identity state
- [ ] Measure main-thread shutdown delay, packet rate and client appearance
      before considering a larger shutdown packet budget


## Tab identity restoration acceptance gates (cycle 33)

- [ ] With a global alias applied to A, remove it and verify B sees A's
      authentic tab profile name and original skin after a bounded refresh
- [ ] With a viewer-specific override, remove it and verify the affected
      viewer sees the global alias, while unrelated viewers remain unchanged
- [ ] Remove an alias immediately before respawn or world change; the
      delayed resync must include any previously applied alias still awaiting
      restoration, even if the disguise registry is already empty
- [ ] With another plugin hiding A from B, reconciliation must not send
      restore packets that would reveal A; when visible again, verify the
      queued restoration reaches B without duplicated tab entries
- [ ] Inject an exception after REMOVE or ADD and verify the recovery sequence
      attempts REMOVE plus the original ADD even if cleanup itself fails
- [ ] Disable while alias state is still populated and verify the dedicated
      shutdown path sends only original GameProfile data, not a rewritten alias
- [ ] Record actual 1.8.9 client identity after each failure; successful
      transaction tests alone are not evidence that the client recovered


## Operator reproduction steps (cycle 34)

On a disposable Java 8 / Spigot 1.8.8 server with ProtocolLib 4.8.0
and two vanilla Minecraft 1.8.9 clients (A and B):

1. Set `experimental-tab-refresh: true`, leaving entity refresh false.
   Apply `/disguise set A TestAlias` and confirm B's tab entry behavior.
2. Run `/disguise inspect B A` and record alias, visibility, world,
   spectator and scoreboard-team ownership. The command must leave
   scoreboard membership and server visibility unchanged.
3. Change A's alias, run `/disguise clear A`, then repeat with a
   viewer-specific override. Verify B sees the real profile again.
4. Run `/disguise reset` while both global and viewer overrides and
   texture lookups are pending. Confirm all registry aliases clear and
   no late texture callback reinstates them.
5. Inject repeated packet-send failures until a pair is quarantined.
   Confirm periodic/delayed resyncs do not cause unbounded resend attempts.
   Create a new identity transition; verify the pair becomes retry-eligible.
6. Repeat with controlled world refresh enabled **only on the disposable
   server**, observing other plugins' scoreboards, vanish, spectators,
   player skin, head rotation, held item, armor and hit registration.
7. Record log excerpts, TPS, packet counts and actual client screenshots
   before checking any acceptance box. Disable experimental modes if any
   client becomes invisible, ghosted or visually inconsistent.


## Cycle 35: collision and targeted repair checks

- [ ] On a disposable 1.8.8 server, give the alias username to another
      online player and verify experimental world refresh refuses collision
      while the other player's entity remains intact
- [ ] Assign the alias to a third-party scoreboard team and verify
      `/disguise inspect <viewer> <player>` reports the block without
      modifying scoreboard ownership, nametag rules or invisibility
- [ ] Apply then clear an alias; if B's tab still shows the alias,
      run `/disguise restore B A` and verify B sees A's authentic name
      and signed skin afterward
- [ ] Confirm restore refuses a currently disguised pair and a viewer who
      cannot see the subject (vanish compatibility)
- [ ] With a manual restoration queued, apply a new alias immediately;
      the old tab-only repair must not override that newer disguise
- [ ] Inject tab transport failures; confirm targeted repair is bounded
      by retry limits and never sends world destroy/respawn packets
- [ ] Record real-client screenshots and packet traces; unit-test success
      is not proof of a correct overhead nametag or original-world skin


## Viewer-visible duplicate aliases and ambiguous send recovery (cycle 36)

- [ ] Use two online disguised subjects with matching aliases and verify
      the world renderer skips synthetic respawn for both, while no other
      players become invisible or ghosted
- [ ] Override one subject's alias for just one viewer; verify collision
      checks differ for that viewer and all other viewers remain unaffected
- [ ] Check aliases that collide case-insensitively and with a third
      party's online real username
- [ ] After a simulated tab send throws despite packet delivery, verify
      the real 1.8.9 client converges to the original UUID, display name
      and signed textures without duplicated entries
- [ ] Inject repeated cleanup failures; verify bounded retries and
      recovery logging, then re-test nameplates and hit registration
- [ ] Do not mark "complete" without actual client evidence and a
      scoreboard/vanish compatibility test on a disposable server


## Display-name packet and collision acceptance (cycle 37)

- [ ] With player A disguised to an alias, send real 1.8
      UPDATE_DISPLAY_NAME for A; viewer B must not see A's real username
      return in the tab list
- [ ] Confirm UPDATE_LATENCY, UPDATE_GAME_MODE and REMOVE_PLAYER retain
      normal vanilla/third-party behavior and cancelled packets stay untouched
- [ ] Assign the same alias to A and C for B; verify safe vanilla fallback
      rather than conflicting profile usernames; remove one alias and verify
      the other's appearance can refresh without an extra command
- [ ] Repeat collision introduction/removal with a viewer-specific override
      and with clearPlayer, ensuring unrelated viewers are unaffected
- [ ] Test synthetic in-world entity refresh at 16, 40, 48 and 49 blocks,
      with player tracker range configured, and record any ghost entities
- [ ] Verify third-party scoreboard teams and vanish visibility are untouched
- [ ] Do not enable experimental world refresh for competitive use until
      multiplayer appearance, equipment, hit registration and TPS are evidenced
