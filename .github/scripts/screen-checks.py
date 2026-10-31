#!/usr/bin/env python3
"""The three checks no green test can skip (Boss, 2026-10-31): no cut text, no emoji, no red.

1. Problems the Roborazzi tests wrote while photographing (ScreenChecks.kt): CUT-TEXT, EMOJI, CHECK-ERROR.
2. Red: the app never uses red, so a PNG with a saturated red cluster is a bug (hue within 12 degrees of 0, S > 0.55,
   V > 0.45, at least 40 touching pixels).
3. Emoji in the app's own strings (strings.xml and Kotlin string literals), whether or not a test shows them.

Usage: screen-checks.py [roborazzi_dir] [source_dir]. Exit 1 lists every finding at once.
"""
import pathlib
import re
import sys

import numpy as np
from PIL import Image

args = [a for a in sys.argv[1:] if not a.startswith("--")]
shots = pathlib.Path(args[0] if args else "app/build/outputs/roborazzi")
source = pathlib.Path(args[1] if len(args) > 1 else "app/src/main")
EMOJI = re.compile("[\U0001F000-\U0001FAFF☀-➿⬀-⯿️]")
MIN_CLUSTER = 40
findings = []

problems = shots / "checks" / "problems.txt"
if problems.exists():
    findings += [f"[test] {line}" for line in problems.read_text(encoding="utf8").splitlines() if line.strip()]


def red_clusters(path):
    """Sizes of the 4-connected groups of red pixels in one PNG."""
    rgb = np.asarray(Image.open(path).convert("RGB"), dtype=np.float32) / 255.0
    mx, mn = rgb.max(axis=2), rgb.min(axis=2)
    delta = mx - mn
    sat = np.where(mx > 0, delta / np.maximum(mx, 1e-6), 0)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    hue = np.zeros_like(mx)
    d = np.maximum(delta, 1e-6)
    hue = np.where(mx == r, ((g - b) / d) % 6, hue)
    hue = np.where(mx == g, (b - r) / d + 2, hue)
    hue = np.where(mx == b, (r - g) / d + 4, hue)
    deg = hue * 60.0
    near_zero = (deg <= 12) | (deg >= 348)
    mask = near_zero & (sat > 0.55) & (mx > 0.45) & (delta > 0)
    ys, xs = np.nonzero(mask)
    todo = set(zip(ys.tolist(), xs.tolist()))
    sizes = []
    while todo:
        stack = [todo.pop()]
        n = 0
        while stack:
            y, x = stack.pop()
            n += 1
            for q in ((y + 1, x), (y - 1, x), (y, x + 1), (y, x - 1)):
                if q in todo:
                    todo.remove(q)
                    stack.append(q)
        sizes.append(n)
    return sizes


def self_test():
    """The control: a check that finds nothing must be shown able to find something (a red square, a mint square)."""
    import tempfile
    with tempfile.TemporaryDirectory() as d:
        for name, colour in (("red", (220, 30, 40)), ("pink-ink", (230, 120, 130)), ("mint", (28, 126, 105)), ("orange", (255, 140, 0))):
            Image.new("RGB", (20, 20), colour).save(f"{d}/{name}.png")
        found = {n: bool([x for x in red_clusters(f"{d}/{n}.png") if x >= MIN_CLUSTER]) for n in ("red", "pink-ink", "mint", "orange")}
    assert found == {"red": True, "pink-ink": False, "mint": False, "orange": False}, found
    print("screen-checks: self-test ok (red found; mint, orange and pink ignored)")


if "--self-test" in sys.argv:
    self_test()
    sys.exit(0)

pngs = sorted(shots.rglob("*.png"))
for png in pngs:
    big = [s for s in red_clusters(png) if s >= MIN_CLUSTER]
    if big:
        findings.append(f"[red] {png.relative_to(shots)}: {sum(big)} red px in {len(big)} cluster(s), largest {max(big)}")

for f in sorted(source.rglob("*")):
    if f.suffix in (".kt", ".xml") and f.is_file():
        for i, line in enumerate(f.read_text(encoding="utf8").splitlines(), 1):
            if EMOJI.search(line):
                findings.append(f"[emoji] {f}:{i}: {line.strip()[:90]}")

print(f"screen-checks: {len(pngs)} screenshots scanned")
if findings:
    print(f"screen-checks: {len(findings)} finding(s)")
    for line in findings:
        print("  " + line)
    sys.exit(1)
print("screen-checks: no cut text, no red, no emoji")
