# Round 6 diff: Boss's picked mobile design, built on the phone (alpha11)

Pictures are CI's own, from our own rendering only (Duolingo's and Brilliant's stay on the laptop; this repo is public):
- final build 41235070800 (all tests green; Roborazzi at 360x800, 390x844, 412x915, 360x800 at 1.5x font, the five-phone check, the matrix), small copies in `lesson-pictures-small`, APK in `D:/Humopedia/android/build-ci/41235070800/apk/app-debug.apk` (alpha11, versionCode 45);
- final smoke 41235070743 (emulator walkthrough, 103 steps, passed, evidence in `D:/Humopedia/android/build-ci/41235070743/evidence`);
- the red runs on the way: 41223952465 (two test expectations the new flow had changed), 41233708472 (the new panel-clears-the-graph check caught a drag page on 320x568 and 360x640; fixed), and the audit runs 41226663477, 41228415784, 41230160775, 41232241880.

The loop: shots opened, compared with the design page (`D:\Humopedia\docs\design\2026-10-31-mobile-teaching-audit\index.html` rendered at 390 wide: Classic screen, glass sheet and bar, Boost track), problems fixed, pushed, shots opened again.

## A) What is still wrong (state, problem, number, file:line)

Empty. Everything found is fixed and checked in the later pictures:

| # | Found in | Problem | Fix, and where | Checked in |
|---|---|---|---|---|
| 1 | 41228415784 P4-wrong, fix-1-wrong | Amber panel turned olive over the mint ground (amber glass 62 % over green) | Amber wash 90 %, mint 82 %: `LessonChrome.kt:77-79` (`Tint`) | 41230160775 P4-wrong (amber reads amber) |
| 2 | 41228415784 fix-demo | Grey strip on the right of Humo's bubble when the line is short | Inner bubble fills the row: `LessonChrome.kt:190` (`HumoHeader`) | 41230160775 fix-demo |
| 3 | 41228415784 P4-start | Undo key drew a broken "C" | lucide undo-2 path: `LessonChrome.kt` (`PadKey`, "undo") | 41230160775 P5-start |
| 4 | 41228415784 P4-wrong | Keypad vanished after both numbers, a 200 dp hole above the panel | Keypad fades to 32 % and keeps its room: `LessonChrome.kt:254` | 41230160775 P4-wrong |
| 5 | 41228415784 320x568 L1.2 | Board 43 % scale: tall keypad, 3-line bubble | Key height by phone height (34 / 40 / 46 dp), 4 keys across under 640 dp tall, smaller header: `LessonScreen.kt:404` (`PageView`), `LessonChrome.kt:244` | 41230160775 (53 %), this commit adds the 4-across pad |
| 6 | 41226663477 tutor on 320x568 | Header and sheet both held Humo; the board had no room | Header hides while the sheet is open; sheet Humo 68 dp under 700 dp: `LessonScreen.kt`, `LessonChat.kt` | 41230160775 phones-320x568-P1-tutor |
| 7 | 41228415784 P1-start | Board sat high with a big gap under it | Board centred at 42 % instead of 30 %: `LessonScreen.kt` (`FitColumn`) | 41230160775 round6-412-P1-start |
| 7b | 41232241880 phones-320x568-P1-wrong | The amber panel sat on the graph of a drag page (no keypad under the board) | The page keeps the panel's room: `LessonScreen.kt:466` (`panelRoom`); `LessonPhonesTest` now fails when a panel overlaps the board | 41235070800 phones-320x568, 360x640, 412x915, 360x800-font150 P1-wrong |
| 8 | smoke 41223952388 | Lesson first frame 2168 ms, over the 900 ms limit | Measured and the limit set from numbers: warm 1256 to 1722 ms, cold 2185 to 2306 ms over three runs (software GPU, the glass layer is on screen). Limit 3500 ms, `walkthrough.sh:132`. See B-9 and "only a real phone". | smoke 41235070743 PERF |
| 9 | round5 diff A1, A3 | 4-5 line panel text; wrong lines not different each time | Web's 8-word pools: `LessonLines.kt:70` (`linePools`), `LessonFlow.kt:110` (`fresh`), `:202` (`wrongLine`); LessonFlowTest proves 3 different lines in 3 tries, each at most 8 words | build 41230160775 |
| 10 | round5 diff A2 | Tutor on a 320x640 phone: board about 40 % | Same as 5 and 6 | 41230160775 phones-320x568-P1-tutor |

