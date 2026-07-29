# Build and test

## Requirements

- 64-bit Java 21
- Internet access on the first Gradle run

## Build

Windows:

```bat
gradlew.bat build
```

Linux/macOS:

```sh
chmod +x gradlew
./gradlew build
```

The distributable mod jar is generated in `build/libs/`.

## Development client

Windows:

```bat
gradlew.bat runClient
```

Linux/macOS:

```sh
./gradlew runClient
```

## Manual smoke-test checklist

1. Verify the five chests appear only in the dedicated Curious Chests creative tab and in item registration order.
2. Put items into each chest, break it, place it again, and verify the inventory survives.
3. Verify a loose chest item cannot be opened and performs no ability.
4. Open Bottomless Chest with at least 7 experience points and confirm exactly 7 points are removed.
5. Attempt to open Bottomless Chest with fewer than 7 points and confirm opening is denied.
6. Confirm Bottomless Chest has 63 slots and that multiple ordinary stacks merge into one visible count of 256; also test an item that normally stacks to 16.
7. Put smeltable items in Infernal input slots and verify output appears without fuel.
8. Fill all 18 Infernal output slots and verify new results are ejected from the front without stopping input processing.
9. Fill Ender Dispatch Chest, place storage blocks within 8 blocks, and verify it waits about 2.5 seconds between stack transfers and prioritizes matching contents.
10. Consume the selected stack while standing within 16 blocks of Builder's Chest and verify an exact replacement stack appears.
11. Repeat Builder's Chest testing with multiple players in range.
12. Drop items at different distances within 8 blocks of Collector's Chest and verify they move smoothly toward it before insertion.
13. Put a full wall between an item and Collector's Chest and verify it is ignored; then test an actual open gap in a partial collision shape.
14. Fill Collector's Chest and verify items it cannot accept are not pulled indefinitely.
15. Replace one GUI PNG, reload resources, and verify the custom art appears without Java changes.
16. Test server restart, explosion drops, hopper insertion/extraction, modded smelting recipes, and full inventories.

## Packaging note

Generated folders such as `.gradle`, `.idea`, `build`, and `run` are intentionally excluded from the source archive. Gradle and the IDE create them locally.
