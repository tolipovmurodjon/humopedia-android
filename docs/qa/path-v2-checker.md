# Path v2 — fresh checker (CI run 40836363476, 2026-10-29)

Read-only review. Checked: 15 Roborazzi PNGs, 20 walkthrough PNGs + steps.txt, Boss's two before-shots,
`ui/path/*`, `ui/glass/*`, `res/` launcher, `docs/qa/icons-board.png`. Skills used: apple-design and emil-design-eng.

## Score: **6.5 / 10** (before: about 3/10)

This is a real jump. There is no text on the map. The islands are big and the three states are clear without
words. The trail and the footbridge look like a road you could walk. The top bar is in. What still pulls it
down: the clouds look like one sticker pasted many times, not a floating sky. The "tap me" pulse cannot be
seen. The map stutters (92 % janky frames). The scrim turns the whole world grey, close to the fog Boss hated.

## The first 5 seconds, as a 10-12-year-old

"Ooh, a jet!" The SR-71 is the first thing the eye lands on: it is the only dark, realistic object on a pastel
map. Next comes the gold block with the flag, and they know "that's me". Then the clouds: they look like white
paper cut-outs lying on the grass, not a sky the jet flies through. "15 ▾" means nothing to them yet. The gem
and fire counts read well. The map is calm and clear, but it does not move enough to feel alive. On a real
phone the stutter would make it feel cheap.

## Founder notes, one by one

| Note | Verdict | Evidence |
|---|---|---|
| No text on the map | ✅ | `PathScreen.kt:285`: names only in TalkBack; no text in any shot |
| Big, clean, tappable nodes | ✅ | 88 dp (current 100 dp) × 0.86 = 76-86 dp tap boxes (`PathScreen.kt:113-114, 274-275`) |
| Walkable path | ✅ / ⚠️ | 30 dp dirt trail, pebbles, bridge at each unit end. Pebbles vs dirt = **1.08:1**, so they read as gravel noise, not stepping stones |
| Low-poly clouds, not fog | ⚠️ | No blur ✅. But one 4-puff template (`LowPoly.kt:181`), body vs page **1.04:1**, no cast shadow, so they read as flat stickers |
| SR-71 really flies | ⚠️ | Figure-eight with bank and shadow ✅. In landscape the jet is off-screen and only its **shadow** shows (`08-home-path-landscape.png`) |
| Top bar: course, gems, streak | ✅ / ⚠️ | Glass, fixed 48 dp ✅. Streak icon **2.98:1** < 3:1. "15" has no meaning for a child |
| Icons, 5 sets | ✅ / ⚠️ | Board has 5 sets. Shipped set A: Battle "active" is only a thicker X (not filled), and it reads as "close" |
| Launcher icon | ✅ / ⚠️ | Clean sprout Humo, no old hair ✅. Humo box is 50 dp of 108 dp (`build_launcher_icon.mjs:17`), so it looks small on the home screen |

## Contrast (computed from the hex in the code; pane ≈ #F8FCFA)

| Pair | Ratio | Rule | |
|---|---|---|---|
| Ink #0F1630 on pane (counts) | 17.2 | 4.5 | ✅ |
| White on Primary #1C7E69 (Davom etish) | 4.96 | 4.5 | ✅ |
| Muted #4D5670 on pane / on 16 % scrim | 7.05 / 5.03 | 4.5 | ✅ |
| Gem #1E8FCB on pane | 3.47 | 3 | ✅ |
| **Streak #E8711F on pane** | **2.98** | 3 | ❌ |
| Lock #5D6880 on locked top #C6CFDA | 3.55 | 3 | ✅ |
| Tick #1C7E69 on white badge | 4.96 | 3 | ✅ |
| **Pulse ring white on page #F3FBF7** | **1.05** | 3 | ❌ invisible |
| Cloud body / underside vs page | 1.04 / 1.25 | (art) | ⚠️ dissolves into the page |
| Flagstone face vs dirt | 1.08 | (art) | ⚠️ |
| Page under the scrim vs page | 1.40 | — | ⚠️ grey cast over the whole map |

## Top 10 fixes, ranked

1. **Stop the jank: take the Sky out of the scrolled content.** `PathScreen.kt:162` puts `Sky` inside a box that is
   `layout.height` tall (≈2,700 dp, ≈8,000 px at 3×). The box redraws every frame because `seconds` is read in
   draw (`:340`). The walk measured **96/104 janky frames, p50 150 ms**. Change: move `Sky(...)` out of the
   `verticalScroll` Box. Make it a sibling at `Modifier.fillMaxSize()` (viewport-sized), and in `onDrawBehind`
   wrap the drawing in `translate(0f, -scroll.value.toFloat())`. Culling at `:348` already skips the clouds you
   cannot see. Then measure again with gfxinfo on the A05s.