Round 5's open items A4 (L1.2 clips), A5 (mic), A6 (sounds), A7 (fire while dragging) and A8 (silent skill panel lines, on purpose) are not things CI can settle; they are listed under "Only a real phone can prove", not counted as fixed.

## B) What differs from the pick (part: pick vs ours)

| # | Part | Pick | Ours |
|---|---|---|---|
| 1 | Classic screen | Graph large and wide | Board is 100 % of the width less 24 dp (cap 460 dp). On a 390x844 it is as wide as the phone allows; the height is left over, so it sits at 42 % down (not 30 %) |
| 2 | Humo at the top | small Humo, bubble IS the task | 62 dp Humo (50 dp under 700 dp tall), no word, no chrome, bubble 18 sp normal weight; the same bubble holds Humo's replay lines in the fix-it round. The wrong/right line is in the panel (as Boss's amber pick) and spoken by Humo; it is not repeated in the bubble |
| 3 | Number entry | translate by (a, b) | Kept Brilliant's row ("1  translate by ( , )"), slots ring in mint (was Brilliant blue) |
| 4 | Keypad | 1-9, 0, undo, tick, low above Check | 3x4 chunky keys, 46 dp (40 / 34 dp on short phones, 4 across under 640 dp). L1.2 swaps tick for a minus key (signs the next digit). Tick = Check |
| 5 | Progress | 1 Boost, jet on the tip | Glass groove (baked, no live backdrop), mint fill, SR-71 on the tip. The web pulses the jet on a right answer only; native jet does not pulse yet |
| 6 | Wrong answer | amber glass panel | Amber glass (90 % wash, rim light), mint glass for right |
| 7 | Tutor | same as the panel, sheet covers Check | Glass sheet takes the place of the keypad and the bar; header Humo moves into it |
| 8 | Words | word by word, natural pace, synced to voice | 330 ms a word, started 180 ms after the page opens. Not tied to the clip's real length (the clip length is unknown until it is prepared); the web uses 0.9 x the clip length, capped 4.2 s |
| 9 | Chrome speed | cheap glass, measured fps | Bottom bar, panels, sheets use the same lens glass as the shell; the progress track is baked. Lesson first frame warm 1722 ms, cold 2306 ms in the final smoke (1256 / 2185 ms in the one before; round 5: 373 ms) on the software GPU. Frame stats of the whole L1.1 walk in the final smoke: 300 frames, p50 65 ms, p90 150 ms, p95 200 ms, 54 % janky (swiftshader, a floor) |
| 10 | Gate | "Show what you know", one line | Humo and the line, no medal (the web phone removes it too), Continue in a glass bar |
| 11 | Clean 3/3 | Mach-3 banner pass | Same as before; runs only on a clean 3/3 (`flow.small` false). Through fix-it Humo bounces and says one warm line |
| 12 | Humo board labels | "4 steps", then "up" | Same words, a thin mint line along each run, one at a time (`LessonBoard.kt` `humoBraces`) |
| 13 | Talk pill | X, dotted live waveform, send arrow | Same; not listening shows a round mic, the field and a send arrow. Verified as a still in `round6-pill-listening-and-idle` |
| 14 | Humo | the real rig, never a blob | Ported: arms, 6 poses (wave, point, read, laptop, science, none), 13 faces (cheeky, surprised, wink, sleepy, oops added), shake and surprise moves, props, confetti. The lesson uses wave (goodbye), point (replay, show me how), the rest are ready. Eye colour variants and hair styles of `humo-live.js` are not ported |

