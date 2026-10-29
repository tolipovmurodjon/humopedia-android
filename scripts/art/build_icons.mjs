// Humopedia's own icon set (PHASE-PATH-V2 #6, Boss 2026-10-29: "we did our own SVG icons, do that, many formats 5+").
// One geometry on a 24 dp grid, drawn in 5 treatments. Writes:
//   docs/qa/icons-board.png                                   every set, idle + active, for Boss to pick
//   app/src/main/java/com/mcompany/humo/ui/glass/TabIcons.kt  the shipped set (A, rounded line, filled active)
// Run on the laptop from the repo root:  node scripts/art/build_icons.mjs
import { createRequire } from 'node:module';
import fs from 'node:fs';

const require = createRequire('D:/Humopedia/web/package.json');
const { chromium } = require('playwright');

const c = (x, y, r) => `M${x - r} ${y}a${r} ${r} 0 1 0 ${2 * r} 0a${r} ${r} 0 1 0 ${-2 * r} 0`;

// Each icon: `line` = stroked outline (idle), `fill` = solid shapes (active), `holes` = cut out of the fill
// (even-odd), `always` = small parts filled in both states (a star, Humo's sprout), `idleDots` = filled only when
// idle (Humo's eyes; when active they are holes), `activeLine` = strokes kept on top of the active fill,
// `boldLine` = active is a heavier stroke instead of a fill.
// Humo's own gumdrop: a round top, wide hips, two feet (the clean 2.5D Humo, flattened to one line).
const HUMO = 'M12 5.4c-3 0-4.6 2.4-5.7 5.6l-1.6 4.8c-.9 2.6.5 4.4 2.8 4.4 1.7 0 2.6-1.2 4.5-1.2s2.8 1.2 4.5 1.2c2.3 0 3.7-1.8 2.8-4.4l-1.6-4.8C16.6 7.8 15 5.4 12 5.4Z';
const HOUSE = 'M3.5 10.6 12 3.6l8.5 7v8.4a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2Z';
const RING = 'M21.4 8.6A10 3.2-20 0 1 13.1 15A10 3.2-20 0 1 2.6 15.4';
const PODIUM = 'M3 20.5v-6a1 1 0 0 1 1-1h4.5v-4a1 1 0 0 1 1-1h5a1 1 0 0 1 1 1v6h4a1 1 0 0 1 1 1v4Z';
const GEM = 'M7.2 4h9.6l4 5.2L12 20.4 3.2 9.2Z';
const FLAME = 'M12 21.2c-3.9 0-6.6-2.7-6.6-6.3 0-3.3 2.3-5.3 3.6-8.2.4 1.9 1.3 3 2.4 3.4.2-3.5 1.7-6 4.4-7.3-.5 2.8.7 4.6 1.9 6.3 1 1.5 1.5 3 1.5 5C19.2 18.2 16.2 21.2 12 21.2Z';
const PAGE_L = 'M3.5 5.2c0-.7.5-1.2 1.2-1.2H9c1.7 0 3 1.3 3 3v13.2c0-1.2-1-2.2-2.2-2.2H3.5Z';
const PAGE_R = 'M20.5 5.2c0-.7-.5-1.2-1.2-1.2H15c-1.7 0-3 1.3-3 3v13.2c0-1.2 1-2.2 2.2-2.2h6.3Z';

const ICONS = {
  Home: { line: [HOUSE, 'M9.75 21v-4.75a2.25 2.25 0 0 1 4.5 0V21'], fill: [HOUSE], holes: ['M9.75 21v-4.75a2.25 2.25 0 0 1 4.5 0V21Z'] },
  Worlds: { line: [c(12, 12, 6), RING], fill: [c(12, 12, 6)], activeLine: [RING] },
  Battle: {
    line: ['M4 4h3.6l9.1 9.1', 'M20 4h-3.6l-9.1 9.1', 'M13.2 17.6l4.4-4.4', 'M10.8 17.6 6.4 13.2', 'M15.4 15.4 19.6 19.6', 'M8.6 15.4 4.4 19.6'],
    boldLine: true,
  },
  Leaderboard: {
    line: [PODIUM, 'M8.5 13.5v7', 'M15.5 15.5v5'], fill: [PODIUM],
    always: ['M12 1.9l.95 1.9 2.1.3-1.52 1.48.36 2.08L12 6.68l-1.88.98.36-2.08L8.95 4.1l2.1-.3Z'],
  },
  Me: {
    line: [HUMO], fill: [HUMO], holes: [c(9.9, 13.4, 1.2), c(14.1, 13.4, 1.2)],
    always: ['M12 5.4c0-1.4-.9-2.5-2.5-2.7.1 1.5 1 2.5 2.5 2.7Z', 'M12 5.4c.3-1.1 1.1-1.8 2.4-1.9-.2 1.1-1 1.8-2.4 1.9Z'],
    idleDots: [c(9.9, 13.4, 1.2), c(14.1, 13.4, 1.2)],
  },
  Gem: { line: [GEM, 'M3.2 9.2h17.6', 'M12 20.4 8.6 9.2 10.4 4', 'M12 20.4l3.4-11.2L13.6 4'], fill: [GEM], holes: ['M8.9 9.9h6.2L12 19.2Z'] },
  Streak: {
    line: [FLAME, 'M12 21.2c-1.6 0-2.8-1.1-2.8-2.7 0-1.7 1.3-2.6 2.8-4.2 1.5 1.6 2.8 2.5 2.8 4.2 0 1.6-1.2 2.7-2.8 2.7Z'],
    fill: [FLAME], holes: ['M12 20c-1.2 0-2-.8-2-2 0-1.2.9-2 2-3.2 1.1 1.2 2 2 2 3.2 0 1.2-.8 2-2 2Z'],
  },
  Course: { line: [PAGE_L, PAGE_R], fill: [PAGE_L, PAGE_R] },
};