2. **Make the "tap me" pulse visible.** `PathScreen.kt:295`: `Color.White.copy(alpha = 0.75f * (1f - p))` is
   1.05:1 on the page. Change it to `Glass.Primary.copy(alpha = 0.55f * (1f - p))` (4.71:1 at full) and the
   stroke at `:298` to `4.dp`. Add a slow bob of 3 dp on the current island (`graphicsLayer { translationY = sin(t*2π/2.4)*3.dp }`).
3. **Clouds that float, not stickers.** (a) `PathScreen.kt:355`: before each cloud, draw a cast shadow on the ground.
   Use `drawOval(Color(0xFF1F5E57).copy(alpha = 0.10f), …)` offset +16 dp right and +44 dp down, 0.8× the cloud
   width, and let it move with the cloud's drift and parallax. (b) `LowPoly.kt:176`: underside `0xFFD3E3EC` →
   `0xFFB8CCDA`. (c) `LowPoly.kt:181`: pick the puff list from `seed % 3` (3 silhouettes: 3, 4 and 5 puffs with
   different heights) so no two neighbours match. (d) `LowPoly.kt:185`: `n = 7` → `n = 5` with a second facet,
   for real low-poly angles instead of cartoon semicircles.
4. **No grey scrim over the world.** `PathScreen.kt:171`: `Glass.Ink.copy(alpha = 0.16f)` greys the whole map
   (the "fog" again, see `09-locked-sheet.png`, `10-course-list.png`). Change it to
   `Color(0xFF0B3D36).copy(alpha = 0.08f)` for the level sheet. Use no scrim for the course list: it is a
   dropdown and should not block. Keep only the transparent tap-to-close layer.
5. **Jet never shows a lone shadow.** `PathLayout.kt:204`: `cy` ignores the viewport, so in landscape (390 dp
   tall) the loop sits behind the top bar. Pass `viewport` and `scrollDp` into `plane(...)` and clamp
   `cy` to `[scrollDp + 120, scrollDp + viewport*THUMB - 140]`. At `PathScreen.kt:343`, skip the shadow when
   the plane centre is outside the viewport.
6. **Streak icon to 3:1.** `PathScreen.kt:415`: `Color(0xFFE8711F)` → `Color(0xFFD0600E)` (3.79:1 on the pane).
7. **Battle icon: real crossed swords with a filled active state.** `TabIcons.kt:23-24`: the idle X reads as
   "close", and `BattleFilled` is the same strokes at 2.8 dp. Add guards (a short cross-bar per blade at
   ~1/3 from the hilt) and make the active version `Filled` blade shapes, like Home and Leaderboard. Edit
   `scripts/art/build_icons.mjs` (the file is generated) and redraw `docs/qa/icons-board.png`.
8. **Walkable stones you can see.** `LowPoly.kt:118`: face `0xFFDCCBA4` → `0xFFCDB68A`, and rim (`:116`)
   `0xFFBCA57E` → `0xFFA88C5E`, so each stone stands off the dirt. `PathLayout.kt:108`: radius `6.5f + …` →
   `8.5f + …`. At `PathLayout.kt:104`, a 5.5 dp zig-zag → 7 dp, so it reads as left-right footsteps.
9. **Done island hidden behind the nav bar on short phones.** In walk `01-home.png` (640 dp tall), the done stone
   sits under the tab bar. In `PathScreen.kt:148`, pass `viewport - padding.calculateBottomPadding().value` to
   `scrollFor`, and at `PathLayout.kt:221` set `THUMB = 0.55f`. Then the stone behind (proof of progress) and
   the current one both show above the bar.
10. **Launcher Humo bigger.** `scripts/art/build_launcher_icon.mjs:17`: `HUMO_DP = 50` → `56`. Re-run it, then
    check that the bell stays inside the 66 dp safe circle on an overlay before you ship. Also make the course
    chip say something to a child: `PathScreen.kt:405`, `"${course?.number}"` → `"${course?.number}-kurs"`
    (Boss's brief said "Course 15 ▾"; the top bar is not the map, so a word is allowed there).

## Not checked / caveats
- The jank numbers come from the CI emulator. Its GPU p90 of 4,950 ms suggests software rendering, so measure
  again on the A05s. Fix 1 is still right: an 8,000 px layer redrawn every frame is costly on any GPU.
- I did not compare against the 77 refs in `D:\Humopedia\design\`. The lesson screen (`03b`) and the
  dark-mode shot were only glanced at: dark is the same as light by design (`Glass.kt:67`).
- Optional, not ranked: put a tiny Humo on the current island (the Duolingo-avatar move). That makes "you are
  here" obvious even without the pulse.
