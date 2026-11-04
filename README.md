# Lumen'd - Customizable Led Lights/Strips

NeoForge mod. Stick LED strips on block edges and paint them with gradients using the LED Configurator.

## LED Strip
- Use on a block face: the strip snaps to the nearest edge of that face. Up to 24 strips per block space (4 edges x 6 faces).
- Strips that touch end to end join into a **run**. The gradient spans the whole run, round corners and up walls.
- A new strip copies the look of a strip it touches, so runs keep going.
- Break one strip at a time. Strips pop off when their supporting block goes.

## LED Configurator
- **Use in air**: open the editor. 2-4 colour stops (hex, RGB sliders or dye palette), live preview.
- **Use on a strip**: apply to its run. **Sneak-use**: copy that strip's look.
- Modes: Static, Scroll, Pulse, Rainbow. Speed, brightness, reverse.
- Light: 0-15 real block light.
- Redstone: ignore, on when powered, off when powered, or brightness by signal strength.

## Recipes
- 8 LED Strips: glowstone dust + glass pane + copper ingot (shapeless).
- LED Configurator: amethyst shard over copper / redstone / copper, copper below.

## Build
`./gradlew build`. Textures: `python3 tools/textures.py` (needs Pillow).
