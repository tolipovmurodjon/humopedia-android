# Humopedia native app: shared brief (phase N0 = the shell + the Path)

Boss, 2026-10-28: "let's build first the native app with that liquid glass nav bar we have; learn all mobile app
building and design skills and use them; keep all creative ideas and the North Star." North Star: one child uses
Humopedia 15 minutes, gets better, wants to come back.

## Read first (in this order)
1. `D:\Brain\rules\native-app-playbook.md` (the method, skills, checks, phase loop) and `D:\Brain\rules\liquid-glass-navbar.md` (the bar, numbers).
2. PAGEL's working app, the source to COPY (not rewrite): `D:\PAGEL\android\` — `docs\RULES.md`, `docs\PHASE-BRIEF.md`,
   `app\src\main\java\org\pagel\ai\ui\glass\*`, `ui\shell\AppShell.kt`, `crash\`, toolchain in `gradle\libs.versions.toml`,
   `build.gradle.kts` files, `.github\workflows\build.yml`, `.github\scripts\walkthrough.sh`.
3. Humopedia vision + ideas: `D:\Brain\humopedia\humopedia-2-vision.md`, `D:\Brain\humopedia\idea-bank-2026-10-28.md`,
   `D:\Brain\humopedia\comeback-loops.md`, `D:\Brain\rules\soft-toy-low-poly-and-gift-collectibles.md`.
4. Load skills: android-dev, android-skills (navigation-3, jetpack-compose-performance, edge-to-edge, testing-setup, agp-9-upgrade), android-testing, ui-ux-pro-max, apple-design.

## Hard limits (never)
- Package `com.mcompany.humo` never changes. Never read/print/commit `*.jks`, `keystore.properties`, `google-services.json`,
  `serviceAccount*.json`, `local.properties`, `.env`, anything `*secret*|*token*|*password*|*credential*`.
- Work on branch `feat/native-app` from `main`. Never push to main, never force-push, never `git add -A` (stage by name).
  Commit messages: Conventional Commits, author Boss only, no Co-Authored-By / "Generated with" lines.
- No Play release, no `publishBundle`, no change to `release.yml` production behaviour. versionCode only goes up (next 35, versionName 3.0.0-alpha01).
- Uzbek UI strings address the learner as **siz**. Lesson content is English (Boss: machine-translated Uzbek reads AI-written).
- Do not spawn agents. Return ≤ 200 lines.

## Phase N0: build
1. **Toolchain + shell from PAGEL:** Kotlin 2.4.x, AGP 9.3.x, compileSdk 37, Compose BOM, Navigation 3, Hilt + KSP, backdrop 2.0.1,
   Roborazzi. The TWA launcher is replaced by a native `MainActivity` on this branch (keep the TWA code in git history only).
   Crash-to-file + send-on-next-start (adapt PAGEL `crash\`; no server endpoint yet → keep the file locally, TODO noted).
2. **Liquid glass bottom bar** exactly per the card (64 dp capsule, liquid drop 1→1.2, SegmentTick haptic, ink 72 % labels,
   Lucide icons). 4 tabs: **Yoʻl** (path, start tab) · **Olam** (world) · **Doʻstlar** · **Siz**. Humopedia mint tokens
   (`#3DC98C` brand, `#1C7E69` ink-mint for lines/selected, ink `#0F1630`), aurora page with 4 colour clouds (two under the bar).
3. **Yoʻl tab = the river path that scrolls UP** (the core of round 7): course 15 "Coordinate Transformations"
   (14 levels; names from `D:\Humopedia\docs\curriculum-v0.5\curriculum-v0.5.json`, course id `trans`, English titles).
   Each level = one stepping stone sprite on a winding river, bottom = level 1, the list starts scrolled to the current stone
   at thumb height; done / current / locked sprites from
   `D:\Humopedia\docs\design\2026-10-28-app-redesign\round-07-models\sprites\` (stone-done / -current / -locked, 256 px);
   locked stones fade into soft clouds above. Tap the current stone = 1 tap → a lesson screen (placeholder: title +
   "Lesson 1.1 · Translating Points" + back) — the lesson player is the next phase. No "Oʻynash" text button.
   Progress is local (Room or DataStore, off the first frame); current = level 3 for the demo (Boss's likely start).
4. **Olam / Doʻstlar / Siz:** clean glass placeholder screens with one line each saying what comes (world, friends, profile shelf of 6 souvenirs).
5. **Feel:** every press 0.97 in 120 ms; no bounce/elastic easing anywhere; scroll edge fades under the glass; 60 fps scrolling
   (no allocations in draw, sprites decoded once).

## Checks (the maker never checks its own work)
- `./gradlew testDebugUnitTest` + Roborazzi screenshots (390 dp, 1.5× font, dark) of all 4 tabs and the Path scrolled.
- CI on the branch (copy PAGEL's build.yml + walkthrough: launch, every tab, scroll the path, tap the current stone, back, rotate,
  big font); one push per CI cycle; open every screenshot yourself before reporting.
- Build a debug (or test-signed) APK artifact in CI; the lead installs it on the A05s / App Distribution.

## Done when
CI green on `feat/native-app`; screenshots show the glass bar bending the aurora + path; walkthrough passes; APK artifact exists.
Return: CI run URL + status, test counts, screenshot paths, what is not done, the next phase proposal (lesson player for L1.1/L1.2,
ported from `D:\Humopedia\docs\courses\15-coordinate-transformations\lessons\`).
