# CFB Scoreboard Overlay — native Android TV app

A real overlay: press a dedicated remote button from inside *any* app
(a streaming app, a live TV input, anything) and the NCAA football
scoreboard slides in on top, fully interactive (D-pad scrolling works),
press the same button again to close it. No timer, no auto-dismiss.

This solves it two proven Android mechanisms most apps never need to combine:

1. An **Accessibility Service** requesting `filterKeyEvents`, which gets
   raw remote-button events regardless of which app is in the foreground.
   This is the same mechanism Projectivy Launcher itself uses for its own
   remote-shortcut menu.
2. A `TYPE_ACCESSIBILITY_OVERLAY` window (reserved for accessibility
   services) containing a plain WebView loaded with your existing
   self-hosted scoreboard page — no `SYSTEM_ALERT_WINDOW` permission dance
   needed, unlike a normal app trying to draw over others.

## What you need to do (I can't do this part — no Android build
   toolchain in my environment, and it needs your actual TV to test)

### Option A — Build via GitHub Actions (recommended if your PC is low on RAM)

This compiles the app on GitHub's servers instead of your own machine —
your PC just needs Git, nothing else.

1. **Install Git for Windows** if you don't have it (git-scm.com).
2. **Create a new repo on GitHub** (github.com/new) — private is fine,
   public is fine too, doesn't matter for this.
3. **Push this project to it.** Open PowerShell, `cd` into this unzipped
   `CFBOverlay` folder, then:
   ```
   git init
   git add .
   git commit -m "Initial CFB overlay app"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo-name>.git
   git push -u origin main
   ```
   (GitHub will prompt you to sign in the first time you push.)
4. **Watch it build.** On the repo's page on github.com, click the
   "Actions" tab — you'll see a "Build APK" workflow run start
   automatically after the push. It takes a couple minutes.
5. **Download the APK.** Once the run shows a green checkmark, click into
   it, scroll to "Artifacts," and download `CFBOverlay-debug-apk` — it's
   a zip containing `app-debug.apk`.
6. **Install it on the TV** exactly like Fully Kiosk and Tasker:
   ```
   .\adb install "C:\path\to\app-debug.apk"
   ```

Any time you change the config values in
`RemoteKeyAccessibilityService.kt` (URL, keycode, width), just commit and
push again — a new build kicks off automatically, no local rebuild step.

### Option B — Build locally via command line (Android Studio or plain SDK tools)

1. **Open this folder** as a project in Android Studio (File > Open,
   select the `CFBOverlay` folder — the one containing `settings.gradle.kts`).
   Let it sync Gradle; it'll download what it needs automatically.
2. **Check the config values** at the top of
   `app/src/main/java/com/mkfm/cfboverlay/RemoteKeyAccessibilityService.kt`:
   - `overlayUrl` — already set to `http://192.168.1.135:8097/cfb-scoreboard-overlay.html`
   - `toggleKeyCode` — already set to `KEYCODE_TV_SATELLITE_SERVICE`
   - `overlayWidthPx` — currently `480`, adjust to taste
3. **Build and install directly to the TV.** Since you've already got
   `adb connect 192.168.1.113:5555` working from earlier in this project,
   Android Studio should detect the TV as a run target automatically (it
   shows up in the device dropdown next to the Run button, the same way
   any adb-connected device does). Just hit Run.
   - Alternatively: Build > Build Bundle(s)/APK(s) > Build APK(s), then
     `.\adb install` the resulting APK exactly like you did for Fully
     Kiosk and Tasker.
4. **Enable the Accessibility Service once.** Open the app on the TV —
   it just shows a button. Tap it to jump to Accessibility Settings, find
   "CFB Scoreboard Overlay" in the list, and turn it on.
   - Faster alternative via ADB, if navigating that settings menu with a
     remote is annoying:
     ```
     .\adb shell settings put secure enabled_accessibility_services com.mkfm.cfboverlay/.RemoteKeyAccessibilityService
     .\adb shell settings put secure accessibility_enabled 1
     ```
5. **Test it.** Open any app (or just sit on the home screen), press the
   button bound to `KEYCODE_TV_SATELLITE_SERVICE`, and the scoreboard
   should slide in on top. Press it again to close.

## Realistic expectations

This is genuinely new code, tested nowhere but here. Things that may
need iteration once you actually run it on your hardware:
- **Overlay z-order / layering** — some OEM skins handle
  `TYPE_ACCESSIBILITY_OVERLAY` slightly differently; if it's drawing
  *behind* certain full-screen video apps, the window flags may need
  tuning (this is exactly the kind of thing that's unpredictable without
  testing on the real device).
- **Whether the WebView actually grabs D-pad focus cleanly** the first
  time — `requestFocus()` usually works but some launchers/video apps
  fight for input focus aggressively.
- **KEYCODE_TV_SATELLITE_SERVICE actually reaching onKeyEvent** — should
  work per Button Mapper's own capture of it, but worth confirming with a
  quick Log.d() + `adb logcat` check if the first test doesn't respond.

None of these are exotic problems — they're the normal first-run
debugging any new Android app goes through. Report back what you see and
we'll iterate.

## Removing the debug button from the web page

Once this native trigger is confirmed working, the amber "TOGGLE PANEL"
debug button and the long-press-Enter/D-pad-scroll keyboard handling in
`cfb-scoreboard-overlay.html` are no longer the primary way this gets
triggered — but there's no harm leaving them in place as a manual
fallback for testing directly in a browser.
