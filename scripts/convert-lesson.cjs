// One converter: the web lesson file (lessons/<id>/lesson.js) -> a JSON the native app reads, so the two never disagree.
// Also copies the cached Humo voice mp3 of every scripted line that has one and writes the missing ones to stdout.
// usage: node scripts/convert-lesson.cjs [courseDir] [lessonId]
const fs = require('fs'), path = require('path'), crypto = require('crypto');
const course = process.argv[2] || 'D:/Humopedia/docs/courses/15-coordinate-transformations', id = process.argv[3] || 'L1.1';
globalThis.window = {};
const L = require(path.join(course, 'lessons', id, 'lesson.js')), S = L.strings.en;
const grids = { '6,6,1': 'graph', '7,6,0': 'figure', '7,5,2': 'work', '8,6,2': 'recap' };
const pages = L.screens.filter((p) => p.type !== 'frame').map((p) => {
  const g = p.grid, key = [g.x1, g.y1, g.every].join(',');
  if (!grids[key]) throw new Error(`${p.page}: unknown grid ${key}`);
  const o = { id: p.page, kind: p.type === 'drag' ? 'Drag' : p.type === 'explainer' ? 'Explainer' : p.type === 'translate' ? 'Translate' : 'Recap',
    grid: grids[key], prompt: S[p.lines[0]], points: p.planes, move: p.order, skill: !!p.skill, explain: p.explain ? S[p.explain] : null };
  if (p.targets) p.targets.forEach((t, j) => { if (t[0] !== p.planes[j][0] + p.order[0] || t[1] !== p.planes[j][1] + p.order[1]) throw new Error(`${p.page}: target ${j} is not point + move`); });
  return o;
});
const voiceDir = path.join(course, 'lessons/assets/voice'), outVoice = path.join(__dirname, '../app/src/main/assets/voice');
const name = 'SR-71', w = (t) => t.replace(/\bthe points\b/g, `the ${name}s`).replace(/\bthe point\b/g, `the ${name}`).replace(/\bThe point\b/g, `The ${name}`).replace(/\*\*(.+?)\*\*/g, '$1').replace(/\*(.+?)\*/g, '$1');
const lines = new Set([...Object.values(S.coach).map(w), ...S.praise.split('|'), ...S.nudge.split('|'), ...pages.map((p) => w(p.prompt)),
  `Not all the ${name}s landed on their targets, lowkey close though.`, `The ${name} isn't in the right spot yet, no stress.`, "Thanks, we'll take a look at this page.",
  "Ok, let's get into it.", 'This one tripped you up on the skill check. How did you figure out what to put in the slots?', "Here's how it goes."]);
const voice = {}, missing = [];
for (const t of lines) {
  const f = crypto.createHash('sha1').update('yj30vwTGJxSHezdAGsv9|' + t).digest('hex').slice(0, 16) + '.mp3';
  if (fs.existsSync(path.join(voiceDir, f))) { fs.copyFileSync(path.join(voiceDir, f), path.join(outVoice, f)); voice[t] = f; } else missing.push(t);
}
const out = { id, title: S.title, pages, coach: S.coach, praise: S.praise.split('|'), nudge: S.nudge.split('|'), voice };
fs.writeFileSync(path.join(__dirname, '../app/src/main/resources/lessons/' + id.replace('.', '').toLowerCase() + '.json'), JSON.stringify(out, null, 1));
console.log(`${pages.length} pages, ${Object.keys(voice).length} voice files bundled, ${missing.length} lines without a cached mp3:`);
missing.forEach((t) => console.log('  - ' + t));