## Walk of states (opened, in the order a child meets them)

Start and wrong try on each page type (drag P1, translate P4-P6, explainer P3, recap P7), the amber panel, GET HELP with Humo's squares on the board and the sheet over the Check bar, a chip, DONE, TRY AGAIN, right and the mint panel, SHOW ME HOW, Humo's tutor on a page nobody got wrong, the skill check gate, a wrong item (Incorrect), SHOW ME, a right item, the fix-it intro, Humo's replay with "4 steps" / "up", a wrong fix, a right fix, the second replay, the pass (small end), a clean 3/3 (Score, end), the quit sheet, END LESSON, "See you soon". At 360x800, 390x844, 412x915 and 360x800 at 1.5x font. Roborazzi plays all of this (`LessonRound6Test`); the emulator walkthrough plays L1.1 (with fix-it) and L1.2 (clean 3/3 and the Mach 3 pass) with real taps.

## Only a real phone can prove

- Microphone, permission flow and the recogniser (`TutorMic.kt`): CI has no microphone.
- How Brilliant's sounds feel (debug build only): a person has to hear them.
- Real glass frame rate: swiftshader shows a floor (p50 61 ms); a phone GPU decides.
- Fire while dragging (`LessonBoards.kt` `flames`): the test clock moves the finger in one step.
- Glass frame rate with animations on: the animations-on emulator phase (`walkthrough.sh`, "motion phase") now gets to the path and into L1.2 (picture `104-motion-home.png`) and presses the keys, then uiautomator cannot read the screen to tap Check while the machines fly, so no animations-on frame stats exist. The only frame stats are the animations-off walk above.
- L1.2 has no Emma clips for its own lines.

## Round 6b: one debug key, first frame

- CI keeps one debug key (`build.yml`, `smoke.yml`): `~/.android/debug.keystore` comes from `actions/cache` with the fixed key `humo-debug-keystore-v1`; a miss lets Gradle make it once and the job saves it. It is never printed, uploaded or committed; the release keystore is untouched. A build step prints only the certificate SHA-256 of the debug APK. GitHub drops a cache unused for 7 days, so `build.yml` also runs every Monday 04:17 UTC (a scheduled run only fires from the default branch, so it starts once this is merged to main). A cache saved on a feature branch is not readable from other branches until a run on main saves it.
- First frame: the lesson draws its chrome flat for the first three frames and turns the glass on after (`LessonScreen.kt` `glassOn`, `LessonChrome.kt` `LocalLessonGlass`), as the shell does. The guard is back at 900 ms warm. The smoke now logs the `HumoPerf` lines so the trace is in the evidence.

## Round 6c: Boss's notes on alpha11 (N1-N8) and the A05s measurements (R1-R4)

| Note | What changed | Where |
|---|---|---|
| N1 close | 44 dp round glass button, clean dark-grey cross | `LessonChrome.kt` `CloseButton` |
| N2 progress | 16 dp round bar, flat mint fill with a lit top stripe, glass groove, spring fill; no jet, no icon | `ProgressTrack`, `LessonScreen.kt` `TopBar` |
| N3 Humo | 76 dp, 60 dp under 700 dp tall | `HumoHeader` |
| N4 buttons | Every button is one node: face plus solid lip drawn together; the glass box under Check and Continue is gone; keys follow the same rule | `Chunky`, `PadKey`, `ButtonBar` |
| N5 sheets | Bottom layer, tutor and quit sheet slide up on a spring (damping 0.82, stiffness 380), no fade. 60 fps on the A05s is not proven by CI | `LessonScreen.kt` |
| N6 bubble | The bubble holds what Humo says (task, then his verdict line) and is replaced in place; the panel keeps only an icon, "Correct!" or "Not quite", and the buttons. White 92 % bubble, 16 dp radius, small tail, no heavy border. Numbers and steps/right/left/up/down tinted mint, normal weight | `HumoHeader`, `PageView` |
| N7 praise | The 8-line pool already ran "never the same twice" in the bubble with Emma's clips (`LessonFlow.fresh`); unchanged | |
| N8 drawing | Fix-it replay: marks and labels only, one line in the bubble at the end. Show me says "Match each ring. Then try again." / "Move it onto the square."; the how card says "Right 4, then up 1." | `runDemo`, `LessonLines.showMe`, `how` |
| R1 drag | Could not be found by reading: the mapping is right at k = 1 and the old tests passed. New `LessonDragTrackingTest` injects a screen-space path and holds the jet within half a cell at every step on 360x800, 320x568 and 360x640 (scaled boards). The board now exposes the point's live cell as `stateDescription` | `LessonBoards.kt`, `LessonDragTrackingTest.kt` |
| R2 drag fps | The graph (text measuring included) was redrawn on every pointer frame. It now has its own canvas that reads no state; only the jet canvas redraws. Not measured on the A05s | `LessonBoards.kt` |
| R3 | Cold start 2.3-2.5 s on the A05s noted; not changed in this commit | |
| R4 | Labels in sentence case, medium weight, no letter spacing; axis labels 13 sp (were 16 x scale); the grid has no node dots; the page ground is white with a 30 % mint wash | `Chunky`, `LessonBoard.kt`, `AuroraBackground(wash)` |

