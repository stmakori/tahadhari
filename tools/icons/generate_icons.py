#!/usr/bin/env python
"""
Generates every Tahadhari app icon from one geometry definition. Pure Python 3 (standard library
only, no Pillow needed). Run from the repo root:

    python -I tools/icons/generate_icons.py

Writes:
  Android  composeApp/src/androidMain/res/drawable/ic_launcher_{background,foreground,monochrome}.xml
           composeApp/src/androidMain/res/mipmap-anydpi-v26/ic_launcher{,_round}.xml
           composeApp/src/androidMain/res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher{,_round}.png (API 24-25)
  Desktop  composeApp/src/desktopMain/resources/tahadhari-icon.png (window icon)
           composeApp/icons/tahadhari.{png,ico,icns} (installer icons)
  Preview  tools/icons/preview/icon-512.png

The mark: a white water drop on the app's deep teal (primary 0xFF0E6670, see ui/Theme.kt) with an
exclamation mark cut out of it: water + alert. All shapes live on a 108 x 108 canvas (Android
adaptive-icon units); the drop sits inside the 66dp safe zone.
"""
import math
import re
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "composeApp/src/androidMain/res"
TEAL = (0x0E, 0x66, 0x70)
TEAL_HEX = "#0E6670"
WHITE = (255, 255, 255)

# ---- geometry (108 x 108 canvas) -------------------------------------------------------------
# SVG-style path data, shared by the Android vector drawables and the rasteriser below.
FG_PATH = (
    # drop: pointed tip at the top, round bottom
    "M54,24 C54,24 32,42 32,60 A22,22 0 0 0 76,60 C76,42 54,24 54,24 Z "
    # exclamation bar (cut out with even-odd fill)
    "M50.5,41 L57.5,41 L55.8,62 L52.2,62 Z "
    # exclamation dot
    "M50.4,70 A3.6,3.6 0 1 0 57.6,70 A3.6,3.6 0 1 0 50.4,70 Z"
)


