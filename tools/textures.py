"""Generates Ledz textures. Run from repo root: python3 tools/textures.py"""
from PIL import Image, ImageDraw, ImageFilter
import colorsys, os

A = 'src/main/resources/assets/ledz/textures'
os.makedirs(f'{A}/block', exist_ok=True)
os.makedirs(f'{A}/item', exist_ok=True)


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def save(img, path):
    img.save(path, optimize=True)


# block/led: near-white diffuser with faint centre line
img = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        d = abs(y - 7.5) / 7.5
        v = round(255 - 28 * d * d)
        img.putpixel((x, y), (v, v, v, 255))
save(img, f'{A}/block/led.png')


BLUE, GREEN = (30, 91, 255), (25, 230, 160)

# item/led_strip: coiled reel of strip with LEDs
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
px = img.load()
pcb, pcb_d, pcb_l = (46, 52, 60), (28, 32, 38), (78, 86, 96)
# diagonal strip from bottom-left to top-right, 3px wide
for i in range(14):
    x, y = 1 + i, 14 - i
    for w in (-1, 0, 1):
        yy = y + w
        if 0 <= yy < 16:
            px[x, yy] = (pcb_l if w == -1 else pcb_d if w == 1 else pcb) + (255,)
    if i % 3 == 1:
        c = lerp(BLUE, GREEN, i / 13)
        px[x, y] = c + (255,)
        if x + 1 < 16:
            px[x + 1, y - 1 if y - 1 >= 0 else y] = lerp(c, (255, 255, 255), 0.45) + (255,)
# reel end
for (x, y) in [(0, 14), (0, 15), (1, 15), (2, 15)]:
    px[x, y] = pcb_d + (255,)
save(img, f'{A}/item/led_strip.png')

# item/led_configurator: handheld remote with gradient screen
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
body, body_d, body_l = (54, 60, 70), (30, 34, 40), (92, 100, 112)
copper, copper_d = (214, 128, 82), (150, 80, 50)
d.rectangle([4, 1, 11, 14], fill=body + (255,))
d.line([4, 1, 4, 14], fill=body_l + (255,))
d.line([4, 1, 11, 1], fill=body_l + (255,))
d.line([11, 2, 11, 14], fill=body_d + (255,))
d.line([5, 14, 11, 14], fill=body_d + (255,))
for x in range(5, 11):
    t = (x - 5) / 5
    c = lerp(BLUE, GREEN, t)
    for y in range(3, 7):
        cc = lerp(c, (255, 255, 255), 0.25) if y == 3 else c
        img.putpixel((x, y), cc + (255,))
d.rectangle([5, 8, 6, 9], fill=copper + (255,))
d.rectangle([9, 8, 10, 9], fill=copper + (255,))
img.putpixel((6, 9), copper_d + (255,))
img.putpixel((10, 9), copper_d + (255,))
for x in (5, 7, 9):
    img.putpixel((x, 11), (200, 60, 60, 255) if x == 5 else body_l + (255,))
img.putpixel((7, 0), (170, 120, 230, 255))
img.putpixel((8, 0), (120, 80, 200, 255))
save(img, f'{A}/item/led_configurator.png')

# logo 256: dark tile, block outline with gradient strip, glow
S = 256
logo = Image.new('RGBA', (S, S), (16, 20, 24, 255))
glow = Image.new('RGBA', (S, S), (0, 0, 0, 0))
g = ImageDraw.Draw(glow)
L = ImageDraw.Draw(logo)
# isometric cube
top = [(128, 40), (216, 88), (128, 136), (40, 88)]
left = [(40, 88), (128, 136), (128, 224), (40, 176)]
right = [(128, 136), (216, 88), (216, 176), (128, 224)]
L.polygon(top, fill=(122, 122, 128, 255))
L.polygon(left, fill=(92, 92, 98, 255))
L.polygon(right, fill=(70, 70, 76, 255))
def strip(draw, p0, p1, w, c0, c1):
    n = 64
    for i in range(n):
        t0, t1 = i / n, (i + 1) / n
        a = lerp(p0, p1, t0); b = lerp(p0, p1, t1)
        c = lerp(c0, c1, t0)
        draw.line([a, b], fill=c + (255,), width=w)
MID = lerp(BLUE, GREEN, 0.5)
for d_, w in ((g, 26),):
    strip(d_, (128, 136), (216, 88), w, BLUE, MID)
    strip(d_, (216, 88), (128, 40), w, MID, GREEN)
glow = glow.filter(ImageFilter.GaussianBlur(10))
logo = Image.alpha_composite(logo, glow)
L = ImageDraw.Draw(logo)
strip(L, (128, 136), (216, 88), 7, BLUE, MID)
strip(L, (216, 88), (128, 40), 7, MID, GREEN)
save(logo.convert('RGB'), 'src/main/resources/logo.png')
print('ok')
