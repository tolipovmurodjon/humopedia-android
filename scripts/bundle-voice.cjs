// Every line Humo can say in a lesson -> a bundled Jessa clip. Mirrors LessonLines.all (ui/lesson/LessonLines.kt), whose
// unit test (LessonVoiceCoverageTest) fails the build when any showable line has no clip. Re-runnable: lines that already
// have a clip cost nothing; a NEW VOICE is one change of VOICE_ID (or HUMO_VOICE_ID=... in the environment), then run again (old clips are deleted from assets/voice).
// Needs the laptop's local server (python lessons/serve-nocache.py 8158) that holds the key; the key never touches the app.
// usage: node scripts/bundle-voice.cjs [lessonFile=l11]
const fs = require('fs'), path = require('path'), crypto = require('crypto');
const VOICE_ID = process.env.HUMO_VOICE_ID || 'oClOrzqamOXmtcB8iqTj'; // Emma (Boss, 2026-10-31); backup id Veda Sky IR4Yywb88oVWHqzWLjAs. The server hashes files with the same id
const MODEL = process.env.HUMO_TTS_MODEL || 'eleven_v4'; // the server's cache names include the model
const SERVER = process.env.HUMO_TTS_SERVER || 'http://127.0.0.1:8158';
const course = process.env.HUMO_COURSE || 'D:/Humopedia/docs/courses/15-coordinate-transformations';
const lesson = process.argv[2] || 'l11';
const jsonPath = path.join(__dirname, '../app/src/main/resources/lessons/' + lesson + '.json');
const outVoice = path.join(__dirname, '../app/src/main/assets/voice');
const cacheDir = path.join(course, 'lessons/assets/voice');
const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

const M = 'SR-71';
const rules = [[/\b[Aa] point is translated\b/g, `The ${M} flies`], [/\bthe points\b/g, `the ${M}s`], [/\bThe points\b/g, `The ${M}s`],
  [/\b(?:a|the) point\b/g, `the ${M}`], [/\b(?:A|The) point\b/g, `The ${M}`]];
const plain = (t) => rules.reduce((s, [re, to]) => s.replace(re, to), t).replace(/\*/g, '');
const steps = (n, way) => `${Math.abs(n)} step${Math.abs(n) === 1 ? '' : 's'} ${way}`;
const SENSE = '\n\nDoes that make sense?';
const graded = (p) => p.kind === 'Drag' || p.kind === 'Translate';
const hint = (p) => p.skill ? 'This one is yours. Take your time, you can do it.'
  : p.kind === 'Drag' ? 'Count the steps right, then up. Then move it there.'
  : p.kind === 'Translate' ? `Pick one ${M} and its ring. Count right, then up.`
  : `Watch one ${M} move. The others move the same.`;
const why = (p) => p.explain ? plain(p.explain) + SENSE : 'Check first. Then I will show you why.';
const look = (p) => p.kind === 'Translate'
  ? 'Look at the teal squares. What do you notice?'
  : 'Look at the teal squares. What do you notice?';
const showMe = (p) => p.kind === 'Translate'
  ? `Compare each ${M} with its ring. Fix the difference, then try again.`
  : `Compare the two squares. Move it the difference, then try again.`;
const how = (p) => {
  const [mx, my] = p.move, [fx, fy] = p.points[0];
  return p.kind === 'Drag'
    ? `The ${M} starts at (${fx}, ${fy}). Move it ${steps(mx, mx >= 0 ? 'right' : 'left')}: x goes from ${fx} to ${fx + mx}. Move it ${steps(my, my >= 0 ? 'up' : 'down')}: y goes from ${fy} to ${fy + my}. It lands on (${fx + mx}, ${fy + my}).`
    : `Each ${M} moves ${steps(mx, mx >= 0 ? 'right' : 'left')} and ${steps(my, my >= 0 ? 'up' : 'down')}, so the translation is (${mx}, ${my}). For example, (${fx}, ${fy}) moves to (${fx + mx}, ${fy + my}).`;
};

const lines = new Set();
for (const p of data.pages.filter((q) => !q.skill)) if (data.coach[p.id]) lines.add(data.coach[p.id]);
data.praise.forEach((t) => lines.add(t));
["Ok, let's get into it.", "Ok, let's go. What do you need?",
  'Right adds to x, up adds to y. Ready for the next one?', 'Nice. Fix it and try again.', 'Sure. Take your time.',
  'Check first. Then I will show you why.', 'Say it another way?', 'Leaving so soon? Your spot is saved. Come back when you are ready.', 'I need to hear you. Tap Allow, please.', 'No problem. You can type, or tap one of these.'].forEach((t) => lines.add(t));