def flatten(path):
    """Flatten an SVG path (absolute M L C A Z; circular arcs only) into a list of polygons."""
    tok = re.sub(r"([MLCAZ])", r" \1 ", path.replace(",", " ")).split()
    polys, cur, pos, i = [], [], (0.0, 0.0), 0
    while i < len(tok):
        c = tok[i]
        i += 1
        if c == "M":
            if cur:
                polys.append(cur)
            pos = (float(tok[i]), float(tok[i + 1]))
            i += 2
            cur = [pos]
        elif c == "L":
            pos = (float(tok[i]), float(tok[i + 1]))
            i += 2
            cur.append(pos)
        elif c == "C":
            p1, p2, p3 = [(float(tok[i + k]), float(tok[i + k + 1])) for k in (0, 2, 4)]
            i += 6
            for s in range(1, 25):
                t = s / 24
                u = 1 - t
                cur.append((
                    u**3 * pos[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t**3 * p3[0],
                    u**3 * pos[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t**3 * p3[1],
                ))
            pos = p3
        elif c == "A":
            r = float(tok[i])
            large, sweep = int(tok[i + 3]), int(tok[i + 4])
            end = (float(tok[i + 5]), float(tok[i + 6]))
            i += 7
            # the centre lies on the perpendicular bisector of the chord; pick the side that
            # matches the large-arc / sweep flags
            mx, my = (pos[0] + end[0]) / 2, (pos[1] + end[1]) / 2
            dx, dy = end[0] - pos[0], end[1] - pos[1]
            d = math.hypot(dx, dy)
            h = math.sqrt(max(r * r - d * d / 4, 0))
            sign = 1 if large != sweep else -1
            cx, cy = mx + sign * h * (dy / d), my - sign * h * (dx / d)
            a0 = math.atan2(pos[1] - cy, pos[0] - cx)
            a1 = math.atan2(end[1] - cy, end[0] - cx)
            if sweep and a1 <= a0:
                a1 += 2 * math.pi
            if not sweep and a1 >= a0:
                a1 -= 2 * math.pi
            for s in range(1, 49):
                a = a0 + (a1 - a0) * s / 48
                cur.append((cx + r * math.cos(a), cy + r * math.sin(a)))
            pos = end
        elif c == "Z":
            pass
    if cur:
        polys.append(cur)
    return polys


def rounded_rect(x, y, w, h, r):
    pts = []
    for cx, cy, a0 in ((x + w - r, y + r, -90), (x + w - r, y + h - r, 0), (x + r, y + h - r, 90), (x + r, y + r, 180)):
        for s in range(13):
            a = math.radians(a0 + 90 * s / 12)
            pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return [pts]


def circle(cx, cy, r):
    return [[(cx + r * math.cos(t * math.pi / 90), cy + r * math.sin(t * math.pi / 90)) for t in range(180)]]


# ---- rasteriser (even-odd scanline, 8 sub-rows, exact horizontal coverage) ---------------------
SUB = 8


def coverage(polys, size, x0, y0, span):
    """Coverage mask (rows of floats 0..1) of `polys` for the canvas window starting at (x0, y0)
    of width/height `span`, rendered at size x size pixels."""
    k = size / span
    edges = []
    for poly in polys:
        for (ax, ay), (bx, by) in zip(poly, poly[1:] + poly[:1]):
            ax, ay, bx, by = (ax - x0) * k, (ay - y0) * k, (bx - x0) * k, (by - y0) * k
            if ay != by:
                edges.append((ax, ay, bx, by) if ay < by else (bx, by, ax, ay))
    rows = []
    for py in range(size):
        row = [0.0] * size
        for s in range(SUB):
            y = py + (s + 0.5) / SUB
            xs = sorted(ax + (y - ay) * (bx - ax) / (by - ay) for ax, ay, bx, by in edges if ay <= y < by)
            for a, b in zip(xs[0::2], xs[1::2]):
                a, b = max(a, 0.0), min(b, float(size))
                if b <= a:
                    continue
                ia, ib = int(a), int(b)
                if ia == ib:
                    row[ia] += (b - a) / SUB
                else:
                    row[ia] += (ia + 1 - a) / SUB
                    for x in range(ia + 1, ib):
                        row[x] += 1 / SUB
                    if ib < size:
                        row[ib] += (b - ib) / SUB
        rows.append(row)
    return rows


def render(size, window, shape):
    """RGBA bytes. window=(x0, y0, span) on the 108 canvas; shape = polygons masking the icon."""
    x0, y0, span = window
    fg = coverage(flatten(FG_PATH), size, x0, y0, span)
    mask = coverage(shape, size, x0, y0, span)
    out = bytearray()
    for py in range(size):
        for px in range(size):
            f = min(fg[py][px], 1.0)
            col = tuple(TEAL[i] * (1 - f) + WHITE[i] * f for i in range(3))
            out += bytes((round(col[0]), round(col[1]), round(col[2]), round(min(mask[py][px], 1.0) * 255)))
    return bytes(out)


def png(size, rgba):
    raw = b"".join(b"\x00" + rgba[y * size * 4:(y + 1) * size * 4] for y in range(size))

    def chunk(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(data, bytes):
        path.write_bytes(data)
    else:
        with open(path, "w", newline="\n", encoding="utf-8") as f:
            f.write(data)


# ---- outputs ------------------------------------------------------------------------------------
def android_vectors():
    head = ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp"\n    android:height="108dp"\n'
            '    android:viewportWidth="108"\n    android:viewportHeight="108">\n')
    write(RES / "drawable/ic_launcher_background.xml",
          head + f'    <path\n        android:fillColor="{TEAL_HEX}"\n        android:pathData="M0,0h108v108h-108z" />\n</vector>\n')
    for name, color in (("foreground", "#FFFFFF"), ("monochrome", "#000000")):
        write(RES / f"drawable/ic_launcher_{name}.xml",
              head + f'    <path\n        android:fillColor="{color}"\n        android:fillType="evenOdd"\n'
                     f'        android:pathData="{FG_PATH}" />\n</vector>\n')
    for name in ("ic_launcher", "ic_launcher_round"):
        write(RES / f"mipmap-anydpi-v26/{name}.xml",
              '<?xml version="1.0" encoding="utf-8"?>\n'
              '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
              '    <background android:drawable="@drawable/ic_launcher_background" />\n'
              '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
              '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
              '</adaptive-icon>\n')


def android_pngs():
    # Legacy (API 24-25) icons: the central 80 of the 108 canvas, as rounded square / circle.
    win = (14, 14, 80)
    for dpi, px in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        write(RES / f"mipmap-{dpi}/ic_launcher.png", png(px, render(px, win, rounded_rect(14, 14, 80, 80, 18))))
        write(RES / f"mipmap-{dpi}/ic_launcher_round.png", png(px, render(px, win, circle(54, 54, 40))))


def desktop():
    # Rounded square with a little margin, like a macOS/Windows app tile.
    win = (4, 4, 100)
    shape = rounded_rect(8, 8, 92, 92, 22)
    pngs = {px: png(px, render(px, win, shape)) for px in (16, 32, 48, 64, 128, 256, 512)}
    write(ROOT / "composeApp/src/desktopMain/resources/tahadhari-icon.png", pngs[256])
    write(ROOT / "composeApp/icons/tahadhari.png", pngs[512])
    write(ROOT / "tools/icons/preview/icon-512.png", pngs[512])
    # .ico: PNG-compressed entries (Vista and later)
    sizes = (16, 32, 48, 64, 128, 256)
    head = struct.pack("<HHH", 0, 1, len(sizes))
    entries, blobs, off = b"", b"", 6 + 16 * len(sizes)
    for s in sizes:
        d = pngs[s]
        entries += struct.pack("<BBBBHHII", s % 256, s % 256, 0, 0, 1, 32, len(d), off + len(blobs))
        blobs += d
    write(ROOT / "composeApp/icons/tahadhari.ico", head + entries + blobs)
    # .icns: ic07 (128), ic08 (256), ic09 (512)
    body = b"".join(t + struct.pack(">I", 8 + len(pngs[s])) + pngs[s]
                    for t, s in ((b"ic07", 128), (b"ic08", 256), (b"ic09", 512)))
    write(ROOT / "composeApp/icons/tahadhari.icns", b"icns" + struct.pack(">I", 8 + len(body)) + body)


if __name__ == "__main__":
    android_vectors()
    android_pngs()
    desktop()
    print("icons written")
