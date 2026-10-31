# Round 5 diff: the mobile lesson, redone with Duolingo Math's patterns (alpha10)

Pictures: CI run 41205230758 (Roborazzi: `round5/`, `phones/`, `matrix/`), run 41205230622 (emulator walkthrough, 99 steps, passed).
Our own pictures only: Duolingo's stay on the laptop (this repo is public).

## A) What is still wrong (state, problem, number, file)

| # | State | Problem | Number | Where |
|---|---|---|---|---|
| 1 | Wrong answer, 320 dp wide, 1.5x font | The web's long line ("Not all the SR-71s landed on their targets, lowkey close though.") takes 4-5 lines in the panel and the board shrinks to about 55 % to stay above it. Panel type is now 18 sp; the real fix is the web's new 8-word pools. | 5 lines at 20 sp before, 4 at 18 sp | `LessonScreen.kt` `Panel`, text from `L11.nudge`/`LessonFlow.check` |
| 2 | Tutor open on a 320x640 phone | Board is wholly above the sheet and undimmed while Humo draws (gated), but it is small. | about 40 % scale | `LessonScreen.kt` `FitColumn` |
| 3 | Five wrong tries in a row | Not done: "never the same line twice" needs the web's pools (lesson.js still has 8 praise and 2 nudge lines; the wrong lines are 2 fixed strings). The converter and `bundle-voice.cjs` take the pools as soon as they exist. | 0 of 5 different | `LessonFlow.check`, `scripts/convert-lesson.cjs` |
| 4 | L1.2 | No Emma clips for L1.2's own lines (the voice map is L1.1's). L1.2 is silent apart from shared lines. | 0 clips | `LessonL11.kt` `L12`, `LessonVoice.kt` |
| 5 | Talk to Humo (mic) | Intent matching is unit-tested (45 phrasings); the recogniser and the permission flow could not be run (no microphone on the CI emulator). | untested on a phone | `TutorMic.kt`, `LessonChat.kt` |
| 6 | Boss's real sounds | In the debug source set only; nobody has heard them on a phone yet. | 11 files | `app/src/debug/assets/sounds/`, `LessonSounds.kt` |
| 7 | Fire while dragging | The mid-drag Roborazzi frame (`round5/motion-P1-mid-drag.png`) shows the soft smoke but no nozzle fire: the test clock moves the finger in one step, so the jet's speed is near zero at the frame. The code now lights the nozzles while dragging (`LessonBoards.kt` `flames`); a real drag on a phone is the proof. The end pass (`lesson/motion-end-mach3-a.png`) shows fire, smoke and banner. | unproven on a phone | `LessonBoards.kt` |
| 8 | Skill check | Panel lines ("Correct!", "Not quite.") are silent on purpose (no Humo in the skill check). | | `LessonScreen.kt` |

Fixed in this round after Boss's shots (checked in the pictures): overlapping jets (jet length now shrinks with the nearest pair, `jetLength`), rings drawn over the jets, empty gap under the board (content sits in the upper third), bold skill-check title (every prompt is the same 22 sp bold), explainer jet cut by the edge (figure drawn at 88 %), HUMO button taking the whole bar on a 320 dp phone (found in `phones/320x568-P4-start.png`, fixed), top jet clipped by the scroll box while the tutor is open (24 dp room above the board).

## B) Differences from the Duolingo pattern (part: ours vs reference)

| Part | Ours | Duolingo |
|---|---|---|
| Top bar | Close cross and a 16 dp mint bar, nothing else (Boss: no XP while learning) | Close cross, green bar, a counter on the right |
| Primary button | Mint with a 4 dp darker edge, 16 dp radius, ExtraBold capitals, 0.8 sp letter spacing | Same pattern in their green or blue |
| Bottom bar | HUMO (Humo's face + word) and CHECK; disabled CHECK flat grey | Calculator, HELP and CHECK |
| Right answer | Mint panel, tick, our praise line, small flag top right, SHOW ME HOW and CONTINUE | Green panel, praise, REPORT flag, CONTINUE |
| Wrong answer | Yellow panel (never red), nudge line, GET HELP and TRY AGAIN | Red panel |
| Tutor | Bottom sheet, grabber, Humo 110 dp, outlined bubble with a tail, letters typing in, one chip row that scrolls, a text field with a mic, DONE | Same, without a field |
| Tutor and the board | The board shrinks to sit above the sheet; dimmed unless Humo draws on it | Board dimmed and partly under the sheet |
| Show me how | Card under the board with the move written out, CONTINUE LESSON | Same |
| Keypad | Boss's tile tray kept as it was for now (concepts pending) | Grey keypad with 3D keys |
| Quit | Sheet with a sad Humo, line in Emma's voice, KEEP LEARNING and END LESSON | Sheet with their character and the same two choices |

## Cold start and the lesson's first frame (CI emulator, software GPU, three runs)

| Run | Warm-up off: cold start / tap to first lesson frame | Warm-up on |
|---|---|---|
| 41200868158..41202050105 | 2648 ms / 583 ms | 2775 ms / 327 ms |
| 41203929737 | 5040 ms / 367 ms | 4369 ms / 394 ms |
| 41205230622 | 3533 ms / 461 ms | 2794 ms / 397 ms |

Cause: the first frame of the lesson decodes the JSON, the 1280 px jet atlas and builds the sounds on the main thread; the warm-up (`LessonWarmup`, started a moment after launch) moves that off. The gain is about 100 ms (470 to 373 ms on average) and is close to the emulator's noise; the 2.5 to 5 s cold start is the glass shader's first frame ("glass drawn in 2.4 to 3.8 s" in the log), which this round did not touch.
CI limits set: cold start at most 7000 ms, lesson first frame at most 900 ms (about twice the worst measured warm value). The walkthrough fails above either.
