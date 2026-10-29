// Builds the launcher icon from the approved clean Humo (brain card rules/two-five-clean: Soft 2.5D body, two-leaf
// sprout, clean eyes): renders D:/30_library/assets/humo-brand/code-soft25d/rig/humo-rig.svg with a transparent
// background, then writes the adaptive foreground and the monochrome (themed) layer at every density.
// Run on the laptop (uses the web repo's Playwright + sharp):  node scripts/art/build_launcher_icon.mjs
import { createRequire } from 'node:module';
import fs from 'node:fs';
import path from 'node:path';

const require = createRequire('D:/Humopedia/web/package.json');
const { chromium } = require('playwright');
const sharp = require('sharp');

const SVG = 'D:/30_library/assets/humo-brand/code-soft25d/rig/humo-rig.svg';
const RES = path.resolve('app/src/main/res');
// Humo's box inside the 108 dp canvas: 50 dp keeps the bell (its round bottom corners) inside the 72 dp visible
// circle that launchers mask to; the 66 dp safe zone holds the face.
const HUMO_DP = 50;
const DENSITIES = { mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4 };

const svg = fs.readFileSync(SVG, 'utf8').replace(/<ellipse id="shadow"[^>]*\/>/, '').replace(/<rect id="page"[^>]*\/>/, '');
// The web repo's Playwright wants a browser build this laptop may not have; any installed Chromium renders SVG the same.
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM ?? 'C:/Users/Murodjon/AppData/Local/ms-playwright/chromium_headless_shell-1248/chrome-headless-shell-win64/chrome-headless-shell.exe' });
const page = await browser.newPage({ viewport: { width: 1024, height: 1024 } });
await page.setContent(`<html><body style="margin:0;background:transparent">${svg.replace('<svg ', '<svg width="1024" height="1024" ')}</body></html>`);
const full = await page.screenshot({ omitBackground: true, clip: { x: 0, y: 0, width: 1024, height: 1024 } });
await browser.close();

const trimmed = await sharp(full).trim({ threshold: 1 }).toBuffer({ resolveWithObject: true });
const { width: tw, height: th } = trimmed.info;
console.log('humo box', tw, th);

for (const [name, k] of Object.entries(DENSITIES)) {
  const canvas = Math.round(108 * k);
  const box = Math.round(HUMO_DP * k);
  const scale = box / Math.max(tw, th);
  const w = Math.round(tw * scale), h = Math.round(th * scale);
  const humo = await sharp(trimmed.data).resize(w, h).png().toBuffer();
  const left = Math.round((canvas - w) / 2), top = Math.round((canvas - h) / 2);
  const dir = path.join(RES, `drawable-${name}`);
  fs.mkdirSync(dir, { recursive: true });
  const fg = await sharp({ create: { width: canvas, height: canvas, channels: 4, background: { r: 0, g: 0, b: 0, alpha: 0 } } })
    .composite([{ input: humo, left, top }]).png().toBuffer();
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
