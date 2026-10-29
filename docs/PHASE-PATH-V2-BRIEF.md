# Path screen v2: builder brief (lead: Opus main chat · 2026-10-29)

Read first, in full: `D:\Brain\rules\humopedia-app-build-prompt.md` (the golden method), `D:\Brain\rules\native-app-playbook.md`,
`D:\PAGEL\android\docs\PHASE-BRIEF.md` (builder loop, look in numbers), this repo's `docs\NATIVE-BRIEF.md` and `docs\RULES.md`,
`D:\Brain\rules\colour-theory.md`, `D:\Brain\rules\liquid-glass-navbar.md`. Repo `D:\Humopedia\android`, branch `feat/native-app`
(package `com.mcompany.humo` never changes; never touch keystore/`google-services.json`/secrets; stage files by name; commits show
Boss only, no Co-Authored-By; never push to main; no sub-agents).

## Boss's notes on the A05s, 2026-10-29 11:50 (word for word, they decide)
"That SR-71 is perfect there. Can you make it like we added flying part, real flying kind of vibe. Also that node are trash, that path
is trash, make walkable path, see that reference, we got 70+ references, do better with image. That lesson node is trash, also remove
'Level 2' kind of things, just don't add text to it. Make bigger, cleaner, more tappable, small elements trash. When I go up it is
showing foggy, trash, it is not fog, top part not built kind of vibe: make real working low-poly clouds floating kind of games have,
not blur. Keep a bar on top where we have course selection, numbers etc. Icons: change for better, see all references, we did our own
SVG icons, do that, many formats 5+. App icon is looking trash, the hair is old, not clean, I want a better one."
His screenshots: `C:\Users\Murodjon\.claude\uploads\e34b4f9b-a00d-49ba-afdd-0cb93f36f328\63c172dd-image.jpg` (top: fog),
`27ea0596-image.jpg` (path + SR-71 + nodes), `ea469f98-image.jpg` (home screen, old launcher icon).

## Build (one hero slice, every state)
1. **SR-71 keeps its look** (`res/drawable-nodpi/plane_sr71.webp`) and really flies: a slow lazy loop over the map (bank into turns,
   gentle bob, its ground shadow offset and softer when higher), never over a stone the child is about to tap. Reduce-motion = still.
2. **Walkable path**: stepping stones across the river become a path a child could walk (stones close together like a trail, or a
   path on the bank with a little bridge per unit end). Study refs first: `D:\Humopedia\design\` (77 images),
   `D:\Humopedia\docs\design\2026-10-28-app-redesign\refs\` (+ `PINTEREST.md`, `BOSS-KEYWORDS.md`, `INDEX.md`), round-07-look sprites.
3. **Nodes**: no text on the map at all. Big (≥ 72 dp target, art ≈ 64-80 dp), clean low-poly stones/islands, 3 states clearly
   different without words: done (moss + small check), current (bright, gentle pulse, the one obvious "tap me"), locked (muted,
   under cloud). Tap → glass bottom sheet: level name, lesson count, big "Davom etish" button. Press = 0.97 scale + haptic.
4. **Clouds instead of fog**: low-poly clouds (flat faceted polygons, 2-3 tones, drawn in Compose Canvas or vector) floating slowly
   with parallax over the locked part above the current level; they part a little as you scroll up. No blur, no white haze.
5. **Top bar (glass, reserves its slot, no layout jump)**: course chip "Course 15 ▾" (opens a sheet with course list, stub ok),
   gems count, streak count (plain Uzbek labels in content descriptions, icons only on screen).
6. **Icons**: our own SVG icon set on a 24 dp grid, 2 dp stroke, filled active state. Make **5 distinct sets** (e.g. rounded line,
   duotone, chunky filled, soft 3D clay, geometric) for the 5 tabs (Home · Worlds · Battle · Leaderboard · Me) + gem + streak +
   course; render them as one board PNG `docs/qa/icons-board.png` for Boss to pick; ship the set you rate best in the app now.
7. **Launcher icon**: the new clean Humo (sprout, no old hair) from `D:\30_library\assets\humo-brand\code-soft25d\` (see `rig\`,
   brain card `D:\Brain\rules\two-five-clean.md`), adaptive icon (foreground/background/monochrome) at all densities.

## Look in numbers
Humopedia mint glass kit tokens already in `ui/glass`; text ≥ 4.5:1, marks ≥ 3:1, mint line #1C7E69, never #000; one light top-left;
8-pt spacing tokens; springs 150-300 ms. Low-poly art = flat facets, 2-3 tones per part, soft contact shadow.

## Checks (a machine judges)
Unit + Roborazzi screenshots at 360x800, 390 dp and 1.5x font, dark; emulator walkthrough: scroll to top (clouds visible), tap the
current stone → sheet opens, tap a tab. ONE push per CI cycle; CI must be green. Open every new screenshot yourself before saying done.

## Return (≤ 200 lines)
What changed (file:line), CI run id + result, the screenshot paths, the icon board path, what is not done, and the APK artifact name.
