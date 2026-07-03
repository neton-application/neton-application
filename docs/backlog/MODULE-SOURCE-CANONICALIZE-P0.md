# MODULE-SOURCE-CANONICALIZE-P0

## Status

**OPEN (severity revised down after local verification).** Extracted from `NETON-1.0-STANDARDIZATION-RC` (see `docs/NETON-1.0-STANDARDIZATION-RC.md`).
**Not part of STANDARDIZATION-RC completion** — that RC validates module-level compile + tests + docs/API cleanup only.

This is a multi-repo build-topology / included-build / canonical-workspace concern, not a code bug. It needs its own project and an explicit architectural decision before any build changes.

> **Do NOT fix this by `publishToMavenLocal`.** Publishing modules to the local Maven repo to make a build green only hides the topology gap — any fix must work from a fresh checkout with no local publish step.

---

## Verified findings (corrects an earlier over-statement)

An earlier draft claimed the aggregate `:application` compile was a broken release gate. Local verification revised this:

- **`privchat-application` (the product / commercial validation project) `:application:compileKotlinMacosArm64` is GREEN.** Verified locally (member fix included, full aggregate compiles).
- **`neton-application` (the base distribution) standalone `:application:compileKotlinMacosArm64` FAILS** with `Could not find com.netonstream.app:module-system` / `module-infra` inside the `module-member`/`payment`/`platform` included builds.

**Why the product works but the base doesn't** (the load-bearing mechanism):
- In `privchat-application`, the product modules `privchat-application-module-game` and `...-assistant` each `includeBuild("../privchat-application")`. That pulls the root `privchat-application` (which owns `module-system`/`module-infra` as subprojects) back into the composite **as an included build**. With the root present as an included build, its `module-system`/`module-infra` subprojects become substitutable for the `com.netonstream.app:module-system`(+`infra`) dependencies declared by the canonical `member`/`payment`/`platform` included builds.
- In `neton-application`, **nothing pulls `neton-application` back into the composite as an included build.** Its `module-system`/`module-infra` are root subprojects, unreachable to its own included `member`/`payment`/`platform` (composite substitution does not flow from an included build up into the including root).

**Consequence for release gating:** the actual product release gate (`privchat-application`) passes. The base-distribution standalone aggregate compile does not — but the base may not be intended to build standalone as a runnable product (products are assembled in forks; see `R5-B`/`R5-C`). So this is **a topology-consistency gap, not a broken product release gate.**

Two things still worth fixing/deciding:
1. The mechanism that makes the product work relies on a **cyclic `includeBuild`** (root includes game/assistant; game/assistant include the root). It works today but is fragile and non-obvious.
2. Whether `neton-application` base is expected to build standalone at all (and if so, it needs the same pull-in wiring), and whether `module-system`/`module-infra` should be canonicalized as standalone included builds so the resolution stops depending on the cyclic pull-in.

---

## Problem (base standalone)

`neton-application`'s standalone `:application:compileKotlinMacosArm64` fails because the canonical included business modules (`module-member` / `module-payment` / `module-platform`) cannot resolve `com.netonstream.app:module-system` / `com.netonstream.app:module-infra` — those coordinates are provided only by `neton-application`'s root subprojects, which included builds cannot reach.

Note it IS a documented build entry (`.github/workflows/backend-ci.yml:73` in both repos; README; `scripts/release-gate-smoke.sh` consumes `application.kexe`), so the base's standalone failure is still worth resolving even though the product build is green.

---

## Current topology

- `module-system` / `module-infra`: **in-tree subprojects** of `neton-application` (`include(":module-system")` / `include(":module-infra")` in `settings.gradle.kts`). No standalone `settings.gradle.kts`. Group `com.netonstream.app`, version `1.0.0` (from root `subprojects { }`).
- `module-member` / `module-payment` / `module-platform`: **independent included builds** (`includeBuild("../../Neton/neton-application-module-*")`), each its own Gradle build root with `group = "com.netonstream.app"`.
- `module-member`/`payment`/`platform` `build.gradle.kts` depend on `com.netonstream.app:module-system` (member and one other also on `com.netonstream.app:module-infra`).
- `privchat-application` is a **fork** of `neton-application` with the same `system/infra` embedding, plus extra product modules (`game` / `assistant` / `module-privchat`). Same topology, same latent issue.

Design intent (from `docs/module-template-spec.md` §2.1/§2.2 + `settings.gradle.kts` `R5-B`/`R5-C` comments): `system`/`infra` = internal core modules; `member`/`payment`/`platform` = external canonical modules that evolve as sibling repos; `game`/`assistant` = product modules present only in the fork. So the split is intentional — but the wiring that lets external modules see `system`/`infra` was never completed.

---

## Failure chain

