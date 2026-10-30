# Lesson L1.1 native: builder brief (lead: Opus main chat · 2026-10-29)

Read first, in full: `D:\Brain\rules\humopedia-app-build-prompt.md` (the golden method), `D:\Brain\rules\native-app-playbook.md`,
`D:\Brain\humopedia\brilliant-teaching-is-our-base.md`, `D:\Brain\humopedia\brilliant-plus-one-loved-thing.md`, this repo's
`docs\RULES.md`, `docs\PHASE-PATH-V2-BRIEF.md` (how the last slice was built). Repo `D:\Humopedia\android`, branch `feat/native-app`
(PUBLIC repo: CI is free). Package `com.mcompany.humo` never changes; never touch keystore/google-services/secrets; stage by name;
commits show Boss only; one push per CI cycle; no sub-agents; do not build locally (RAM is low).

## Boss's words (2026-10-29)
"Path can be done later. Main thing: the lesson, same sound, everything as Brilliant, everything we built, use it fully." Asked how,
he chose: "Rebuild natively, same as all code engine, fully working, see it fully, same as brilliant.org, compare; I want the same;
rebuild the engine yourself and do the job." Earlier he loved (rule card above): Brilliant 1:1 + his SR-71 IS the point (follows the
drag; tap the jet then tap a spot and it flies there; Check = Brilliant's own tick/cross; nothing overcomplex; no buzzy engine sound).

## The source of truth (run it, measure it, never guess)
Brilliant's own L1.1 runs on the laptop: serve with `python -m http.server 8158 --bind 127.0.0.1` from
`D:\Humopedia\docs\courses\15-coordinate-transformations\` (check first: it may already run), open
`http://127.0.0.1:8158/lessons/L1.1/brilliant.html` (plain = Brilliant) and `?theme=fly&craft=sr71` (Boss's loved version).
- Page-by-page data (texts, numbers, targets, answers, explanations): `...\lessons\L1.1\lesson.js` (10 pages: P1-P7 + skill check
  SC1-3) and Brilliant's raw `...\trans15\L1.1 Translating Points\*.md`.
