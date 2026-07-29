# GUI texture guide

Curious Chests no longer paints its container panels or slot frames in Java.
Each chest screen reads a separate editable PNG from:

```text
src/main/resources/assets/curiouschests/textures/gui/container/
```

## Files

| Chest | PNG | Visible GUI area |
|---|---|---:|
| Bottomless Chest | `bottomless.png` | 176 x 240 px |
| Infernal Chest | `infernal.png` | 176 x 214 px |
| Ender Dispatch Chest | `ender_dispatch.png` | 176 x 186 px |
| Builder's Chest | `builders.png` | 176 x 204 px |
| Collector's Chest | `collectors.png` | 176 x 186 px |

Every file uses a **256 x 256 transparent canvas**. Minecraft displays the top-left
176-pixel-wide region down to the listed height. Keep the file name and canvas
size unchanged; everything else in the PNG may be repainted.

Java now renders only the title text, the `Inventory` label, items and tooltips.
The panel, slot frames, flames, portals, decorative art and every other visual
part belong in the PNG.

## Slot coordinates

Coordinates below are the top-left corner of the 16 x 16 item area. A normal
slot frame is usually drawn one pixel above and to the left, making an 18 x 18
frame.

### Bottomless Chest

- Chest: 9 columns x 7 rows
- First item position: `x=8, y=18`
- Player inventory first row: `x=8, y=157`
- Hotbar: `x=8, y=215`

### Infernal Chest

- Input: 9 slots, item positions from `x=8, y=18`
- Output row 1: `x=8, y=72`
- Output row 2: `x=8, y=90`
- Player inventory first row: `x=8, y=130`
- Hotbar: `x=8, y=188`
- The area between input and output is intentionally empty for a custom furnace,
  lava, grille or progress illustration.

### Ender Dispatch Chest

- Chest: 9 columns x 4 rows
- First item position: `x=8, y=18`
- Player inventory first row: `x=8, y=103`
- Hotbar: `x=8, y=161`

### Builder's Chest

- Chest: 9 columns x 5 rows
- First item position: `x=8, y=18`
- Player inventory first row: `x=8, y=121`
- Hotbar: `x=8, y=179`

### Collector's Chest

- Chest: 9 columns x 4 rows
- First item position: `x=8, y=18`
- Player inventory first row: `x=8, y=103`
- Hotbar: `x=8, y=161`

## Workflow

1. Open the desired PNG in a pixel-art editor.
2. Keep all slot item positions unchanged unless the Java menu coordinates are
   changed at the same time.
3. Paint the entire visible panel and decorative elements directly in the PNG.
4. Export as RGBA PNG without resizing.
5. Replace the original file and reload resources with `F3 + T` during testing.
