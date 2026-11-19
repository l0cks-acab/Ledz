# Changelog

## 0.3.0
- LED Panel: flat square light for ceilings, walls and floors. Touching panels facing the same way share a look; the configurator applies to the whole group.
- LED Spot: small downlight. Colour wash makes panels and spots cast a coloured pool of light on the surface they face (soft and wide for panels, tighter for spots).
- Light Controller: pick a channel (dye colour) in the configurator and use it on the controller. It sends that look to every light on the channel within 48 blocks, and passes its redstone signal on to them, so one lever runs a whole room.
- Presets: 8 saved looks on each configurator. Click to load, shift-click to save, right-click to clear.
- New modes: Chase, Sparkle, Strobe, Breathe.

## 0.2.2
- Fixed strips at 0% brightness (or dimmed to off by redstone) still giving off block light. A strip that shows as off now gives no light.

## 0.2.1
- Fixed colour wash flickering where several strips light the same face (wash no longer writes depth).

## 0.2.0
- Renamed to Ledz. Mod id is now `ledz` (items `ledz:led_strip`, `ledz:led_configurator`). Strips placed with the old `lumend` id do not carry over.
- Fixed visible seams between joined strips: no end caps where strips meet, and the texture no longer darkens at each block joint.
- Removed the translucent halo shell around strips (it caused the joint lines). Use Colour wash for glow on surfaces.

## 0.1.1
- Colour wash option (configurator slider, 0-100%): strips cast a coloured glow onto nearby surfaces, fading out over 2.5 blocks. Vanilla block light stays white, this fakes the tint.

## 0.1.0
- LED Strip: edge-snapping strips, joined runs, per-strip breaking, support checks.
- LED Configurator: 2-4 stop gradients, Static/Scroll/Pulse/Rainbow, speed, brightness, reverse, block light, redstone modes, copy/apply.