const INK = '#0F1630', MINT = '#1C7E69', BRAND = '#3DC98C';

// ---------- SVG for the board ----------
let gid = 0;
function svgFor(icon, set, active) {
  const ink = active ? MINT : INK;
  const p = (d, extra) => `<path d="${d}" ${extra}/>`;
  const st = (w, join = 'round', cap = 'round') => `fill="none" stroke="${ink}" stroke-width="${w}" stroke-linejoin="${join}" stroke-linecap="${cap}"`;
  const filled = () => p([...icon.fill, ...(icon.holes ?? [])].join(' '), `fill="${ink}" fill-rule="evenodd"`);
  const always = (icon.always ?? []).map((d) => p(d, `fill="${ink}"`)).join('');
  const dots = (icon.idleDots ?? []).map((d) => p(d, `fill="${ink}"`)).join('');
  const lines = (w, j, cap) => icon.line.map((d) => p(d, st(w, j, cap))).join('');
  const activeLines = (w, j, cap) => (icon.activeLine ?? []).map((d) => p(d, st(w, j, cap))).join('');
  const solidOrBold = (bold, w, j, cap) => (icon.boldLine || !icon.fill ? lines(bold, j, cap) : filled() + activeLines(w, j, cap));
  let body = '', defs = '';
  switch (set.id) {
    case 'A': body = active ? solidOrBold(2.8, 2) + always : lines(2) + always + dots; break;
    case 'B': {
      const under = (icon.fill ?? []).map((d) => p(d, `fill="${active ? BRAND : INK}" fill-opacity="${active ? 0.45 : 0.14}"`)).join('');
      body = under + lines(2) + always + dots; break;
    }
    case 'C': body = active ? solidOrBold(3.4, 2.8) + always : lines(2.8) + always + dots; break;
    case 'D': {
      const top = active ? '#7FE8BC' : '#9AA3B8', base = active ? BRAND : '#5B6580', deep = active ? MINT : '#3B4560';
      const id = `g${gid++}`;
      defs = `<linearGradient id="${id}" x1="0" y1="0" x2="0.4" y2="1"><stop offset="0" stop-color="${top}"/><stop offset="0.55" stop-color="${base}"/><stop offset="1" stop-color="${deep}"/></linearGradient>`
        + `<filter id="${id}s" x="-30%" y="-30%" width="160%" height="160%"><feDropShadow dx="0.6" dy="1.2" stdDeviation="0.8" flood-color="${deep}" flood-opacity="0.35"/></filter>`;
      const shape = icon.fill
        ? p([...icon.fill, ...(icon.holes ?? [])].join(' '), `fill="url(#${id})" fill-rule="evenodd"`) + (icon.activeLine ?? []).map((d) => p(d, `fill="none" stroke="url(#${id})" stroke-width="2.6" stroke-linecap="round"`)).join('')
        : icon.line.map((d) => p(d, `fill="none" stroke="url(#${id})" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"`)).join('');
      const al = (icon.always ?? []).map((d) => p(d, `fill="url(#${id})"`)).join('');
      body = `<g filter="url(#${id}s)">${shape}${al}</g><ellipse cx="8.5" cy="8" rx="3" ry="1.4" fill="#fff" fill-opacity="0.5" transform="rotate(-30 8.5 8)"/>`; break;
    }
    case 'E': body = active ? solidOrBold(2.5, 1.75, 'miter', 'butt') + always : lines(1.75, 'miter', 'butt') + always + dots; break;
  }
  return `<svg viewBox="0 0 24 24" width="48" height="48"><defs>${defs}</defs>${body}</svg>`;
}

const SETS = [
  { id: 'A', name: 'A · Rounded line', note: '2 dp round stroke, active = filled mint · SHIPPED' },
  { id: 'B', name: 'B · Duotone', note: 'line + 14 % ink tone; active = mint tone 45 %' },
  { id: 'C', name: 'C · Chunky', note: '2.8 dp stroke for small hands; active = solid' },
  { id: 'D', name: 'D · Soft clay 3D', note: 'lit top-left, hue-tinted shadow' },
  { id: 'E', name: 'E · Geometric', note: '1.75 dp, mitre corners, flat ends' },
];

