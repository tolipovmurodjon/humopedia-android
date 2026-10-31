# Round 4 diff: native L1.1 (alpha09, CI run 41193154756) vs the web player

Pictures: this folder, one side-by-side per state (web left = engine/shots/look-390-*.png, native right). The web pictures
for P4-wrong, P4-correct and SC-score are the older round-3 captures; the web player has since changed (texts, buttons), so
those three differ on purpose and the text/flow comparison below is against the current engine/player.js. NOT re-shot on the web:
Playwright was not run this round (see B-9). Native shots of every state at 390, 360 and 1.5x font: CI artifact
`screenshots-and-reports/outputs/roborazzi/round4/<390|360|font150>-<state>.png` (84 files).

## A) What was wrong and is now fixed in this round
1. Lesson numbers were hand-copied and stale (P1 start (2,1) move (3,1) ...): now read from `resources/lessons/l11.json`, written by `scripts/convert-lesson.cjs` from lessons/L1.1/lesson.js (targets checked as point + move).
2. Skill check had no verdict and auto-advanced: now corner verdict "Correct!" / "Not quite." with drawn icons, "Here's how it goes." on I don't know (the machines fly the right move), Continue / Finish, no Humo, no progress bar, page tint kept.
3. Score buttons "Retry skill check / Continue" -> "Review with Humo / Skip review" (Continue when perfect); the extra Retry screen removed; review ends at the end card; Try again in review clears the tiles. Score titles "So close!" / "Good start!".
4. Wrong bubble was cut to "close th..." (2 lines): 3 lines now. Wrong/right bubble text and delay as the web: right +0.5 s, wrong +0.65 s; wrong text "Not all the SR-71s landed on their targets, lowkey close though." / "The SR-71 isn't in the right spot yet, no stress."; praise rotates by page place.
5. Coach, praise, nudge lines come from the web file. Skill-check splash line "Let's see if it all clicked."
6. Voice: bundled Jessa clips, mouth moves for the clip's length, speaker toggle (saved). Two identical speaker icons fixed: read aloud is now a text-lines icon.

## B) What still differs from the web (each is a choice for Boss unless noted)
1. Voice: only 6 of 31 scripted lines have a cached mp3 (list in the report). The rest are silent with an estimated mouth. Needs `/tts` run on the laptop, then re-run the converter. Not a code gap.
2. Wrong state timing: web sets the state 0.1 s after Check; native sets it at once (250 ms colour tween). Not changed (Boss's choice, invisible).
3. Praise queue: web queues voice lines; native stops the old line when a new one starts. Boss's choice.
4. Humo chat replies are native's own scripted logic (LessonChat.kt), not the web's data-driven `lookTogether`/`why`: wording differs in places; maths is the same. Not ported this round.
5. Skill-check sparks: native pays 15 per right item like the web; end card shows 90 for a run with 1 right item.
6. Chat sheet: web has a free text box ("How can I help?"); native has chips only. Not ported.
7. Flag / read aloud: native uses a local log line and Android TextToSpeech; web logs to localStorage. Same intent.
8. Mach 3 pass: matches the round-3 look (fire, smoke, banner); not re-compared frame by frame this round.
9. Web screenshots at 390x844 of every state were not regenerated (web player was being changed by another agent): comparisons above use the committed look-390 pictures plus the code.
