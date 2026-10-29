// Builds the launcher icon from the approved clean Humo (brain card rules/two-five-clean: Soft 2.5D body, two-leaf
// sprout, clean eyes). Boss 2026-10-29 13:05: "Humo's head only, not the whole body": the rig SVG is framed by its
// viewBox on the face and the sprout, so the eyes fill the safe zone and the launcher's mask cuts the bell's feet.
// Rendered as vector at every density (no upscaled PNG), plus the monochrome (themed) layer and a 48 dp audit board
// on a dark and a light wallpaper (docs/qa/launcher-48dp.png).
// Run on the laptop (uses the web repo's Playwright + sharp):  node scripts/art/build_launcher_icon.mjs
import { createRequire } from 'node:module';
import fs from 'node:fs';
import path from 'node:path';

const require = createRequire('D:/Humopedia/web/package.json');
const { chromium } = require('playwright');
const sharp = require('sharp');

const SVG = 'D:/30_library/assets/humo-brand/code-soft25d/rig/humo-rig.svg';
const RES = path.resolve('app/src/main/res');
// On the 108 dp canvas (visible circle 72 dp, safe circle 66 dp): the face group (eyes, lids, smile) is 54 dp wide and
// centred at (54, 64.5): the head fills the circle, the sprout sits just inside its top and the mask cuts the feet.
const FACE_W_DP = 54;
const FACE_AT = [54, 64.5];
const DENSITIES = { mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4 };

const src = fs.readFileSync(SVG, 'utf8').replace(/<ellipse id="shadow"[^>]*\/>/, '').replace(/<rect id="page"[^>]*\/>/, '');
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM ?? 'C:/Users/Murodjon/AppData/Local/ms-playwright/chromium_headless_shell-1248/chrome-headless-shell-win64/chrome-headless-shell.exe' });
const page = await browser.newPage({ viewport: { width: 1024, height: 1024 } });
await page.setContent(`<html><body style="margin:0;background:transparent">${src.replace('<svg ', '<svg width="1024" height="1024" ')}</body></html>`);
const box = (id) => page.evaluate((id) => { const b = document.getElementById(id).getBBox(); return { x: b.x, y: b.y, w: b.width, h: b.height }; }, id);
const face = await box('face');
const hair = await box('hair');
const s = FACE_W_DP / face.w; // dp per source unit
const vx = face.x + face.w / 2 - FACE_AT[0] / s, vy = face.y + face.h / 2 - FACE_AT[1] / s, vw = 108 / s;
console.log('face', face, 'sprout top at', ((hair.y - vy) * s).toFixed(1), 'dp (visible circle top = 18 dp)');
const framed = src.replace(/viewBox="[^"]*"/, `viewBox="${vx} ${vy} ${vw} ${vw}"`);

const fgAt = {};
for (const [name, k] of Object.entries(DENSITIES)) {
  const canvas = Math.round(108 * k);
  await page.setViewportSize({ width: canvas, height: canvas });
  await page.setContent(`<html><body style="margin:0;background:transparent">${framed.replace('<svg ', `<svg width="${canvas}" height="${canvas}" `)}</body></html>`);
  const fg = await page.screenshot({ omitBackground: true, clip: { x: 0, y: 0, width: canvas, height: canvas } });
  fgAt[name] = fg;
  const dir = path.join(RES, `drawable-${name}`);
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, 'ic_launcher_humo.png'), fg);

  // Monochrome: Humo's silhouette in white; pupils and mouth (the dark parts) are cut out so the face still reads.
  const { data, info } = await sharp(fg).raw().toBuffer({ resolveWithObject: true });
  const mono = Buffer.alloc(info.width * info.height * 4);
  for (let i = 0; i < info.width * info.height; i++) {
    const r = data[i * 4], g = data[i * 4 + 1], b = data[i * 4 + 2], a = data[i * 4 + 3];
    const lum = 0.2126 * r + 0.7152 * g + 0.0722 * b;
    const dark = Math.max(0, Math.min(1, (110 - lum) / 50));
    mono[i * 4] = mono[i * 4 + 1] = mono[i * 4 + 2] = 255;
    mono[i * 4 + 3] = Math.round(a * (1 - dark));
  }
  await sharp(mono, { raw: { width: info.width, height: info.height, channels: 4 } }).png().toFile(path.join(dir, 'ic_launcher_humo_mono.png'));
  console.log(name, canvas, 'px');
}

// Audit board: the icon as a launcher shows it (circle and squircle masks over the deep-teal tile) at 48 dp (xxhdpi,
// 144 px) and at 4x, on a dark and a light wallpaper.
const tile = (px) => `<div style="width:${px}px;height:${px}px;position:relative;overflow:hidden;border-radius:var(--r);background:linear-gradient(135deg,#17615A,#0E3F3B 50%,#0A2A33)">
  <img src="data:image/png;base64,${fgAt.xxxhdpi.toString('base64')}" style="position:absolute;left:${-px / 4}px;top:${-px / 4}px;width:${px * 1.5}px;height:${px * 1.5}px"></div>`;
const row = (bg, ink) => `<div style="display:flex;gap:40px;align-items:center;padding:36px;background:${bg};color:${ink};font:600 22px sans-serif">
  <div style="--r:50%">${tile(144)}</div><div style="--r:30%">${tile(144)}</div><div style="--r:50%">${tile(576)}</div><div>48 dp · circle · squircle · 4x</div></div>`;
await page.setViewportSize({ width: 1400, height: 1300 });
await page.setContent(`<html><body style="margin:0">${row('#1D2430', '#fff')}${row('#EEF1F4', '#111')}</body></html>`);
await page.screenshot({ path: path.resolve('docs/qa/launcher-48dp.png'), fullPage: true });
await browser.close();
console.log('board docs/qa/launcher-48dp.png');
