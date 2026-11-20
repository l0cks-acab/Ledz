"""CurseForge images for Ledz 0.3.0 (spots, Light Controller). python3 tools/hero_030.py <shots_dir> <out_dir>"""
import sys, os
sys.argv = [sys.argv[0], sys.argv[1], sys.argv[2]]
from PIL import Image, ImageDraw
SH, OUT = sys.argv[1], sys.argv[2]
_save_argv = sys.argv
# reuse helpers from hero.py without running its image list
src = open(os.path.join(os.path.dirname(__file__), 'hero.py')).read().split("room_wash = ")[0]
exec(src)

def shot(n):
    return os.path.join(SH, n + '.png')

def label(canvas, x, y, text):
    d = ImageDraw.Draw(canvas)
    f = BODY(22)
    w = d.textlength(text, font=f)
    d.rectangle([x, y, x + w + 24, y + 36], fill=(8, 12, 20, 215))
    d.text((x + 12, y + 5), text, font=f, fill=(225, 235, 245, 255))

def text_block(c, title, lines, y0=170):
    glow_text(c, (56, y0), title, TITLE(58))
    d = ImageDraw.Draw(c)
    for i, l in enumerate(lines):
        d.text((60, y0 + 100 + i * 50), l, font=BODY(34), fill=(225, 235, 245, 255))
    chips(c, 60, y0 + 100 + len(lines) * 50 + 24)

# 06 spots
c = bg(shot('spots_hero'))
win = window(Image.open(shot('spots_hall')), 690)
place(c, win, W - win.width - 46, (H - win.height) // 2)
text_block(c, 'LED Spots', ['Small downlights that', 'cast a coloured pool', 'on what they face'])
save(c, '06-spots.png')

# 07 spots hero (player)
c = bg(shot('spots_hall'))
win = window(Image.open(shot('spots_hero')), 690)
place(c, win, W - win.width - 46, (H - win.height) // 2)
text_block(c, 'Pools of light', ['Up, down or sideways:', 'spots and panels', 'paint the surfaces', 'around you'], y0=140)
save(c, '07-pools.png')

# 08 controller off / on
c = bg(shot('ctrl_on'))
off, on = window(Image.open(shot('ctrl_off')), 540), window(Image.open(shot('ctrl_on')), 540)
x = W - on.width - 46
place(c, off, x, 44)
place(c, on, x, H - on.height - 44)
label(c, x + 14, 58, 'Lever off')
label(c, x + 14, H - on.height - 30, 'Lever on')
text_block(c, 'Light Controller', ['One lever runs', 'every light on', 'its channel'], y0=170)
save(c, '08-light-controller.png')

# 09 channels: new look in one click
c = bg(shot('ctrl_scene2'))
big = window(Image.open(shot('ctrl_scene2')), 690)
bx, by = W - big.width - 46, (H - big.height) // 2 - 30
place(c, big, bx, by)
close = Image.open(shot('ctrl_close')).convert('RGB')
cw, ch = close.size
close = close.crop((int(cw * 0.25), int(ch * 0.08), int(cw * 0.95), int(ch * 0.78)))
small = window(close, 300)
place(c, small, bx - 40, by + big.height - small.height + 70)
text_block(c, 'Channels', ['16 dye channels.', 'Configurator on the', 'controller: the whole', 'room changes'], y0=140)
save(c, '09-channels.png')
print('ok')