// Humo's wrong-try pools (player.js linePools), every slip from 3 short to 3 too far, and what the fix-it replay says
const linePools = (ex, ey) => {
  const xs = `${Math.abs(ex)} ${ex > 0 ? 'too far' : 'short'}`, ys = `${Math.abs(ey)} ${ey > 0 ? 'too far' : 'short'}`;
  return {
    xOnly: [`Up's good. Right is ${xs}.`, `So close. Right is ${xs}.`, `Half there! Right is ${xs}.`, `Right's the slip: ${xs}.`, `Up works. Count right again.`, `Nice up. Right needs a recount.`],
    yOnly: [`Right's good. Up is ${ys}.`, `So close. Up is ${ys}.`, `Half there! Up is ${ys}.`, `Up's the slip: ${ys}.`, `Right works. Count up again.`, `Nice right. Up needs a recount.`],
    both: [`Right ${xs}, up ${ys}.`, `Both are off. Right first.`, `Not yet. Count right, then up.`, `Both slipped. Try one ${M} first.`, `Count the gap to one ring.`, `Two to fix. Start with right.`],
    swap: [`Right first, up second. Swap them.`, `Right numbers, wrong order.`, `Flip those two numbers.`, `Order slip: right goes first.`],
    generic: [`Not on the rings yet.`, `Missed it. Look at the rings.`, `Close! Try another pair.`, `Off the mark. Count again.`, `Rings still empty. Go again.`, `Hmm, not quite. Recount.`, `Still short. Try once more.`, `Nope, not that one. Again?`],
  };
};
for (let ex = -3; ex <= 3; ex++) for (let ey = -3; ey <= 3; ey++) if (ex || ey) { const P = linePools(ex, ey); (ex && !ey ? P.xOnly : ey && !ex ? P.yOnly : P.both).forEach((t) => lines.add(t)); P.swap.forEach((t) => lines.add(t)); P.generic.forEach((t) => lines.add(t)); }
for (let a = 0; a <= 5; a++) for (let b = 0; b <= 5; b++) lines.add(`You moved ${a} right, ${b} up.`);
for (const m of [...data.pages.filter((q) => q.skill), ...data.fixit]) { const [dx, dy] = m.move; lines.add(dy ? `Right ${dx}, then up ${dy}.` : `Right ${dx}. Nothing up.`); }
for (const k of ['fixLook2', 'fixNone', 'fixThen', 'fixNew', 'fixPass', 'fixTitle']) data.fx[k].forEach((t) => lines.add(t));
for (const p of [...data.pages, ...data.fixit]) {
  lines.add(hint(p)); lines.add(hint(p) + SENSE); lines.add(plain(p.prompt));
  if (!p.skill) lines.add(why(p));
  if (graded(p)) { lines.add(look(p)); lines.add(showMe(p)); lines.add(how(p)); }
}

// node's own http (fetch refuses the Sec-Fetch-Site header the local server wants)
const get = (url) => new Promise((resolve) => require('http').get(url, { headers: { 'Sec-Fetch-Site': 'same-origin' } }, (res) => { res.resume(); res.on('end', () => resolve({ status: res.statusCode })); }).on('error', () => resolve({ status: 0 })));
// Eleven v4 reads inline audio tags as spoken direction. Used sparingly, never on a task prompt; the app shows and matches the
// clean line (no brackets), so spoken words = written words.
const TAGS = { [data.fx.fixTitle[0]]: '[softly]' };
data.praise.forEach((t, i) => { if (i % 3 === 0) TAGS[t] = '[cheerful]'; });
const spoken = (t) => (TAGS[t] ? `${TAGS[t]} ${t}` : t);
const file = (t) => crypto.createHash('sha1').update(`${VOICE_ID}|${MODEL}|${spoken(t)}`).digest('hex').slice(0, 16) + '.mp3';
(async () => {
  const voice = {}, failed = [];
  let fetched = 0;
  for (const t of lines) {
    const f = file(t), cached = path.join(cacheDir, f);
    if (!fs.existsSync(cached)) {
      const r = await get(`${SERVER}/tts?text=${encodeURIComponent(spoken(t))}`);
      if (r.status === 429) { console.log('server cap reached (150 new lines an hour): run again later'); failed.push(t); continue; }
      if (r.status !== 200) { failed.push(t); console.log(`no clip (${r.status}): ${t.slice(0, 60)}`); continue; }
      fetched++;
    }
    if (!fs.existsSync(cached)) { failed.push(t); continue; }
    fs.copyFileSync(cached, path.join(outVoice, f));
    voice[t] = f;
  }
  const keep = new Set(Object.values(voice));
  for (const f of fs.readdirSync(outVoice)) if (f.endsWith('.mp3') && !keep.has(f)) fs.unlinkSync(path.join(outVoice, f));
  data.voice = voice;
  fs.writeFileSync(jsonPath, JSON.stringify(data, null, 1));
  console.log(`${lines.size} lines, ${Object.keys(voice).length} clips bundled (${fetched} newly made), ${failed.length} without a clip`);
  failed.forEach((t) => console.log('  - ' + t.slice(0, 90)));
  process.exit(failed.length ? 1 : 0);
})();
