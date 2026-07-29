# Implementation status — 0.3.1

## Shared chest lifecycle

- Every Curious Chests chest stores its inventory in the dropped `ItemStack`.
- A loose chest item cannot be opened and no passive ability runs from a normal inventory slot.
- A placed chest opens with right click.
- Curious Chests blocks, shulker boxes, bundles, and items carrying a container component cannot be nested.
- All five items appear in the dedicated Curious Chests creative tab.

## Bottomless Chest

- 63 deep slots; all normally stackable items can reach 256 units in one visible slot.
- Shared physical storage, not player-bound storage.
- Costs 7 raw experience points on every successful survival-mode opening.
- Creative-mode players are exempt.
- Opening is refused when the player has fewer than 7 experience points.

## Infernal Chest

- 9 input and 18 protected output slots.
- Processes recipes registered under Minecraft's normal `SMELTING` recipe type.
- No fuel required.
- One operation every 100 game ticks.
- If the output area cannot hold the complete result, the remainder is spawned in front of the chest.
- Item form is fire resistant.

## Ender Dispatch Chest

- 36 slots.
- When placed, moves one available stack every 50 ticks (2.5 seconds) into storage within 8 blocks.
- First fills storage already containing the exact item; otherwise uses nearest empty capacity.
- Skips Curious Chests blocks and furnace-type processing blocks.
- Emits portal particles and a teleport sound for successful transfers.

## Builder's Chest

- 45 slots.
- Scans server players within 16 blocks every two ticks.
- Remembers each player's selected item.
- When that selected stack is consumed completely, an exact replacement stack is moved from the chest into the same hotbar slot.
- Supplies multiple players without owner binding.

## Collector's Chest

- 36 slots.
- Scans an 8-block radius every tick for smooth motion.
- Valid dropped items are smoothly steered toward the chest rather than snapped by a sudden velocity change.
- Attraction uses real collision shapes: full walls block it, while an open ray through a partial block can pass.
- Items are inserted only after reaching the chest.
- Full chests do not pull items they cannot accept.

## Intentional limitations

- Placeholder block and item art is included. Five independent editable GUI PNG templates are included.
- Chest block models are static; lid opening animation is not implemented yet.
- Ender Dispatch currently targets block entities implementing vanilla `Container`; capability-only third-party storage can be added later.
- No configuration screen, upgrades, filters, accessory slots, portable opening, or mixin hooks are used in 0.3.1.
