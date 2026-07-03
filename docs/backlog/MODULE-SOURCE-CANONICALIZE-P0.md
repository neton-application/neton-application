# MODULE-SOURCE-CANONICALIZE-P0

## Status

**OPEN.** Extracted from `NETON-1.0-STANDARDIZATION-RC` (see `docs/NETON-1.0-STANDARDIZATION-RC.md`).
**Not part of STANDARDIZATION-RC completion** — that RC validates module-level compile + tests + docs/API cleanup only.

This is a multi-repo build-topology / included-build / canonical-workspace problem, not a code bug. It needs its own project and an explicit architectural decision before any code/build changes.

> **Do NOT fix this by `publishToMavenLocal`.** Publishing modules to the local Maven repo to make CI green only hides the topology break — the included builds still can't resolve their dependencies within a clean composite. Any fix must work from a fresh checkout with no local publish step.

---

## Problem

`:application:compileKotlinMacosArm64` is an **official CI / release-gate build entry**, but it currently fails because the canonical included business modules (`module-member` / `module-payment` / `module-platform`) cannot resolve `com.netonstream.app:module-system` / `com.netonstream.app:module-infra`.

Evidence it is an official entry:
- `.github/workflows/backend-ci.yml:73` runs `./gradlew :application:compileKotlinMacosArm64` in **both** `neton-application` and `privchat-application`.
- `README.md` documents `:application:compileKotlinMacosArm64` / `:application:linkDebugExecutableMacosArm64` as the build command.
- `scripts/release-gate-smoke.sh` consumes `application/build/bin/macosArm64/releaseExecutable/application.kexe` — the output of this build.

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

**Open question (needs someone with CI access — `gh` not available in the working environment):** is `:application:compileKotlinMacosArm64` currently GREEN or RED in CI? This determines urgency. If it's been red, the release gate has been non-functional; if green, there is a CI-specific layout convention not captured here that must be documented.

---

## Release impact

- `:application` aggregate compile is the official release gate but is currently gated by this topology issue.
- STANDARDIZATION-RC module-level validation (compile + tests + docs) is CLOSED and unaffected.
- A functioning full aggregate release gate requires this P0 resolved.

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
