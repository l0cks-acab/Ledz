"""Modes GIF frames for 05-modes.gif. python3 tools/modes_gif.py <mode_shots> <shots_dir> <frames_out>, then ffmpeg palettegen (256, stats_mode=full) + paletteuse (sierra2_4a) at 6.667 fps."""
sys.argv = [sys.argv[0], sys.argv[1], sys.argv[2], sys.argv[3]]
SHOTS, SH, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
src = open('tools/hero.py').read().split("room_wash = ")[0].replace("SH, OUT = sys.argv[1], sys.argv[2]", "")
exec(src)
fsrc = open('tools/modes_frames.py').read().split("frames = [")[0].replace("FR, OUT = sys.argv[1], sys.argv[2]", "")
exec(fsrc)
for i, p in enumerate(sorted(glob.glob(os.path.join(SHOTS, 'mode_*.png')))):
    c = bg(os.path.join(SH, 'modes.png'))
    win = window(label(p), 690)
    place(c, win, W - win.width - 46, (H - win.height) // 2)
    glow_text(c, (56, 170), 'Animated modes', TITLE(58))
    d = ImageDraw.Draw(c)
    for k, l in enumerate(['Static, Scroll, Pulse,', 'Rainbow, Chase, Sparkle,', 'Strobe and Breathe']):
        d.text((60, 270 + k * 50), l, font=BODY(34), fill=(225, 235, 245, 255))
    chips(c, 60, 270 + 3 * 50 + 24)
    c.convert('RGB').save(os.path.join(OUT, f'f{i:02d}.png'))
