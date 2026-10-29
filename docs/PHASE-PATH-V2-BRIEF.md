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

---
# ROUND 3 (Boss on the A05s, 2026-10-29 13:01 — he played alpha02; alpha03 = run 40838456917 already folds the checker's 10 fixes)
Screenshots: `C:\Users\Murodjon\.claude\uploads\e34b4f9b-a00d-49ba-afdd-0cb93f36f328\` 36be09e1-image.jpg (path), 9ae1f644-image.jpg (current sheet),
5f8e784c-image.jpg (done sheet), 47b19335-image.jpg (scrolled up: cloud wall), 3c02d20e-image.jpg (jet).
Boss's words: "Perfect for v0.1. That SR-71 is flying upside down kind of, not clean: the user looks directly (top-down), it should fly
directly like it is a 2.5D world, not 3D. When flying it starts tilting left or right like it is feeling the manoeuvre. Lovely; also that
fire should be like in the lesson, better colour. The path is good but clouds are even blocking the current lesson: always auto-scroll to
the current lesson. Top bar: the user should know the goal is to continue, no friction. Is that model a PNG, a 3D model or vector — it must
work the same on other phone sizes. Islands look squashed, not clean. Clouds not moving, trash. Show 5-7 lessons above (the story), then a
cloud bank you cannot scroll past — clouds fixed starting low, whole covered above. Audit, do creative small things a child loves, like
that fire and flag in the lesson."

## Do (in this order)
1. **The jet = the lesson's SR-71, top-down vector, flat 2.5D.** Port the SR-71 art from
   `D:\Humopedia\docs\courses\15-coordinate-transformations\lessons\fly-layer.js` (ART.sr71 svg paths, viewBox -30 -60 60 120, nose up,
   real black colours) to a Compose vector/Canvas drawing; delete `plane_sr71.webp`. Heading = direction of travel (nose into the move);
   bank = a small scaleX squash (≤ 18 %) plus a lit/shade swap toward the turn, never a 3D flip. Two afterburner flames like the lesson
   (radial #FFFBE6 → #FFD45C → #FF8A2A, flicker) and a soft smoke trail of fading puffs. Shadow = same silhouette, offset, 18 % teal.
2. **Open on the current lesson**: scroll so the current island sits at THUMB above the tab bar on every open and on returning from a
   lesson; never covered by a cloud. **Current island = one tap starts the lesson** (no sheet; "click and start, no friction"); done
   and locked keep the sheet.
3. **Visible story, then a cloud bank**: the current island + the next 5-7 locked islands are clearly visible (no cloud over them);
   above that a solid low-poly cloud bank that starts low and covers everything above; the scroll stops at the bank (it is the top).
   Clouds drift slowly and visibly (each layer its own speed), puffs gently breathe; they never cover an island below the bank.
4. **Islands not squashed**: taller side face and less flattened top (top ellipse ratio ~0.62, side ≥ 22 % of height), crisp facets.
5. **Small loved things** (pick 2-3, cheap, reduce-motion honoured): the flag waves on the current island; a tiny sparkle on done
   ticks when they come into view; the jet does a small celebratory roll after the child returns from a finished lesson.
6. Answer for Boss in the report: is each map piece vector/code-drawn (scales on every phone) or a bitmap.
Bump to 3.0.0-alpha04 / code 38. Same checks; screenshots must show: current island clear on open, the 5-7 visible locked islands,
the cloud bank as the top, the jet top-down with flames.
7. **Launcher icon = Humo's head only, not the whole body** (Boss, 13:05). Crop/redraw to the face + sprout, filling the adaptive
   safe zone. The face-only source exists: `D:\30_library\assets\humo-brand\app-icon-faceonly\` and `make_icon_faceonly.py` (check it
   uses the new sprout Humo, not the old hair; if old, build the face from `code-soft25d\rig\`). Audit at 48 dp on a dark and a light
   wallpaper.
Note: Boss's notes above came from alpha02. Check each one against alpha03's screenshots
(`D:\Humopedia\android\build-ci\40838456917\`) and fix what is still true.
