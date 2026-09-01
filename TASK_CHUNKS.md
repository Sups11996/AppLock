# Build Plan — Work in Chunks

**Rules for the coding agent (read this first):**

1. Read `PROJECT_SPEC.md` before starting any chunk.
2. Do **ONE chunk at a time**, in order. Do not start the next chunk until
   the user has reviewed and explicitly approved the current one.
3. At the end of every chunk, STOP and output:
   - What you built/changed (file list)
   - How to manually test it
   - Any decision you made that the spec didn't cover
   - Then literally ask: "Ready for the next chunk?"
4. Do not silently change the tech stack, min SDK, or architecture decided
   in `PROJECT_SPEC.md`. If you think a change is needed, ask first.
5. Keep each chunk's code compilable and runnable on its own, even if a
   later chunk will hook it up to something bigger — i.e. no half-wired
   dead code that can't be manually verified yet.

---

## Chunk 0 — Project Skeleton

- Create the Android Studio project (Kotlin, Jetpack Compose, minSdk 26).
- Set up `build.gradle.kts` (project + app level) with dependencies for:
  Compose, DataStore, `androidx.biometric`, Coroutines.
- Basic package structure, e.g.:
  - `ui/` (Compose screens)
  - `data/` (storage/repository)
  - `service/` (foreground detection service)
  - `auth/` (biometric wrapper)
- Empty `MainActivity` that just renders "AppLock" text via Compose to
  confirm the project builds and runs.
- Set up the app-wide dark theme: define a `darkColorScheme(...)` and wrap
  the app root in it via `MaterialTheme`. No light theme, no
  system-theme-follow logic — always dark.
- **Test:** app builds and launches on a device/emulator showing the
  placeholder text rendered in the dark theme (dark background, readable
  light text).

## Chunk 1 — Installed Apps Manager

- Query installed apps via `PackageManager`.
- Filter out apps the user can't meaningfully lock (system apps with no
  launcher intent) — show only apps that appear in the launcher.
- Build a data class: package name, label, icon.
- Display as a simple Compose list (icon + name), no toggles yet.
- While the app list is being fetched (this runs on a background
  coroutine since querying `PackageManager` for all apps can take a
  moment), show a skeleton screen: several placeholder rows with a
  shimmering circle (icon) + shimmering bar (label) in place of real
  content. Swap to the real list once loading completes.
- **Test:** on launch, briefly see skeleton placeholder rows, then they're
  replaced by the real installed-apps list with correct icons/names. (If
  loading is too fast to see on your device, temporarily add a short
  artificial delay to confirm the skeleton renders correctly, then remove
  the delay.)

## Chunk 2 — Local Storage for Locked Apps

- Set up DataStore (Preferences) to store: set of package names that are
  locked (ON).
- Repository layer: `getLockedApps()`, `setAppLocked(packageName, Boolean)`.
- No UI change yet — just a way to read/write this from code, verified
  with a temporary test button or log output.
- **Test:** toggling a value in code persists across app restart.

## Chunk 3 — App Selection UI (Toggles)

- Wire Chunk 1's list to Chunk 2's storage: each row gets a Switch/Toggle
  reflecting and updating locked state.
- **Test:** toggle an app ON, kill and reopen the AppLock app, confirm the
  toggle is still ON.

## Chunk 4 — Usage Access Permission Flow

- Add UI/flow to check if `PACKAGE_USAGE_STATS` is granted.
- If not granted, show a prompt/button that deep-links to
  `Settings.ACTION_USAGE_ACCESS_SETTINGS`.
- Detect grant status when the user returns to the app.
- **Test:** fresh install shows the permission prompt; after granting in
  Settings and returning, the app recognizes it's granted.

## Chunk 5 — Foreground Detection Service (skeleton)

- Create a foreground `Service` with a persistent notification.
- Inside it, a coroutine loop polling `UsageStatsManager` (start with a
  ~500ms interval) that logs the current foreground app's package name.
- Start/stop this service from `MainActivity` (temporary manual
  start/stop button is fine for now).
- **Test:** logs show the correct foreground package name as you switch
  between apps on the device.

## Chunk 6 — Lock Detection Logic

- Connect Chunk 5's foreground polling to Chunk 2's locked-apps list.
- When a locked app enters the foreground AND is not currently in an
  "unlocked session," trigger an event (for now, just log
  "SHOULD_LOCK: <package>").
- **Test:** opening a locked app logs the trigger; opening a non-locked
  app does not.

## Chunk 7 — AppLock Screen

- Build a full-screen Compose Activity that can be launched on top of
  whatever is in the foreground, triggered by Chunk 6's event.
- No biometric logic yet — just a static lock screen UI with a "would
  unlock here" placeholder button.
- Tune launch flags/timing so it appears as fast as possible after
  detection (minimize the flash-of-underlying-app gap).
- **Test:** opening a locked app shows this screen over it, blocking the
  content.

## Chunk 8 — Biometric Authentication

- Integrate `BiometricPrompt` into the AppLock screen.
- Handle success, failure, and error/cancel callbacks distinctly.
- On success: dismiss the AppLock screen and reveal the underlying app.
- On failure/cancel: keep the AppLock screen up (don't leak content).
- **Test:** correct fingerprint/face dismisses the lock; failed/cancelled
  attempt keeps it up.

## Chunk 9 — Unlock Session Management

- Define and implement the re-lock rule (default proposal: stay unlocked
  while actively in the app; re-lock if backgrounded longer than N
  seconds, or on screen-off). Confirm the exact rule with the user before
  finalizing.
- **Test:** quick app-switch back and forth doesn't re-trigger the lock
  screen; leaving the app for longer than the grace period does.

## Chunk 10 — End-to-End Polish

- Boot receiver: restart the foreground service after device reboot (if
  desired).
- Persistent notification content/UX cleanup for the foreground service.
- Empty states, first-run experience, minor UI polish.
- **Test:** full flow from the spec's "Definition of done for MVP" works
  after a device reboot.

## Chunk 11 — Packaging for Friends

- Generate a signed release APK.
- Write a short 1-page install/setup guide (sideload steps + granting
  Usage Access + biometric setup) to hand to friends.
- **Test:** install on a second device from scratch using only the guide.

---

**Current status:** Not started. Begin with Chunk 0.
