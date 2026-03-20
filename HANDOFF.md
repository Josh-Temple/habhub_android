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


## 2026-03-20 follow-up: workflow hardening and remaining pinning task

### What changed
1. Added `permissions: contents: read` to `.github/workflows/build-android-debug-apk.yml` so the caller workflow declares the minimum repository scope it needs.
2. Renamed the reusable workflow job to make it explicit that secret validation happens inside the reusable workflow, not in the caller.
3. Added an inline comment in the workflow explaining why `jobs.<job_id>.if` must not reference `secrets.*` for this reusable workflow call.
4. Updated README CI notes with the exact evaluation failure mode and the remaining follow-up for immutable ref pinning.

### Investigation notes
- The failure pattern matched GitHub Actions workflow-evaluation failure: run duration `0s`, no jobs planned, and the reusable-workflow caller previously referenced `secrets.*` inside a job-level `if:`.
- GitHub's context-availability rules allow `secrets` for `jobs.<job_id>.secrets.<secret_id>`, but not for `jobs.<job_id>.if`, which is why the earlier form failed before execution.
- Attempted to resolve the reusable workflow tag (`@v1`) to an immutable commit SHA from this environment, but outbound access to `https://github.com/Josh-Temple/Ingrain.git` returned `CONNECT tunnel failed, response 403`.

### Recommended next task
1. From a network-enabled environment, resolve `Josh-Temple/Ingrain/.github/workflows/reusable-android-debug-apk.yml@v1` to its exact commit SHA and replace the tag ref with that SHA for supply-chain immutability.
