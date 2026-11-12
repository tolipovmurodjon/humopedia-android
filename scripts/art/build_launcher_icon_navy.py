#!/usr/bin/env python3
"""Launcher icon A3 (Boss, 2026-11-12: "A3 is perfect"): the real Humo on a rich navy tile.

Why a script and not hand-made files: the tile is a radial OKLCH light with grain (an XML gradient can only mix 3 sRGB stops
in a straight line, which Boss called flat), and every density, the themed layer and the Play picture must come from one source.
Humo is the approved drawing rendered 1:1 (right eye and its glow = copies of the left; pupils and smile in one soft
ink #133132 instead of near-black); his body carries clay volume (a darker rim, a core shadow, one
soft highlight: lightness range 0.22 in the visible window, because the soft 2.5D shading at 0.10 read as one flat
mint on a real phone, Boss 2/10); only the camera moves: he sits low and the
bottom edge of the visible square cuts him flat, so no legs show under any launcher mask.

Run from the android repo:  python scripts/art/build_launcher_icon_navy.py [path to the 2048 px Humo render]
Needs Pillow + numpy. Writes res/drawable-nodpi/ic_launcher_tile.webp, res/drawable-*/ic_launcher_humo(.png|_mono.png),
the Play listing icon and the audit board docs/qa/launcher-navy.png.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")
HUMO = sys.argv[1] if len(sys.argv) > 1 else "D:/Humopedia/docs/design/2026-11-12-app-icon/humo-real-clay-2048.png"
Q = 4  # supersampling: the 108 dp canvas is drawn at 432 * Q px
N = 432 * Q
K, EYE_Y, CUT = 0.39, 274, 362  # Humo scale (canvas px per drawing unit), eye line and flat cut, on the 432 px canvas
EYE_X_UNITS, EYE_Y_UNITS = 509.4, 611.0  # the middle of his eyes in the 1024-unit drawing
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

# The tile is ONE colour family lit from the top-left (Boss, 2026-11-12: "do one colour, not black... without sparkle and
# without glass, make the gradient much better"; the mesh with four hues, a glass band and sparkles scored 1/10).
# A radial light, not diagonal stripes: (distance from the light 0..1 -> OKLCH). The hue only drifts 266 > 284, the
# darkest point stays L 0.38 with full chroma, so no corner ever goes black.
LIGHT_AT = (0.20, 0.14)  # where the light sits, as a fraction of the 108 dp canvas
RAMP = [(0.00, (0.68, 0.190, 266)), (0.45, (0.52, 0.235, 273)), (1.00, (0.38, 0.215, 284))]
FLAT_BEFORE = ["#4F5BFF", "#3A33D6", "#241A8F"]  # the 3-stop sRGB tile Boss liked but called flat; only for the audit board


def gam(c):
    c = np.clip(c, 0, 1)
    return np.where(c <= 0.0031308, 12.92 * c, 1.055 * c ** (1 / 2.4) - 0.055)


def lin(c):
    return np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)


def oklab2rgb(L, a, b):
    l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
    m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
    s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
    return gam(
        np.stack(
            [
                4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
                -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
                -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
            ],
            -1,
        )
    )


def lch(L, C, h):
    return np.array([L, C * math.cos(math.radians(h)), C * math.sin(math.radians(h))])


def tile(grain=0.02, seed=7):
    y, x = np.mgrid[0:N, 0:N] / (N - 1)
    far = max(math.hypot(cx - LIGHT_AT[0], cy - LIGHT_AT[1]) for cx in (0, 1) for cy in (0, 1))
    t = np.hypot(x - LIGHT_AT[0], y - LIGHT_AT[1]) / far
    pos = np.array([r[0] for r in RAMP])
    col = np.array([r[1] for r in RAMP], float)
    i = np.clip(np.searchsorted(pos, t, side="right") - 1, 0, len(RAMP) - 2)
    f = np.clip((t - pos[i]) / (pos[i + 1] - pos[i]), 0, 1)
    f = f * f * (3 - 2 * f)  # eased, so no band shows where two stops meet
    L = col[i, 0] + (col[i + 1, 0] - col[i, 0]) * f
    C = col[i, 1] + (col[i + 1, 1] - col[i, 1]) * f
    H = np.radians(col[i, 2] + (col[i + 1, 2] - col[i, 2]) * f)
    rgb = oklab2rgb(L, C * np.cos(H), C * np.sin(H))
    rgb = np.clip(rgb + np.random.default_rng(seed).normal(0, grain, (N, N, 1)), 0, 1)  # grain stops banding
    return Image.fromarray((rgb * 255).round().astype("uint8")).convert("RGBA")


def flat_before():
    cs = [tuple(int(h[k : k + 2], 16) for k in (1, 3, 5)) for h in FLAT_BEFORE]
    g = Image.new("RGB", (64, 64))
    px = g.load()
    for yy in range(64):
        for xx in range(64):
            t = (xx + yy) / 126 * 2
            i = min(int(t), 1)
            px[xx, yy] = tuple(round(cs[i][k] + (cs[i + 1][k] - cs[i][k]) * (t - i)) for k in range(3))
    return g.resize((N, N), Image.BICUBIC).convert("RGBA")


def humo_layer():
    humo = Image.open(HUMO).convert("RGBA")
    size = int(1024 * K * Q)
    h = humo.resize((size, size), Image.LANCZOS)
    layer = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    layer.paste(h, (int((216 - K * EYE_X_UNITS) * Q), int((EYE_Y - K * EYE_Y_UNITS) * Q)), h)
    layer.paste((0, 0, 0, 0), (0, CUT * Q, N, N))
    return layer


def with_shadow(layer):
    """Two shadows in the tile's own dark hue, never black: a tight one so Humo sits in the tile, a wide one for depth."""
    out = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    for blur, strength, drop in ((14, 0.34, 7), (4, 0.42, 2)):
        a = layer.split()[3].filter(ImageFilter.GaussianBlur(blur * Q)).point(lambda v, k=strength: int(v * k))
        sh = Image.new("RGBA", (N, N), (16, 8, 78, 0))
        sh.putalpha(a)
        moved = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        moved.paste(sh, (0, drop * Q), sh)
        out = Image.alpha_composite(out, moved)
    out.paste((0, 0, 0, 0), (0, CUT * Q, N, N))
    return Image.alpha_composite(out, layer)


