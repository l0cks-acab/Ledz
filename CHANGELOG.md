# Changelog

## 0.2.0
- Renamed to Ledz. Mod id is now `ledz` (items `ledz:led_strip`, `ledz:led_configurator`). Strips placed with the old `lumend` id do not carry over.
- Fixed visible seams between joined strips: no end caps where strips meet, and the texture no longer darkens at each block joint.
- Removed the translucent halo shell around strips (it caused the joint lines). Use Colour wash for glow on surfaces.

## 0.1.1
- Colour wash option (configurator slider, 0-100%): strips cast a coloured glow onto nearby surfaces, fading out over 2.5 blocks. Vanilla block light stays white, this fakes the tint.

## 0.1.0
- LED Strip: edge-snapping strips, joined runs, per-strip breaking, support checks.
- LED Configurator: 2-4 stop gradients, Static/Scroll/Pulse/Rainbow, speed, brightness, reverse, block light, redstone modes, copy/apply.