const names = Object.keys(ICONS);
const html = `<html><head><style>
body{margin:0;background:#F3FBF7;font-family:Segoe UI,Arial,sans-serif;color:${INK}}
.wrap{padding:28px 32px}
h1{font-size:26px;margin:0 0 4px} p.sub{margin:0 0 18px;color:#4D5670;font-size:14px}
.row{display:flex;align-items:center;background:#fff;border-radius:18px;margin:10px 0;padding:12px 16px;box-shadow:0 2px 10px rgba(28,126,105,.10)}
.row.ship{outline:2.5px solid ${MINT}}
.lab{width:230px} .lab b{display:block;font-size:16px} .lab span{font-size:12px;color:#4D5670}
.cell{width:116px;display:flex;justify-content:center;gap:6px}
.hd{display:flex;padding:0 16px;font-size:12px;color:#4D5670} .hd div{width:116px;text-align:center} .hd .lab{width:230px}
</style></head><body><div class="wrap">
<h1>Humopedia icons · 5 sets</h1><p class="sub">24 dp grid · each cell = idle (ink #0F1630), then active (mint #1C7E69) · drawn by scripts/art/build_icons.mjs</p>
<div class="hd"><div class="lab"></div>${names.map((n) => `<div>${n}</div>`).join('')}</div>
${SETS.map((s) => `<div class="row ${s.id === 'A' ? 'ship' : ''}"><div class="lab"><b>${s.name}</b><span>${s.note}</span></div>${names.map((n) => `<div class="cell">${svgFor(ICONS[n], s, false)}${svgFor(ICONS[n], s, true)}</div>`).join('')}</div>`).join('')}
</div></body></html>`;

const browser = await chromium.launch({ executablePath: process.env.CHROMIUM ?? 'C:/Users/Murodjon/AppData/Local/ms-playwright/chromium_headless_shell-1248/chrome-headless-shell-win64/chrome-headless-shell.exe' });
const page = await browser.newPage({ viewport: { width: 1230, height: 560 }, deviceScaleFactor: 2 });
await page.setContent(html);
fs.mkdirSync('docs/qa', { recursive: true });
await page.screenshot({ path: 'docs/qa/icons-board.png', fullPage: true });
await browser.close();

// ---------- Kotlin for the shipped set A ----------
const kq = (s) => JSON.stringify(s);
const parts = (arr, ctor) => (arr ?? []).map((d) => `${ctor}(${kq(d)})`);
const out = [];
for (const n of names) {
  const i = ICONS[n];
  const idle = [...parts(i.line, 'Stroke'), ...parts(i.always, 'Filled'), ...parts(i.idleDots, 'Filled')];
  const active = i.boldLine || !i.fill
    ? i.line.map((d) => `Stroke(${kq(d)}, width = 2.8f)`)
    : [`Filled(${kq([...i.fill, ...(i.holes ?? [])].join(' '))}, evenOdd = true)`, ...parts(i.activeLine, 'Stroke')];
  out.push(`    val ${n} = icon(${kq(n.toLowerCase())}, ${idle.join(', ')})`);
  out.push(`    val ${n}Filled = icon(${kq(n.toLowerCase() + '-filled')}, ${[...active, ...parts(i.always, 'Filled')].join(', ')})`);
}
const kt = `// GENERATED by scripts/art/build_icons.mjs (set A of docs/qa/icons-board.png). Edit the script, not this file.
package com.mcompany.humo.ui.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Humopedia's own icons (PHASE-PATH-V2 #6): 24 dp grid, 2 dp round stroke when idle, the same shape filled when
 * active. Drawn in black and tinted by the caller. Five treatments of this geometry are on docs/qa/icons-board.png.
 */
object TabIcons {
${out.join('\n')}
    val ChevronLeft = icon("chevron-left", Stroke("m15 18-6-6 6-6"))
    val ChevronDown = icon("chevron-down", Stroke("m6 9 6 6 6-6"))

    private sealed class Part(val d: String)
    private class Stroke(d: String, val width: Float = 2f) : Part(d)
    private class Filled(d: String, val evenOdd: Boolean = false) : Part(d)

    private fun icon(name: String, vararg parts: Part): ImageVector {
        val b = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        for (p in parts) {
            b.addPath(
                pathData = PathParser().parsePathString(p.d).toNodes(),
                pathFillType = if (p is Filled && p.evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
                fill = if (p is Filled) SolidColor(Color.Black) else null,
                stroke = if (p is Stroke) SolidColor(Color.Black) else null,
                strokeLineWidth = if (p is Stroke) p.width else 0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }
}
`;
fs.writeFileSync('app/src/main/java/com/mcompany/humo/ui/glass/TabIcons.kt', kt);
console.log('wrote docs/qa/icons-board.png and TabIcons.kt');
