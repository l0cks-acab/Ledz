"""CurseForge hero images for Ledz. python3 tools/hero.py <shots_dir> <out_dir>"""
import sys, os
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageEnhance, ImageSequence

SH, OUT = sys.argv[1], sys.argv[2]
os.makedirs(OUT, exist_ok=True)
FD = '/mnt/skills/examples/canvas-design/canvas-fonts/'
TITLE = lambda s: ImageFont.truetype(FD + 'Tektur-Medium.ttf', s)
BODY = lambda s: ImageFont.truetype(FD + 'Outfit-Bold.ttf', s)
W, H = 1280, 720
CHIPS = [(30, 91, 255), (25, 140, 255), (25, 200, 240), (25, 230, 224), (25, 230, 160), (155, 48, 255), (255, 48, 160), (255, 138, 0)]
AQUA = (25, 230, 224)


def bg(path):
    im = Image.open(path).convert('RGB').resize((W, H), Image.LANCZOS).filter(ImageFilter.GaussianBlur(16))
    im = ImageEnhance.Brightness(im).enhance(0.42)
    tint = Image.new('RGB', (W, H), (8, 12, 26))
    im = Image.blend(im, tint, 0.35)
    # left-side darkening for text
    grad = Image.linear_gradient('L').rotate(90).resize((W, H))
    shade = Image.new('RGB', (W, H), (4, 6, 14))
    im = Image.composite(im, shade, grad.point(lambda v: int(90 + v * 0.65)))
    return im.convert('RGBA')


def window(img, w):
    img = img.convert('RGB')
    h = round(img.height * w / img.width)
    img = img.resize((w, h), Image.LANCZOS)
    pad = 2
    frame = Image.new('RGBA', (w + pad * 2, h + pad * 2), (52, 64, 80, 255))
    frame.paste(img, (pad, pad))
    return frame


def place(canvas, win, x, y):
    sh = Image.new('RGBA', (win.width + 80, win.height + 80), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rectangle([40, 48, 40 + win.width, 48 + win.height], fill=(0, 0, 0, 190))
    sh = sh.filter(ImageFilter.GaussianBlur(18))
    canvas.alpha_composite(sh, (x - 40, y - 40))
    canvas.alpha_composite(win, (x, y))


def glow_text(canvas, xy, text, font, fill=(255, 255, 255), glow=AQUA, radius=18, strength=230):
    layer = Image.new('RGBA', canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(layer).text(xy, text, font=font, fill=glow + (strength,))
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(radius)))
    ImageDraw.Draw(canvas).text(xy, text, font=font, fill=fill + (255,))


def chips(canvas, x, y):
    d = ImageDraw.Draw(canvas)
    for i, c in enumerate(CHIPS):
        d.rectangle([x + i * 46, y, x + i * 46 + 38, y + 14], fill=c + (255,))


def save(canvas, name):
    canvas.convert('RGB').save(os.path.join(OUT, name), optimize=True)


def crop_gui(path):
    return Image.open(path).convert('RGB').crop((405, 128, 873, 592))


room_wash = os.path.join(SH, 'room_w100.png')
room_lit = os.path.join(SH, 'room_w70.png')
gui = crop_gui(os.path.join(SH, 'gui_full.png'))

# 1. main
c = bg(room_wash)
place(c, window(Image.open(room_lit), 590), 650, 60)
place(c, window(gui, 320), 700, 320)
glow_text(c, (60, 190), 'Ledz', TITLE(124))
ImageDraw.Draw(c).text((64, 345), 'Gradient LED strips for every block edge', font=BODY(30), fill=(225, 235, 245, 255))
chips(c, 64, 400)
save(c, '01-ledz.png')


def feature(name, title, lines, shot, shot_w, bgshot, y=None):
    c = bg(bgshot)
    sh = Image.open(shot) if isinstance(shot, str) else shot
    win = window(sh, shot_w)
    wy = (H - win.height) // 2 if y is None else y
    place(c, win, W - win.width - 46, wy)
    glow_text(c, (56, 170), title, TITLE(58))
    d = ImageDraw.Draw(c)
    for i, l in enumerate(lines):
        d.text((60, 270 + i * 50), l, font=BODY(34), fill=(225, 235, 245, 255))
    chips(c, 60, 270 + len(lines) * 50 + 24)
    save(c, name)
    return c


feature('02-configurator.png', 'LED Configurator', ['Up to 4 colour stops,', 'animated modes,', 'light and redstone'], gui, 470, room_wash)
feature('03-runs.png', 'Runs that join', ['Strips snap to edges', 'and share one gradient', 'across blocks and corners'], os.path.join(SH, 'placements.png'), 690, os.path.join(SH, 'placements.png'))
feature('04-colour-wash.png', 'Colour wash', ['Strips tint the walls,', 'floor and ceiling', 'around them'], room_wash, 690, room_lit)
feature('05-modes.png', 'Animated modes', ['Static, Scroll, Pulse,', 'Rainbow, dim or', 'redstone controlled'], os.path.join(SH, 'modes.png'), 690, os.path.join(SH, 'modes.png'))

# animated modes hero
gif = Image.open(os.path.join(SH, 'modes.gif'))
frames = []
base = None
for fr in ImageSequence.Iterator(gif):
    frames.append(feature('_tmp.png', 'Animated modes', ['Static, Scroll, Pulse,', 'Rainbow, dim or', 'redstone controlled'], fr.convert('RGB'), 690, os.path.join(SH, 'modes.png')).convert('RGB').quantize(colors=255, method=Image.Quantize.MEDIANCUT))
os.remove(os.path.join(OUT, '_tmp.png'))
frames[0].save(os.path.join(OUT, '05-modes.gif'), save_all=True, append_images=frames[1:], duration=150, loop=0)
print('ok')