def mono(layer_px):
    """Themed icon: white silhouette, the dark parts (pupils, smile) cut out so the face still reads."""
    d = np.array(layer_px).astype(float)
    lum = 0.2126 * d[..., 0] + 0.7152 * d[..., 1] + 0.0722 * d[..., 2]
    dark = np.clip((110 - lum) / 50, 0, 1)
    out = np.full(d.shape, 255.0)
    out[..., 3] = d[..., 3] * (1 - dark)
    return Image.fromarray(out.round().astype("uint8"), "RGBA")


def masked(im, size, circle=False):
    v = im.crop((72 * Q, 72 * Q, 360 * Q, 360 * Q))
    n = v.size[0]
    yy, xx = np.mgrid[0:n, 0:n]
    c = (n - 1) / 2
    m = (np.hypot(xx - c, yy - c) <= c) if circle else (np.abs((xx - c) / c) ** 5 + np.abs((yy - c) / c) ** 5 <= 1)
    v.putalpha(Image.fromarray((m * 255).astype("uint8")))
    return v.resize((size, size), Image.LANCZOS)


def contrast(tile_im, layer):
    """WCAG contrast of Humo's mint against the tile pixels right outside his outline: (typical, worst 5 %)."""
    alpha = layer.split()[3]
    ring = (np.array(alpha.filter(ImageFilter.MaxFilter(4 * Q + 1))) > 128) & ~(np.array(alpha) > 128)
    ring[: 72 * Q] = False
    ring[360 * Q :] = False
    ring[:, : 72 * Q] = False
    ring[:, 360 * Q :] = False
    c = lin(np.array(tile_im.convert("RGB"), float) / 255)
    lt = (0.2126 * c[..., 0] + 0.7152 * c[..., 1] + 0.0722 * c[..., 2])[ring]
    m = lin(np.array([0x4D, 0xE5, 0xB5]) / 255)
    lm = 0.2126 * m[0] + 0.7152 * m[1] + 0.0722 * m[2]
    cr = (np.maximum(lt, lm) + 0.05) / (np.minimum(lt, lm) + 0.05)
    return float(np.median(cr)), float(np.percentile(cr, 5))


