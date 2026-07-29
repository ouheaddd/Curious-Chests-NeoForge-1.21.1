# Changelog

## 0.3.1

- Replaced Java-painted container backgrounds with five independent editable 256 x 256 GUI PNG textures.
- Added `docs/GUI_TEXTURE_GUIDE.md` with exact visible sizes and slot coordinates.
- Implemented 256-count logical stacks for normally stackable items in Bottomless Chest slots, including save, dropped-item and menu support.
- Changed Ender Dispatch to move one stack every 50 ticks (2.5 seconds).
- Smoothed Collector Chest attraction and changed it to respect actual block collision shapes.
- Full walls now block collection, while genuinely open gaps in partial blocks can remain traversable.

## 0.3.0

- Renamed the project to **Curious Chests**.
- Changed the mod ID and resource namespace to `curiouschests`.
- Moved Java sources to `com.overyourhead.curiouschests`.
- Renamed the main and client bootstrap classes.
- Kept the five stationary chest mechanics and their saved inventories intact.
- Cleaned obsolete wearable and item-backed code from the source tree.
