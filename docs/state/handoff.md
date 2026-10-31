# Handoff (builder, L1.1/L1.2 round 3) - START HERE

State 2026-10-31: branch feat/native-app, version 3.0.0-alpha08 / code 42. Round 3 = the web player's phone look 1:1:
Figtree chrome (LessonScreen.kt), Humo drawn from the rig (HumoArt.kt paths, LessonHumo.kt: springs, blink to one soft line,
talk on every line, body/eyes baked into bitmaps because the light layers took 1-8 s per frame on CI's software GPU),
verdict states (yellow Try again, green Continue, glow, tint), preview-before-Check flights (LessonBoards.kt), one Humo
conversation per lesson + chat sheet + teal look-together marks (LessonChat.kt, LessonFlow.chat/marks), banner end pass
(LessonFinale.kt MachPassRoute). Cry and sunglasses were dropped on Boss's word.
Side-by-sides (ours left, web look-390 right): docs/qa/round3/*.png. Roborazzi test: LessonRound3Test.
Push over HTTPS if SSH fails: git -c credential.helper='!gh auth git-credential' push https://github.com/tolipovmurodjon/humopedia-android.git feat/native-app
CI artifacts go to build-ci/<run>/ (never stage build-ci). Read failures with: gh run view <id> --log-failed.
Not done: typed questions in the chat (chips only); Review/Retry screens stay in LessonFlow though the UI no longer shows them.