- The SR-71 layer to port: `...\lessons\fly-layer.js` (follow, tap-to-fly, flames, smoke, backfire for cars, drift).
- STEP 1 = reference capture: a Playwright script (web repo's node_modules, `NODE_PATH='D:\Humopedia\web\node_modules'`, see
  `...\lessons\shoot.cjs` for the pattern; engine pages need ~10 s to load) that plays every page at 390x844: start state, mid-drag,
  correct, wrong, Why? sheet, skill check, end card. Save to `...\lessons\shots\ref-390\` (git-ignored HQ folder, NOT the android
  repo: it is Brilliant's UI). Also measure and write down per page: grid geometry (cell px, margins), point/handle sizes, snap
  behaviour, every animation's duration + easing (read Brilliant's widget via the engine's shadow DOM / computed styles / WAAPI),
  button and verdict layouts, fonts and colours. Output `docs\qa\l11-spec.md` (numbers only, no Brilliant assets).

## Build (one hero slice, every state)
Compose screen `ui/lesson/` driven by data for all 10 pages: Graph drag (point snaps to nodes, grey start dot stays), the
transformation workspace (tile tray → slots → points glide, pale ghosts + line), Check / Why? / Try again / Start over / Continue,
P3 a-b figure, P7 Play, skill check with "I don't know" and no verdict per item, progress bar + 3 dots, end card. Same texts (clean
English), numbers, order, grading as Brilliant. Then the loved layer: the point IS the SR-71 (reuse `ui/path/Jet.kt` sprites, 16
headings) with follow + tap-then-tap fly + flames + smoke; Check shows the tick/cross, the jet stays.
**Sounds:** Brilliant's mp3s must NOT ship (public repo, their asset). Make our own short clean sounds matching their character
(measure pitch, length and envelope from `...\lessons\assets\sounds\*.mp3`; generate PCM like PAGEL's two-note ding): correct,
incorrect, tick (snap/tile), toggle, lesson complete. Clean, never buzzy.
Wire: the current island's one tap opens this lesson; finishing marks level progress (local store) and returns to the path (the jet roll).

## Checks
Unit tests: grading for every page (correct, each wrong kind), snap maths, tile flow. Roborazzi at 390 dp and 360x800 and 1.5x font:
every page's start + correct + wrong state. **Side-by-side board** `docs\qa\l11-compare.png`: our screenshot next to the reference for
each page (build the board on the laptop from the ref folder + CI artifacts; keep the board local, do not commit Brilliant's
pictures into the public repo). Emulator walkthrough: play L1.1 to the end with one wrong try per graded page. Open every screenshot
yourself. Version 3.0.0-alpha06 / code 40.

## Return (≤ 150 lines)
Run id + result, APK path (`build-ci\<run>\apk\app-debug.apk`), the spec + compare board paths, each page matched or not (a table),
what differs from Brilliant and why, what is not done.

## ROUND 2 (lead, 2026-10-30 evening): finish L1.1 + L1.2 native, matching the web layer Boss loved
Boss: "perfect flying, I love it going up like circling… redo those lessons 1:1 the same as brilliant.org in our own engine, clean code, on the native APK; I need to feel, play and love it."
Start: the 4 uncommitted files in this repo (your previous run) and CI failures 40871338829 / 40871953851 / newest: read each failing log fully, fix every error in one push (one push per CI cycle).
The web reference for the machine layer is `D:\Humopedia\docs\courses\15-coordinate-transformations\lessons\fly-layer.js` (read it whole). Port these behaviours, all of them Boss-approved today:
1. Drag pages: the jet is glued 1:1 to the point (no lag); only the nose eases toward the last ~5 px of motion; no fire/smoke while dragging (focus). Tap jet → tap spot: the nose turns first (≤700 ms), then a calm eased flight (~40+ frames), every other touch ignored during it. After Check it STAYS where it was put (never returns to the start); a rebuild never resets the heading (that was the nose-up bug).
2. Tile pages: the jets WAIT while numbers are tried; Check → they take off, climb (scale 1→1.45 and the shadow drops away), fly a gentle curve, land, THEN the answer is graded (Brilliant's own tick/cross). Skill check Submit: same flight first, then the next item.
3. The dot under a landed jet is hidden; the tick/cross badge stays. No extra rings, pads or yellow marks ("AI slop").
4. Two afterburner flames and two low-poly smoke trails (one per engine), only during flights.
5. SR-71 art = the NEW 64-heading atlas: `D:\Humopedia\docs\courses\15-coordinate-transformations\lessons\assets\sprites\sr71\sr71_atlas64.webp` (8x8, 160 px frames, our own Blender render, OK for the public repo). Frame = round(heading/5.625) mod 64, rotate only the remainder. Replace the old 16 webp frames.
6. Words name the machine: "Move the SR-71 3 steps to the right…", "The SR-71 flies by (2, 3)…" (never "the point"/"a shape").
7. Sound: our own synthesised twin of Brilliant's per-step tick (one soft short tick on every grid node the point snaps to; brain rule `D:\Brain\rules\soft-tick-on-every-move.md`), plus our correct/incorrect; a soft airy whoosh on take-off. Never buzzy.
8. Lesson complete: a 3 s "Mach 3 pass": the jet crosses the screen bottom-left → top-right, a white ring at mid-way, twin smoke, a tap boosts it; never blocks the buttons.
Then L1.2 (negative translations, the B-2 is the machine there; if no B-2 atlas exists yet, use the SR-71 atlas and say so).
Version 3.0.0-alpha07 / versionCode 41. Done when: CI green, walkthrough plays L1.1 to the end with one wrong try per graded page, screenshots of every page start/after-Check, APK path. Return ≤150 lines: Blocked on me / Changed / Found / Not confirmed, run id, APK path.