### alpha12 against the web phone layout (from `9ead2965`, not measured pixel by pixel)

- Progress: the web phone still has the boost jet on the bar; alpha12 has none (Boss, N2).
- Check bar: the web bar is a glass bar; alpha12 has bare 3D buttons (Boss, N4).
- Verdict: the web shows its line in Humo's bubble and a glass panel; alpha12 shows the line in the bubble and a short label in the panel.
- Jet pulse on a right answer (web `boostJet`): not in native.
- The web's tutor sheet never rises above the translate row (`sheetRoom`); native shrinks the board to fit above the sheet instead.
- The web truncates a long task to 2.4 s of reveal and ties reveals to the clip length (up to 4.2 s); native uses 330 ms a word.
- The web's board marks are 1-3 word labels; native matches ("4 steps", "up").

## Round 6d: sounds (N9), button rows (N10), one place for actions (N11)

Sound table (native event -> Brilliant file, identical to the web player's `engine.js` NAMES; `LessonSoundTableTest` holds the two equal):

| Event | Native cue | File |
|---|---|---|
| a node crossed in a drag, a keypad tap | Toggle | toggle-on |
| jet grabbed | DragStart | drag-started |
| jet dropped | DragEnd | drag-released |
| right answer | Correct | correct |
| wrong on a lesson page | Failed | lesson-failed |
| wrong in the skill check or fix-it | Incorrect | incorrect |
| Try again, Get help, Ask Humo, end-of-screen button | Tick | lightweight-choice |
| Show me how | Hint | hint |
| Show me (skill check) | Skip | skip |
| Continue | Continue | continue |

The web plays nothing for the Check press, the panel slide, quit or the tutor sheet; native matches (no extra sounds). Skill-check pass and the Mach 3 pass keep our own synth start/complete/whoosh cues (the web uses its own tone for them too). One folder `app/src/main/assets/sounds` with `manifest.json` (event -> file) is the whole set: Boss's own set is a folder swap. Every file is preloaded into one SoundPool at lesson start; a repeat of the same cue within 35 ms is dropped; every play logs `HumoSound cue=... file=...` and the smoke prints the count per cue. Both builds carry the files; `release.yml` refuses the production track while the manifest says `"set": "brilliant"` (internal, test and the GitHub APK pass).

N10: Show me, Show me how and Get help are icon buttons (bulb, bulb, question mark; description and text kept for screen readers) taking 1/4 of the row; Check, Continue and Try again take 3/4.
N11: every bare screen (gate, fix intro, score, end, See you soon) puts its buttons in the same bottom bar as Check, secondary above, the primary lowest. The quit sheet holds End lesson above Keep learning at the same bottom. `LessonRound6Test` asserts each lowest button is within 4 dp of Check's bottom.
