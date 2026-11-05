"""Ledz icon. python3 tools/icon.py <out_dir>"""
import sys, os
from PIL import Image, ImageDraw, ImageFilter, ImageFont
OUT = sys.argv[1]; os.makedirs(OUT, exist_ok=True)
S = 1024
STOPS = [(30, 91, 255), (25, 200, 240), (25, 230, 160), (155, 48, 255), (255, 48, 160), (30, 91, 255)]
def grad(t):
    t = t % 1 * (len(STOPS) - 1); i = int(t); f = t - i
    a, b = STOPS[i], STOPS[min(i + 1, len(STOPS) - 1)]
    return tuple(round(a[k] + (b[k] - a[k]) * f) for k in range(3))
bg = Image.new('RGBA', (S, S), (10, 13, 22, 255))
# soft vignette
v = Image.new('L', (S, S), 0); ImageDraw.Draw(v).ellipse([-200, -200, S + 200, S + 200], fill=255)
bg = Image.composite(bg, Image.new('RGBA', (S, S), (4, 5, 10, 255)), v.filter(ImageFilter.GaussianBlur(160)))
cx, cy, r = S / 2, S / 2 + 30, 300
top = (cx, cy - r * 1.15); rt = (cx + r, cy - r * 0.575); ctr = (cx, cy); lt = (cx - r, cy - r * 0.575)
lb = (cx - r, cy + r * 0.575); bot = (cx, cy + r * 1.15); rb = (cx + r, cy + r * 0.575)
d = ImageDraw.Draw(bg)
d.polygon([top, rt, ctr, lt], fill=(30, 32, 40, 255))
d.polygon([lt, ctr, bot, lb], fill=(18, 20, 26, 255))
d.polygon([ctr, rt, rb, bot], fill=(12, 13, 18, 255))
# visible edges as one run
path = [lb, lt, top, rt, rb, bot, lb]
extra = [(lt, ctr), (ctr, rt), (ctr, bot)]
segs = list(zip(path, path[1:])) + extra
total = len(segs)
def draw(dr, w):
    for i, (a, b) in enumerate(segs):
        n = 60
        for k in range(n):
            t0, t1 = k / n, (k + 1) / n
            p0 = (a[0] + (b[0] - a[0]) * t0, a[1] + (b[1] - a[1]) * t0)
            p1 = (a[0] + (b[0] - a[0]) * t1, a[1] + (b[1] - a[1]) * t1)
            dr.line([p0, p1], fill=grad((i + t0) / total) + (255,), width=w)
glow = Image.new('RGBA', (S, S), (0, 0, 0, 0)); draw(ImageDraw.Draw(glow), 70)
bg.alpha_composite(glow.filter(ImageFilter.GaussianBlur(40)))
glow2 = Image.new('RGBA', (S, S), (0, 0, 0, 0)); draw(ImageDraw.Draw(glow2), 34)
bg.alpha_composite(glow2.filter(ImageFilter.GaussianBlur(12)))
draw(ImageDraw.Draw(bg), 22)
bg = bg.convert('RGB')
for sz in (1024, 512, 256, 120):
    bg.resize((sz, sz), Image.LANCZOS).save(os.path.join(OUT, f'ledz-icon-{sz}.png'), optimize=True)
