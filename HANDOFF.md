# HANDOFF

## Build stabilization and tooling refactor completed

### What changed
1. **Gradle launcher hardening**
   - Added Java 17 fallback logic to `gradlew`.
   - Added `JAVA17_HOME` support and a `mise`-based fallback path for environments where Java 24+ is the default.
   - Added a minimal Windows-side fallback in `gradlew.bat` so Windows users can also opt into `JAVA17_HOME`.

2. **Toolchain pinning**
   - Added `.mise.toml` to pin the repo-local Java toolchain to `17.0.2`.
   - This is meant to prevent Gradle Kotlin DSL startup failures such as `java.lang.IllegalArgumentException: 25.0.1`.

3. **Docs refresh**
   - README now documents the Java 17 expectation, Gradle fallback behavior, and the remaining network-related plugin resolution limitation of this environment.

## Behavior/migration notes
- The wrapper now prefers Java 17 when `JAVA17_HOME` is set or when the standard mise install path exists.
- No application schema or runtime behavior changes were made in this pass; this was a build/tooling stabilization update.

## Testing notes from this environment
- Confirmed the original Gradle startup failure under Java 25:
  - `java.lang.IllegalArgumentException: 25.0.1`
- After the wrapper/toolchain changes, that Java-version parsing failure is avoided.
- Build verification is still blocked in this environment by network/plugin-resolution restrictions when fetching Android Gradle Plugin artifacts from Google Maven.

## Intentionally deferred
- Upgrading AGP / Gradle versions wholesale was intentionally deferred because the current blocker is environment/toolchain related, and a version bump would be higher-risk without full CI coverage.
- Application-layer refactoring was not performed in this pass because the blocking issue was in the build bootstrap path.

## Recommended next tasks
1. Run `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` in Android Studio or CI with JDK 17 and normal Google Maven access.
2. If CI still uses a newer system JDK, export `JAVA17_HOME` explicitly before invoking `gradlew`.
3. Consider adding CI preflight output (`java -version`) so future toolchain drift is visible immediately.

## 2026-03-20 workflow fix: signed debug reusable workflow evaluation

### What changed
1. Removed the job-level `if:` from `.github/workflows/build-android-debug-apk.yml` for `signed-debug-apk`.
2. Kept the existing reusable workflow invocation, `with:` inputs, and `secrets:` forwarding unchanged.
3. Updated README CI notes so they reflect that signing readiness is now enforced inside the reusable workflow rather than the caller workflow.

### Why this matters
- GitHub Actions does not allow using `secrets.*` in that job-level `if:` during workflow evaluation for this case, so the workflow could fail before jobs were even planned.
- By always calling the reusable workflow, parse/evaluation succeeds and the reusable workflow can perform its own signing readiness checks.

### Recommended next check
1. Re-run the `Android Validation` workflow in GitHub Actions and confirm that the workflow now evaluates successfully.
2. If signing secrets are missing, expect the reusable workflow to fail in its own validation path because `allow_ephemeral_signing` remains `false`.

## 2026-03-20 workflow fix: reusable workflow ref resolution

### What changed
1. Replaced `Josh-Temple/Ingrain/.github/workflows/reusable-android-debug-apk.yml@v1` with the current valid commit SHA `56019b0963e5970fba0f73e6591c9c8f2fb11cff`.
2. Left the caller-side `with:` inputs, `secrets:` forwarding, and `allow_ephemeral_signing: false` behavior unchanged.
3. Updated README CI notes to explain that the previous failure after merge was caused by an invalid reusable workflow ref, not by the secrets handoff itself.

### Root cause confirmed
- GitHub Actions run `#59` on `main` failed with: `failed to fetch workflow: reference to workflow should be either a valid branch, tag, or commit`.
- The referenced repository `Josh-Temple/Ingrain` exposes `main`, but the reusable workflow commit history for `.github/workflows/reusable-android-debug-apk.yml` shows commit `56019b0963e5970fba0f73e6591c9c8f2fb11cff` and no `v1` ref was available at investigation time.

### Recommended follow-up
1. Re-run `Android Validation` to confirm the workflow now gets past reusable-workflow resolution.
2. If you want a stable semantic ref later, create a `v1` tag in `Josh-Temple/Ingrain` that points to the intended reusable workflow commit, then update this repo back to `@v1`.

