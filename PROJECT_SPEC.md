# Personal Android AppLock — Project Spec

## What this app is

A personal-use Android app that lets the user lock individual installed apps
(e.g. WhatsApp, Instagram, Gallery) behind the phone's existing biometric
authentication (fingerprint / face). When a locked app is opened, an AppLock
screen appears first; after successful biometric auth, the underlying app
becomes visible.

This is **not** a commercial product. It is for personal use and a small
group of trusted friends who sideload the APK. There is **no threat model
against sophisticated attackers or malicious third parties** — no
root-detection, no anti-tamper, no obfuscation, no defense against someone
who already controls the device. The only goal is: casual/opportunistic
access (someone picking up the phone) should be blocked. Do not add
enterprise-grade security work that isn't asked for.

## Non-negotiable constraints

- 100% local. No accounts, no cloud, no backend, no network calls at all.
- The app never touches raw biometric data. All biometric auth goes through
  Android's own `BiometricPrompt` API — the app only receives
  success/failure/error callbacks.
- No Play Store distribution assumed. Sideloaded APK is fine. Don't worry
  about Play policy compliance.

## Tech stack (fixed — do not substitute without asking)

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Min SDK:** 26 (Android 8.0)
- **Local storage:** DataStore (Preferences) for the locked-app list and
  simple settings. Use Room only if/when per-app settings grow beyond
  simple key-value.
- **Concurrency:** Kotlin Coroutines (no `Handler.postDelayed`, no RxJava)
- **Biometrics:** `androidx.biometric:biometric` (`BiometricPrompt`)
- **Foreground app detection:** `UsageStatsManager` polled from a
  foreground `Service`, using a coroutine loop with `delay()`. Do NOT use
  `AccessibilityService` unless explicitly asked to switch later.
- **Build system:** Gradle with Kotlin DSL (`build.gradle.kts`)
- **Theme:** Dark mode only (see UI/UX requirements below)
- **Loading UI:** Skeleton/shimmer placeholders for async-loaded screens
  (see UI/UX requirements below) — no bare spinners for the main app list

## UI / UX requirements

- **Theme:** Dark mode only. No light theme, no system-theme switching —
  the app always renders in a dark color scheme. Use Jetpack Compose's
  `MaterialTheme` with a custom dark `ColorScheme` (`darkColorScheme(...)`)
  applied at the root of the app. Don't build a light theme "just in
  case."
- **Loading states:** Wherever data takes a moment to load — the
  installed-apps list on first load, and any other async read from
  storage — show a skeleton screen (shimmering placeholder rows matching
  the shape of the real content: icon-circle + text-bar placeholders)
  instead of a blank screen or a spinner. Once data is ready, replace the
  skeleton with real content. This applies at minimum to the app list
  screen.

## Core features (must-have)

1. List all user-facing installed apps with icon + name.
2. Per-app ON/OFF toggle for AppLock protection, persisted locally.
3. Foreground detection: notice when a protected app comes to the
   foreground.
4. AppLock screen: full-screen overlay/activity shown before the protected
   app's UI is visible to the user.
5. Biometric authentication via `BiometricPrompt` (fingerprint or face,
   whichever the device/OS exposes — app doesn't need to know which).
6. On success: dismiss AppLock screen, let the user proceed into the app.
7. On failure/cancel: keep the AppLock screen up, don't leak the
   underlying app's content.
8. Unlock session management: don't re-prompt every single time the user
   glances at the app within a short window. Re-lock when the app is
   backgrounded past a grace period, or the screen turns off, or the user
   switches to a different app for more than a few seconds. Exact rule can
   be tuned during the "Unlock Session Management" chunk.
9. Settings and locked-app list survive app restarts and device reboots.

## Explicitly out of scope (don't build unless asked)

- Root/tamper/debugger detection
- Preventing app uninstall to bypass the lock
- Defending against other malicious apps abusing Accessibility/Usage APIs
- Code obfuscation / anti-reverse-engineering
- Multi-user / account system
- Cloud backup or sync of settings
- Any analytics or telemetry

## Known technical realities to respect

- `PACKAGE_USAGE_STATS` (Usage Access) cannot be requested via a normal
  runtime permission dialog. The app must deep-link the user to
  `Settings.ACTION_USAGE_ACCESS_SETTINGS` and detect afterward whether it
  was granted.
- The foreground detection service must run as a proper Android
  foreground service (with a persistent notification) to avoid being
  killed by the OS.
- There will be a small timing gap between "protected app becomes
  foreground" and "AppLock screen becomes visible." Minimizing this gap
  (not eliminating it) is a reasonable goal.

## Definition of done for MVP

A user can: install the app → grant Usage Access → pick 2–3 apps to
protect → open one of them → see the AppLock screen appear before the
app's content → authenticate with fingerprint/face → land inside the
app → background the app and reopen it after the grace period → see the
AppLock screen again.
