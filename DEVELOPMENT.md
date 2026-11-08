# Development policy and production-readiness gates

This repository contains **FyreDisguise**, a Spigot 1.8.8 plugin targeting Minecraft 1.8.9 clients. It does not contain the rest of a PvP server (combat, anticheat, economy, proxy, etc.). Work here must not be represented as validation of those absent systems.

## Autonomous change boundaries

| Change type | Policy |
| --- | --- |
| Safe correctness fixes, targeted refactors, performance improvements, unit tests, build hygiene, documentation | Implement, run CI, and push meaningful atomic commits to `main` when this is the repository's established workflow. |
| Packet send ordering, experimental entity destroy/respawn, tab/profile rewrites | Maintain explicit opt-in flags, preserve original UUID/entity ID, fail open, and require multi-client testing before enabling on production. |
| Scoreboard/vanish/spectator/NPC integrations | Never overwrite other plugins' team rules or bypass visibility. Prefer conservative skip/fallback until an integration contract is tested. |
| Changes affecting PvP hit registration, damage, reach, movement, server tick rate or entity tracking | Do not autonomously enable in production. Require live test-server evidence and explicit approval for changing gameplay semantics. |
| Granting permissions, collecting player data, external downloads, dependencies, publishing releases or license choices | Do not expand permissions or access, add invasive dependencies, deploy publicly, or select a legal license without owner approval. |
| Destructive changes to builds, branches, assets, server/world/player data | Never force-push or reset `main`, and never migrate/delete real server data without a reviewed rollback plan. |

## Batch acceptance checks

1. Identify three to four meaningful improvements where possible (correctness, performance, resilience, and testing), rather than inflating commit counts.
2. Review affected state transitions and APIs for **Spigot 1.8.8**, Java 8, and ProtocolLib 4.8.0 compatibility.
3. Add focused regression tests for modified invariants, cancellation, overload, teardown, and restoration when practical.
4. Run `mvn --batch-mode --no-transfer-progress clean verify` (GitHub Actions if Maven is not available locally).
5. Push to `main` only as a fast-forward; confirm the branch SHA and **the final-head CI run** succeeded. CI test success does not imply live client behavior.
6. Keep `experimental-tab-refresh: false` and `experimental-entity-refresh: false` by default.
7. Report any remaining known failure modes and exact tests not performed.

## Manual production-readiness matrix (currently NOT completed)

Use a disposable Spigot 1.8.8 server with ProtocolLib 4.8.0 and two or more Minecraft 1.8.9 clients. Separately test with both experiments disabled and individually enabled as permitted by the config.

- Apply and remove global and per-viewer alias/skin while idle, moving, fighting and at chunk/view range boundaries.
- Death, respawn, teleport, world changes, vanish, spectator, mounts and sleeping players; reload/disable and restore original identity.
- Correct skin and overhead name for each viewer, scoreboard team prefixes/suffixes, NPC and vanish-plugin behavior; no client-visible ghost or invisible entities.
- Combat hit registration, reach, knockback, hitboxes, server TPS, packet bandwidth and memory during repeated skin/disguise commands under load.
- Verify `/fyredisguise stats` counters do not conceal dropped refreshes or failed restores; compare network traces when necessary.

Do not call this plugin production-ready until these tests pass with reproducible results.

## Standard progress update format

Every substantial development cycle should use these headings:

- **Completed:** Three to four workstreams, concrete behavior changes and commit links.
- **Verified:** Maven/CI job URL, exact final commit SHA, test result, and explicit live tests not run.
- **Pushed:** Target repository and `main` ref, with confirmation of a successful fast-forward.
- **Risks:** Experimental behavior, unresolved regressions, compatibility boundaries and rollback notes.
- **Next:** Ranked list of the next three to four high-value improvements and remaining release blockers.

A failed CI run must be disclosed and fixed in a subsequent commit, never hidden by citing an older passing build.

## Immediate rollback

Disable `experimental-entity-refresh` and `experimental-tab-refresh` and restart the test server if clients flicker, disappear, or show unexpected names. For a code regression, revert the offending commit(s) normally and verify the resulting `main` with CI. Do **not** force-push. Keep a known-good development JAR from GitHub Actions for test-server recovery.
