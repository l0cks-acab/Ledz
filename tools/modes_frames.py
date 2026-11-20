"""Label mode-row frames and build modes.png + modes.gif. python3 tools/modes_frames.py <frames_dir> <out_dir>"""
import sys, os, glob
from PIL import Image, ImageDraw, ImageFont
FR, OUT = sys.argv[1], sys.argv[2]
F = ImageFont.truetype('/mnt/skills/examples/canvas-design/canvas-fonts/Outfit-Bold.ttf', 34)
NAMES = ['Breathe', 'Strobe', 'Sparkle', 'Chase', 'Rainbow', 'Pulse', 'Scroll', 'Static']
XS = [430, 586, 730, 882, 1036, 1186, 1336, 1482]
BOX = (360, 215, 1560, 890)


def label(path):
    im = Image.open(path).convert('RGB')
    d = ImageDraw.Draw(im)
    for n, x in zip(NAMES, XS):
        w = d.textlength(n, font=F)
        d.text((x - w / 2 + 2, 590), n, font=F, fill=(0, 0, 0))
        d.text((x - w / 2, 588), n, font=F, fill=(230, 238, 245))
    return im.crop(BOX).resize((1280, 720), Image.LANCZOS)


frames = [label(p) for p in sorted(glob.glob(os.path.join(FR, 'mode_*.png')))]
frames[5].save(os.path.join(OUT, 'modes.png'))
q = [f.resize((960, 540), Image.LANCZOS).quantize(colors=255, method=Image.Quantize.MEDIANCUT) for f in frames]
q[0].save(os.path.join(OUT, 'modes.gif'), save_all=True, append_images=q[1:], duration=150, loop=0)
print('ok')