def main():
    t = tile()
    layer = humo_layer()
    fg = with_shadow(layer)
    full = Image.alpha_composite(t, fg)
    before = Image.alpha_composite(flat_before(), fg)

    os.makedirs(os.path.join(RES, "drawable-nodpi"), exist_ok=True)
    t.resize((432, 432), Image.LANCZOS).convert("RGB").save(
        os.path.join(RES, "drawable-nodpi", "ic_launcher_tile.webp"), lossless=True, method=6  # lossy WebP smooths the grain away
    )
    for name, k in DENSITIES.items():
        px = int(round(108 * k))
        d = os.path.join(RES, "drawable-" + name)
        os.makedirs(d, exist_ok=True)
        fg.resize((px, px), Image.LANCZOS).save(os.path.join(d, "ic_launcher_humo.png"))
        mono(layer.resize((px, px), Image.LANCZOS)).save(os.path.join(d, "ic_launcher_humo_mono.png"))
    play = os.path.join(ROOT, "app", "src", "main", "play", "listings", "en-US", "graphics", "icon", "play-icon.png")
    full.crop((72 * Q, 72 * Q, 360 * Q, 360 * Q)).resize((512, 512), Image.LANCZOS).convert("RGB").save(play)

    med, worst = contrast(t, layer)
    bold = "C:/Windows/Fonts/segoeuib.ttf"
    font = ImageFont.truetype(bold, 22) if os.path.exists(bold) else ImageFont.load_default()
    board = Image.new("RGBA", (1660, 620), "#EEF1F4")
    board.paste(Image.new("RGBA", (1660, 150), "#1D2430"), (0, 470))
    dr = ImageDraw.Draw(board)
    board.alpha_composite(masked(before, 380), (30, 30))
    board.alpha_composite(masked(full, 380), (440, 30))
    board.alpha_composite(masked(full, 380, True), (850, 30))
    themed = Image.alpha_composite(Image.new("RGBA", (N, N), "#2B3A55"), mono(layer))
    board.alpha_composite(masked(themed, 200), (1270, 30))
    dr.text((30, 420), "before: the flat 3-colour tile", font=font, fill="#111")
    dr.text((440, 420), "A3 v3: one navy, lit from the top-left", font=font, fill="#111")
    dr.text((850, 420), "same, round phones", font=font, fill="#111")
    dr.text((1270, 240), "themed (one colour)", font=font, fill="#111")
    dr.text((1270, 300), "Humo outline vs tile:\ntypical %.1f:1, worst %.1f:1" % (med, worst), font=font, fill="#111")
    for x, circ in ((30, False), (120, True)):
        board.alpha_composite(masked(full, 60, circ), (x, 500))
        board.alpha_composite(masked(full, 48, circ), (x + 220, 506))
    dr.text((440, 518), "real sizes on a dark wallpaper (60 px and 48 px)", font=font, fill="#fff")
    os.makedirs(os.path.join(ROOT, "docs", "qa"), exist_ok=True)
    target = os.path.join(ROOT, "docs", "qa", "launcher-navy.png")
    try:
        board.convert("RGB").save(target)
    except OSError:  # Windows locks the file while a picture viewer has it open
        target = target.replace(".png", "-new.png")
        board.convert("RGB").save(target)
    print("board:", target)
    print("tile + 5 densities + themed + play icon written | outline contrast typical %.2f:1, worst %.2f:1" % (med, worst))


if __name__ == "__main__":
    main()
