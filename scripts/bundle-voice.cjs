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
  : p.kind === 'Drag' ? `Start at the ${M}. Count the steps to the right along the grid first, then count the steps up. Then move it there.`
  : p.kind === 'Translate' ? `Pick one ${M} and its empty circle. Count the steps right, then the steps up. Every ${M} moves the same way.`
  : `Watch one ${M}: it moves some steps right and some steps up. The other one moves exactly the same.`;
const why = (p) => p.explain ? plain(p.explain) + SENSE : 'Check your answer first. Then I can show you why it works.';
const look = (p) => p.kind === 'Translate'
  ? `Let's look together. The teal squares show where your ${M}s landed and where the targets are. What do you notice?`
  : `Let's look together. The teal squares show where your ${M} started and where it is now. Count the steps between them. What do you notice?`;
const showMe = (p) => p.kind === 'Translate'
  ? `Check each ${M} against its ring. Count the steps right, then the steps up, and change your numbers by exactly the difference. Then try again.`
  : `Compare the two teal squares with the task. Count the steps right, then the steps up, and move your ${M} by exactly that much. Then try again.`;
const how = (p) => {
  const [mx, my] = p.move, [fx, fy] = p.points[0];
  return p.kind === 'Drag'
    ? `The ${M} starts at (${fx}, ${fy}). Move it ${steps(mx, mx >= 0 ? 'right' : 'left')}: x goes from ${fx} to ${fx + mx}. Move it ${steps(my, my >= 0 ? 'up' : 'down')}: y goes from ${fy} to ${fy + my}. It lands on (${fx + mx}, ${fy + my}).`
    : `Each ${M} moves ${steps(mx, mx >= 0 ? 'right' : 'left')} and ${steps(my, my >= 0 ? 'up' : 'down')}, so the translation is (${mx}, ${my}). For example, (${fx}, ${fy}) moves to (${fx + mx}, ${fy + my}).`;
};

const lines = new Set();
for (const p of data.pages.filter((q) => !q.skill)) if (data.coach[p.id]) lines.add(data.coach[p.id]);
data.praise.forEach((t) => lines.add(t));
[`Not all the ${M}s landed on their targets, lowkey close though.`, `The ${M} isn't in the right spot yet, no stress.`, "Ok, let's get into it.", "Ok, let's go. What do you need?",
  'Great, that is the whole idea: right adds to x, up adds to y.\n\nReady for the next one?', 'Great. Fix it and try again.', 'Sure. Take your time.',
  'Check your answer first. Then I can show you why it works.', 'Say it another way?', 'I need your microphone to hear you. Tap Allow, please.', 'No problem. You can type, or tap one of these.'].forEach((t) => lines.add(t));
for (const p of data.pages) {
  lines.add(hint(p)); lines.add(hint(p) + SENSE); lines.add(plain(p.prompt));
  if (!p.skill) lines.add(why(p));
  if (graded(p)) { lines.add(look(p)); lines.add(showMe(p)); lines.add(how(p)); }
}

// node's own http (fetch refuses the Sec-Fetch-Site header the local server wants)
const get = (url) => new Promise((resolve) => require('http').get(url, { headers: { 'Sec-Fetch-Site': 'same-origin' } }, (res) => { res.resume(); res.on('end', () => resolve({ status: res.statusCode })); }).on('error', () => resolve({ status: 0 })));
// Eleven v4 reads inline audio tags as spoken direction. Used sparingly, never on a task prompt; the app shows and matches the
// clean line (no brackets), so spoken words = written words.
const TAGS = {
  "Nice, that's it!": '[cheerful]', 'Boom, nailed it.': '[laughs softly]', "Let's go, perfect.": '[cheerful]',
  [`Start at the ${M}. Count the steps to the right along the grid first, then count the steps up. Then move it there.`]: '[curious]',
  'Great. Fix it and try again.': '[cheerful]', 'Say it another way?': '[curious]',
};
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
