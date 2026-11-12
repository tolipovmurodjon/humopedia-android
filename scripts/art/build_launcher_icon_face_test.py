#!/usr/bin/env python3
"""Test-only second launcher icon "Humo B" (Boss, 2026-11-12): the face zoom, to compare with A on his real phone.

A (the shipped icon) stays build_launcher_icon_navy.py. B is the same Humo with only the camera moved: his face fills the
icon on an all-mint tile, eyes in the middle, the sprout falls outside. His lower body fades into the mint, so no legs.
Writes into app/src/debug/res only, so the Play build never carries it; app/src/debug/AndroidManifest.xml adds the alias.

Run from the android repo:  python scripts/art/build_launcher_icon_face_test.py
"""
import os
import sys

import numpy as np
from PIL import Image, ImageChops

sys.argv = sys.argv[:1]  # the navy generator reads argv[1] as its Humo source
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_launcher_icon_navy as b  # noqa: E402

DEBUG_RES = os.path.join(b.ROOT, "app", "src", "debug", "res")
K, EYE_Y = 0.486, 201.6  # the zoom Boss made by hand in Figma and called perfect
FADE_UNITS = (770, 850)  # drawing units where his body melts into the mint tile
MINT_RAMP = [(0.00, (0.91, 0.120, 166)), (0.45, (0.84, 0.145, 168)), (1.00, (0.75, 0.140, 170))]


def face_layer():
    humo = Image.open(b.HUMO).convert("RGBA")
    size = int(1024 * K * b.Q)
    h = humo.resize((size, size), Image.LANCZOS)
    layer = Image.new("RGBA", (b.N, b.N), (0, 0, 0, 0))
    layer.paste(h, (int((216 - K * b.EYE_X_UNITS) * b.Q), int((EYE_Y - K * b.EYE_Y_UNITS) * b.Q)), h)
    ya, yb = [int((EYE_Y + K * (u - b.EYE_Y_UNITS)) * b.Q) for u in FADE_UNITS]
    y = np.arange(b.N)
    ramp = np.clip((yb - y) / (yb - ya), 0, 1)
    ramp = ramp * ramp * (3 - 2 * ramp)
    mask = Image.fromarray(np.repeat((ramp * 255).astype("uint8")[:, None], b.N, 1))
    layer.putalpha(ImageChops.multiply(layer.split()[3], mask))
    return layer


def main():
    b.RAMP = MINT_RAMP
    tile = b.tile()
    layer = face_layer()
    os.makedirs(os.path.join(DEBUG_RES, "drawable-nodpi"), exist_ok=True)
    tile.resize((432, 432), Image.LANCZOS).convert("RGB").save(
        os.path.join(DEBUG_RES, "drawable-nodpi", "ic_launcher_face_tile.webp"), lossless=True, method=6
    )
    for name, k in b.DENSITIES.items():
        px = int(round(108 * k))
        d = os.path.join(DEBUG_RES, "drawable-" + name)
        os.makedirs(d, exist_ok=True)
        layer.resize((px, px), Image.LANCZOS).save(os.path.join(d, "ic_launcher_face.png"))
        b.mono(layer.resize((px, px), Image.LANCZOS)).save(os.path.join(d, "ic_launcher_face_mono.png"))
    full = Image.alpha_composite(tile, layer)
    eyes = 253 / 288  # eye span (drawing units 250..770 at this zoom) over the visible square
    b.masked(full, 512).save(os.path.join(b.ROOT, "docs", "qa", "launcher-face-B.png"))
    print("B written to app/src/debug/res | eyes span %.0f %% of the visible icon width" % (eyes * 100))


if __name__ == "__main__":
    main()
