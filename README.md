# Curious Chests

NeoForge 1.21.1 project by **overyourhead**.

Curious Chests adds five specialized physical chests. They are not backpacks: a chest item stays sealed until it is placed in the world.

- **Bottomless Chest** — 63 deep slots; all normally stackable items can reach 256 per slot. Opening costs 7 experience points.
- **Infernal Chest** — 9 input + 18 output slots, fuel-free vanilla/modded furnace recipe processing. Overflow is ejected in front.
- **Ender Dispatch Chest** — 36-slot storage that automatically unloads into nearby storage within 8 blocks.
- **Builder's Chest** — 45-slot supply chest that restocks the selected hand of players within 16 blocks after a stack is consumed.
- **Collector's Chest** — 36-slot chest that visibly pulls dropped items from within 8 blocks.

## Core rules

- A loose chest item cannot be opened and has no active ability.
- Every ability runs only while the chest is placed.
- Breaking a chest preserves its inventory in the dropped item.
- Curious Chests blocks are shared physical storage, not player-bound Ender Chest storage.
- Curious Chests blocks, shulker boxes, bundles, and other item-backed containers cannot be nested.

## Build

Requirements: Java 21.

Windows:

```bat
gradlew.bat build
```

Linux/macOS:

```sh
./gradlew build
```

The resulting jar is written to `build/libs/`.

## Source layout

- `core/` — blocks, items, menus, block entities, capabilities, and the custom creative tab
- `common/` — gameplay blocks, block entities, menus, chest rules, and separated behavior logic
- `client/` — menu screen registration and PNG-backed GUI rendering
- `mixin/` — reserved compatibility area; no mixins are currently required

## Placeholder art

Current textures and models are deliberately simple placeholders. Every chest GUI has its own editable PNG under `assets/curiouschests/textures/gui/container/`; exact sizes and slot coordinates are documented in `docs/GUI_TEXTURE_GUIDE.md`.

## Current implementation notes

- Bottomless Chest uses 63 visible deep slots. All normally stackable items can reach 256 in one slot; saved block items serialize those logical stacks into safe internal layers.
- Ender Dispatch waits 50 ticks between transfers, targets nearby block entities implementing vanilla `Container`, skips furnaces and Curious Chests blocks, does not load chunks, and does not cross dimensions.
- Placed chests expose NeoForge's standard block item-handler capability for hopper and pipe integration.
- All item registration entries are automatically shown in the dedicated Curious Chests creative tab in registration order.
- Detailed behavior and test steps are in `docs/IMPLEMENTATION_STATUS.md` and `docs/BUILD_AND_TEST.md`.
