# Architecture

`ChestKind` is the single source of truth for chest IDs and slot counts.

Placed chests use `SpecialChestBlockEntity`. When broken, `minecraft:container` and `minecraft:custom_name` components are copied to the dropped block item by loot tables. When placed again, `applyComponentsFromItemStack` restores supported block-entity components.

All active behavior is stationary and server-authoritative:

- `InfernalLogic` processes registered `SMELTING` recipes and ejects overflow in the block's facing direction.
- `DispatchLogic` searches loaded storage blocks within 8 blocks and prefers existing matching stacks.
- `BuilderSupplyLogic` tracks selected-hand snapshots for every server player within 16 blocks and refills a fully consumed stack.
- `CollectorLogic` pulls valid item entities toward the chest and inserts them only when they reach it.

A loose chest item never opens or ticks. No networking or accessory APIs are required.

`ModCreativeTabs` owns the dedicated creative tab. It iterates `ModItems.ITEMS.getEntries()`, so registered items appear automatically in registration order.

No mixins are required for 0.3.1. The package and config remain intentionally present for future narrow compatibility hooks.
