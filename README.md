# Ledz - Customizable Led Lights/Strips

NeoForge mod for Minecraft 1.21.1 and 26.1.2. Stick LED strips on block edges, add LED panels and spots, and paint them all with gradients using the LED Configurator.

![Modes](docs/modes.gif)

*Static, Scroll, Pulse, Rainbow, Dim 40% and a redstone-controlled strip that is switched off.*

## LED Strip
- Use on a block face: the strip snaps to the nearest edge of that face. Up to 24 strips per block space (4 edges x 6 faces).
- Strips that touch end to end join into a **run**. The gradient spans the whole run, round corners and up walls.
- A new strip copies the look of a strip it touches, so runs keep going.
- Break one strip at a time. Strips pop off when their supporting block goes.

![Placements](docs/placements.png)

*All 12 edges of a block as one run, and strips up the corners of a pillar.*

## LED Panel and LED Spot
- Place on any face. Panels are flat squares, spots are small downlights.
- Touching panels facing the same way share a look; the configurator applies to the whole group.
- With Colour wash on, they cast a coloured pool of light on the surface they face.

![Spots](docs/spots.png)

![Panels](docs/panels.png)

## Light Controller
- Set a channel (dye colour) in the configurator, then use it on the controller: every light on that channel within 48 blocks takes the look.
- The controller passes its redstone signal to its lights. Set their redstone mode (e.g. On when powered) and one lever runs the room.
- Use with an empty hand to see its channel and how many lights it reaches. The side lens shows the channel colour.

![Controller](docs/controller.png)

## LED Configurator
- **Use in air**: open the editor. 2-4 colour stops (hex, RGB sliders or dye palette), live preview.
- **Use on a strip**: apply to its run. **Sneak-use**: copy that strip's look.
- **Use on a panel or spot**: apply to it and its group. **Use on a Light Controller**: send to its channel.
- Modes: Static, Scroll, Pulse, Rainbow, Chase, Sparkle, Strobe, Breathe. Speed, brightness, reverse.
- Presets: 8 saved looks per configurator. Click to load, shift-click to save, right-click to clear.
- Channel: none or one of 16 dye colours, for the Light Controller.
- Light: 0-15 real block light.
- Colour wash: 0-100%. Tints nearby walls, floors and ceilings with the strip colour, fading out over 2.5 blocks (vanilla light is colourless, this fakes it).
- Redstone: ignore, on when powered, off when powered, or brightness by signal strength.

![Configurator](docs/configurator.png)

## Lighting a room

![Light 0 vs Light 11](docs/dark-room-compare.png)

*Top: Light 0, the strips only glow. Bottom: Light set, the room is lit.*

| Colour wash 100%, Light 0 | Colour wash 70%, Light 11 |
|---|---|
| ![Colour wash](docs/colour-wash.png) | ![Colour wash lit](docs/colour-wash-lit.png) |

## Recipes
- 8 LED Strips: glowstone dust + glass pane + copper ingot (shapeless).
- LED Configurator: amethyst shard over copper / redstone / copper, copper below.
- 2 LED Panels: 3 LED strips over iron nugget / glass pane / iron nugget.
- 2 LED Spots: LED strip over iron nugget / glass pane / iron nugget.
- Light Controller: copper / amethyst shard / copper, redstone / comparator / redstone, 3 copper.

## Versions
- `main`: Minecraft 1.21.1 (NeoForge 21.1). Panels, spots, Light Controller, presets and the new modes (0.3.0) are 1.21.1 only for now.
- `mc/26.1.2`: Minecraft 26.1.2 (NeoForge 26.1.2).

## Build
`./gradlew build`. Textures: `python3 tools/textures.py` (needs Pillow).