1. Root build `neton-application` `includeBuild`s `module-member` / `payment` / `platform`.
2. Each of those included builds requests `com.netonstream.app:module-system` (+ `module-infra`).
3. Those coordinates are provided **only** by `neton-application`'s own subprojects (`:module-system` / `:module-infra`).
4. Gradle composite dependency substitution flows **one direction**: the including build substitutes deps with included builds' projects; an included build cannot reach back up into the **including (root) build's** subprojects (that would be a cycle: root includes member; member depends on root's subproject).
5. Resolution falls through to an external lookup that doesn't exist → `Could not find com.netonstream.app:module-system:` (empty version) inside `:neton-application-module-member:...CompileKlibraries`.

`module-template-spec.md` §6.3 already prescribes the intended workaround — external modules that need `module-system` should `includeBuild("../neton-application")` — but none of `member`/`payment`/`platform` do this today, and doing so may introduce a cyclic composite (neton-application includes member; member includes neton-application).

---

## CI / workspace layout risk

The `settings.gradle.kts` files use a **canonical `../../Neton/` prefix** for included builds (e.g. `includeBuild("../../Neton/neton-application-module-member")`). This resolves locally **only because the parent directory happens to be named `Neton/`** (`/Users/.../projects/Neton/neton-application` → `../../Neton/...` bounces out and back into `Neton/`).

GitHub Actions `backend-ci.yml` checks the sibling repos out as flat siblings under `$GITHUB_WORKSPACE` (`path: neton-application`, `path: neton`, `path: neton-application-module-member`, ...). `$GITHUB_WORKSPACE` is not named `Neton`, so `../../Neton/...` likely does **not** resolve the same way in CI.

Whether GitHub Actions CI is green cannot be checked from the working environment (`gh` unavailable). But local verification shows `privchat-application`'s aggregate compiles from the on-disk `Neton/` layout, so the product build works at least locally. The CI-vs-local layout question stands for the CI checkout specifically and should be confirmed by someone with CI access — but it is a robustness concern (make paths layout-independent), not evidence of a broken product gate.

---

## Release impact

- **Product release gate (`privchat-application`) aggregate compile: GREEN** (verified locally). Release-gate-smoke can build its `application.kexe`.
- **Base (`neton-application`) standalone aggregate compile: fails** — but base is a distribution template, not necessarily a standalone runnable product.
- STANDARDIZATION-RC module-level validation (compile + tests + docs) is CLOSED and unaffected.
- Fixing this P0 is about topology consistency + removing the fragile cyclic `includeBuild`, not about unblocking a broken product release.

---

## Options (do NOT pick blindly — decision required)

**A. Extract `module-system` / `module-infra` as independent canonical included builds.**
Give them their own `settings.gradle.kts` (standalone build roots), change `neton-application` from `include(":module-system")` to `includeBuild(...)`, and have `member`/`payment`/`platform` (and the app) `includeBuild` them. Most consistent with the "canonical module" model already applied to member/payment/platform. Largest blast radius (touches ~7 build files across repos + the fork).

**B. Add `includeBuild("../neton-application")` to `member`/`payment`/`platform`** (the workaround `module-template-spec` §6.3 hints at).
Lighter, but risks a cyclic composite (root includes member; member includes root) — needs verification that Gradle tolerates it, and it entangles each external module with the whole application build.

**C. Redefine the supported build entry** and remove aggregate `:application` compile from CI / release gate (e.g. build/publish modules independently, consume as artifacts).
Changes the release model; only if A/B are judged not worth it.

---

## Recommended approach (before code)

Do not patch blindly. First decide **canonical module ownership**:
- Are `system`/`infra` product-embedded modules (stay in-tree) or canonical Neton application modules (extract to standalone builds)?
- Should the `privchat-application` fork mirror the exact same topology?
- Is the `../../Neton/` canonical-prefix convention the intended one, and does CI need to adopt a matching checkout layout (or should paths become layout-independent)?

Then merge this with the pre-existing multi-repo backlog: canonical `includeBuild` prefix convention, fork distribution model, reverse-dependency (`module-privchat` → member) wiring, and CI checkout layout.

---

## Acceptance criteria

- `:application:compileKotlinMacosArm64` passes from a clean checkout **without** `publishToMavenLocal`.
- `privchat-application` aggregate compile passes (fork mirrors whatever topology is chosen).
- CI checkout layout does **not** depend on the parent directory being named `Neton/`.
- `includeBuild` paths follow one documented canonical convention (same locally and in CI).
- No cyclic composite dependency.
- `release-gate-smoke.sh` can build/run the expected `application.kexe`.
- The chosen topology is documented in `module-template-spec.md` (single source of truth) and mirrored consistently across `neton-application` + fork.

---

## Constraints for whoever picks this up

- Not part of NETON-1.0-STANDARDIZATION-RC.
- Do not fix with `publishToMavenLocal`.
- Do not fold into HTTP Dispatcher / KSP rework / Kotlin 2.4 / Gradle major upgrade.
- Any change must be synced between `neton-application` and the `privchat-application` fork (separate commits per repo).
