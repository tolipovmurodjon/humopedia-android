# L1.1 Translating Points: the numbers spec (native rebuild)

Measured 2026-10-29 on Brilliant's own L1.1 (their real Elm widgets, served locally) at **390 x 844 CSS px**, by
`D:\Humopedia\docs\courses\15-coordinate-transformations\lessons\ref390.cjs` (states, computed styles, scene-graph boxes,
WAAPI calls, sound cues) and `ref390-motion.cjs` (per-frame point positions). Raw: `...\lessons\shots\ref-390\measure.json`,
`motion.json`, 56 PNGs (git-ignored, Brilliant's UI: never in this repo). Numbers only; no Brilliant asset ships.
All px below are CSS px at 390 wide = dp on a 390 dp phone. The widget is 350 wide at x 20; on other widths every widget
number scales by `(screenWidth - 40) / 350`.

## 1. Page frame
| Part | Measured |
|---|---|
| Top bar | 64 tall. Close X 36x36 at x 20 (ink #000 strokes). Back arrow 19x36 at x 72, only from page 2 on. Progress track 12 tall, radius 999, track #E6E6E6 (rgba 0,0,0,.1), fill #29CC57, moves 550 ms cubic-bezier(.2,.8,.2,1); the fill is a sliver (3 %) on page 1. 3 skill-check dots 8x8 round, 6 apart: idle #E6E6E6, current #A3A3A3, right #29CC57, wrong #4A4A4A. Sparks "0 ✧" 18 px 600 grey #9B9B9B; once > 0 ink + green star |
| Card | inset 8 left/right, top 48 bottom 8 (788 tall), white, 1 px #E2E2E2, radius 18. Correct: border #3FCE69 + 4 px ring rgba(63,206,105,.12). Wrong: border #F6D545 + 4 px ring rgba(246,213,69,.16). Border change 250 ms |
| Page tint | correct: radial edge tint rgba(63,206,105,.13); wrong: rgba(246,213,69,.18) (edge pixel #FDF5D8); fade 350 ms |
| Prompt | 16 px / 25.6, weight 400, #111, x 20, width 350; numbers and *a*, *b* in maths serif (KaTeX). Skill check: 18 px / 28.8, bold. Content block is centred vertically in the card ("mid") |
| Content enter | opacity 0→1 + translateX 28→0, 320 ms cubic-bezier(.2,.8,.2,1). Buttons row: opacity + translateY 6→0, 200 ms |
| Verdict bubble | bottom-left above the buttons (x 20, 12 px radius, 15 px / 21.75, weight 600, padding 13 x 16). Right: bg #BDF0C9, text rotates "Good!", "Nice!", "Nicely done.", "Yep.", "Correct.", "Boom.", "Perfect.", "Exactly.". Wrong: bg #FBE8A4, alternates "Hmm, think about that again." / "Not quite. What's another way to look at it?" |
| +15 sparks | a green chip "+15 ✦" at the card's top right on the first correct per page; P1, P2, P4, P5, P6 = 75 in all |

## 2. Buttons (bottom, 48 tall, y 772)
| Button | Measured |
|---|---|
| Check (primary) | full width beside the tutor dock (75..369 = 294 wide), radius 56, 16 px. Disabled until the board changes: bg #F2F2F2, text #D5D5D5. Enabled: bg #383838 (≈ #2B2B2B), white text, 3 px bottom edge #111 |
| Try again / Continue | same primary shape and dark colour. Review mode: teal #2CBFA7 |
| Why? / Get help | grey pill #EBEBEB, ink #222, 600 17 px, bottom edge 3 px #CFCFCF, left of the primary |
| Skill check footer | "I don't know" outline (white, 1 px #CFCFCF, bottom edge 3 px #CFCFCF, 600 17 px) + Check, side by side |
| Screens (score, retry, end) | 290 x 48, radius 999, 600 17 px, 62 apart. Dark #2B2B2B + 3 px #111 edge; outline white + 1 px #CFCFCF + 3 px #CFCFCF edge |
| Start over | right-aligned under the board, "↻ Start over" 16 px weight 500, 148 x 48; disabled rgba(0,0,0,.4) → enabled #111 once the board changed; hidden in the skill check |

## 3. Graph board (P1, P2: "Graph")
| Part | Measured |
|---|---|
| Box | 350 x 370 at y 212. Range 0..6 both axes, labels 1..6 every 1, no axis names |
| Origin | (48, 308) in the box; cell **42.67** |
| Axes | #000 1.5 px, arrow heads 10 x 11.5 filled #000, axes run 6.7 cells; ticks 10 px #000 1.5 px at every integer |
| Grid | rgba(0,0,0,.1) 1 px, to 6.47 cells |
| Labels | 16 px, centred 18 px below the x axis / 15 px left of the y axis |
| Origin dot | 10 px #000. Start dot: 10 px #808080, stays where the point began |
| Handle idle | white halo 40 px at .9 opacity over a 64 px soft shadow (#E0E0E0 edge), blue dot 10 px #375CE3 |
| Dragging | the dot grows 10 → 30 px; every integer node shows a 4 px #CCC dot; the point follows the finger |
| Snap | on release to the nearest node: ~90 ms ease-out (12 px in 90 ms); `toggle` sound on each node crossed and on the snap |
| Wrong | dot #D7A613; halo stays; badge 16 x 16 rounded square #D7A613 with a white X, centred 36 px above the point |
| Correct | halo goes; dot #15B441; badge 16 x 16 #15B441 with a white tick, centred 21 px above |
| Answer | P1 (2,1) → (5,2). P2 (1,3) → (4,4). Graded by the node only |

## 4. Explainer figure (P3)
Box 350 x 366; range 0..6, no labels, cell 47, origin (22, 334). Start dots #808080 10 px at (2,1) and (1,3); end dots
blue #375CE3 (5,2), orange #E77A16 (4,4); coloured segment start→end 2 px; grey legs #808080 2 px along a then b; dimension
markers 8 px from the legs with "a" / "b" in grey 16 px, centred. Static, then Continue.

## 5. Transformation workspace (P4-P6, SC1-3)
| Part | Measured |
|---|---|
| Frame | 350 wide at x 20, 1.5 px #E5E5E5 border, radius 6. Graph area 346 x 256, divider #E5E5E5, workspace row 66 tall |
| Graph | x 0..7, y 0..5, labels every 2, axis names "x" "y" bold 16 px; cell **37.15**; origin (44, 216) in the graph area |
| Points | 12 px #456DFF on a 20 px #ECF0FF halo |
| Targets | 18 px ring, stroke 2 px #CCC, fill rgba(0,0,0,.024) |
| Row | "1" mono 14 px rgba(0,0,0,.6) at x 10; action chip 191 x 32 bg #F2F2F2 radius 4: "translate" 500 16 px #294BC6, "by (" 16 px, slot, ",", slot, ")" |
| Slots | 32 x 24, radius 4, 2 px border. Active: #456DFF on #ECF0FF. Empty: #999 on white. Filled: digit mono 16 px, border rgba(0,0,0,.1) |
| Bank | 350 x 60, bg #DAE2FF, radius 4, 14 below the frame. Tiles 40 x 34 white, radius 6, shadow 0 2 0 rgba(0,0,0,.4), mono 500 16 px, 8 apart, centred: 0 1 2 3 4 5 |
| Flow | slot 1 starts active; a tile fills the active slot and the next empty one turns active; when both are full the bank hides and the points **glide** (hold ~200 ms, then ~700 ms decelerating); pale ghosts stay at the start (12 px #DAE2FF, stroke #ABBDFF) joined to the point by a 2 px #ABBDFF line at .7. Tapping a full slot makes it active again and the bank returns; the next tile replaces it |
| Wrong | points #D7A613 + halo #FDF3D3; ghosts #E5E5E5 / #CCC; lines grey. Try again keeps the tiles; Start over clears them, the points glide back |
| Correct | points #15B441, no halo; the row number becomes a 16 px green tick box; slots plain white |
| Answers | P4 (2,2): (1,1),(1,2) → (3,3),(3,4). P5 (3,1): (3,1),(1,2),(1,3) → (6,2),(4,3),(4,4). P6 (2,3): (1,1),(1,2),(3,1),(3,2) → (3,4),(3,5),(5,4),(5,5). SC1 (3,2): (1,1),(3,1) → (4,3),(6,3). SC2 (0,3): → (1,4),(3,4). SC3 (2,4): → (3,5),(5,5) |

## 6. Recap (P7)
Graph x 0..8, y 0..6, every 2, cell 32.5; six points translated by (3,2) from (2,1),(1,2),(5,1),(4,3),(3,1),(1,4); ghosts +
lines shown; the row shows fixed tiles 3, 2 (bg rgba(0,0,0,.05)). "▶ Play" pill 90 x 48, 2 px rgba(0,0,0,.1) border,
500 16 px, centred 14 below the frame. Play: points jump back to the ghosts, hold 200 ms, glide **600 ms** decelerating
(measured 20 % at 50 ms, 48 % at 150 ms, 70 % at 256 ms, 97 % at 545 ms ≈ cubic ease-out); target rings show during the run.

## 7. Why? (after a correct answer)
A sheet from the bottom over the lower half (Brilliant: the tutor chat): "Why is this the answer?" then Brilliant's
explanation text (lesson.js `ex1..sc3`) and "Does that make sense?" with two chips. Sounds: tick + hint.

## 8. Skill check
Splash "Skill check" 32 px weight 800 with the check badge, 1.7 s, start-lesson sound, scale .96→1 in 350 ms. Items: prompt
bold 18 px "Translate the points to the target."; footer "I don't know" + Check. Check: no verdict, no colour, the next item
after 500 ms; the dot turns green (right) or dark (wrong). Score screen: "n/3" 72 px weight 800 + title (3 Perfect!, 2 So
close!, 1 That was tricky!, 0 Keep practicing!) 32 px weight 800; "Review mistakes" (dark) + "Skip review" (outline), or
"Continue" at 3/3. Review: the missed item in its wrong state, Try again (teal). Retry screen: "Retry skill check" / "Skip
retry". End: "Lesson complete" 32 px 800 + "75 sparks earned." 16 px #7A7A7A + the button.

## 9. Sounds (Brilliant's files, measured; ours are made as PCM, never their mp3)
| Cue | When | Theirs | Ours |
|---|---|---|---|
| correct | right Check | 410 ms, peak 50 ms, 3-4 soft rising hits, warm | 3 rising notes C6 E6 G6, 90 ms apart, sine + soft 2nd harmonic, 6 ms rise, 220 ms fall |
| incorrect | wrong Check | 365 ms, dry soft clicks, quiet (peak .12) | 2 soft low wood notes E4 → C4, 120 ms apart, 160 ms fall, half volume |
| tick | Try again, Start over, tile, Why? | one 5 ms click ~2 kHz | 8 ms sine click 2.1 kHz, exponential fall |
| toggle | node crossed while dragging, snap | 30 ms low thump | 30 ms 180 → 120 Hz thump |
| start | skill check splash | 570 ms rising chime | C5 E5 G5 C6 arpeggio, 100 ms apart |
| complete | end card | 1.5 s fanfare | C5 E5 G5 C6 E6, 110 ms apart, long tail |

## 10. The loved layer (Boss's SR-71, `fly-layer.js`)
Each point is drawn as the SR-71 (`ui/path/Jet.kt` frames, the two nearest crossfaded), 0.9 of a cell long. It follows
Brilliant's point exponentially (k = 1 − 0.84^(dt/16.7 ms)), nose into the move once it moves > 3 px, flames while moving,
a smoke puff every 70 ms. Two ways to move: drag it, or tap it (a dashed ring + tick) and tap a node: a takeoff burst and
an eased flight (steps ≈ distance / 5 px). Check shows the tick or cross; the jet stays. No engine sound.
