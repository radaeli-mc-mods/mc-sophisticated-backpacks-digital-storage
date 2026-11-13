# Changelog

## [0.6.0] - 2026-11-13

### Added

- Added JEI integration for the Remote Crafting Upgrade. The `+` button in the upgrade tab now pulls items from the linked network when available.
- Added backpack color variants, one for each supported network: AE2, Refined Storage, Tom's Simple Storage and SCC. Craft a backpack with the network's marker item to recolor it; the contents are kept.

### Changed

- Stabilized support for Minecraft 26.1.2.

### Fixed

- Fixed the linking animations on 26.1.2.

## [0.6.0-beta.1] - 2026-11-12

### Added

- Added NeoForge support for Minecraft 26.1.2.

### Changed

- Migrated the project to a multiloader codebase.

### Known issues

- Linking animations on 26.1.2 are not working properly

## [0.5.0] - 2026-11-08

### Added

- Added Remote Crafting, which fills empty crafting slots with ingredients from your backpack and linked network for a recipe you choose.

## [0.4.1] - 2026-11-01

### Fixed

- Fixed startup crashes on Forge 1.20.1, including installations with only the required mods.

## [0.4.0] - 2026-10-31

### Changed

- Updated remote upgrade visuals with network-colored icons and a brief linking animation.

## [0.3.0] - 2026-10-30

### Added

- Added Super Cool Calculator 1.5.3 support for remote upgrades on NeoForge 1.21.1.

### Fixed

- Fixed Stored Content filters in Pickup and Magnet to match each destination independently.
- Fixed upgrade filter settings being lost after synchronization.
- Restored missing filter button icons.
- Fixed source priority buttons to update immediately.
- Restored the allowlist and blocklist selector in the Remote Feeding Upgrade.

## [0.2.0] - 2026-10-15

### Added

- Forge support for Minecraft 1.20.1, including all eight remote storage upgrades.

### Changed

- Remote Refill and Remote Alchemy now uses the native Sophisticated filter grid.

## [0.1.0] - 2026-10-12

### Added

- Remote Deposit, Restock, Pickup, Magnet, Refill, Feeding, Alchemy, and Pump upgrades for Sophisticated Backpacks.
- Universal item upgrades that link to Applied Energistics 2, Refined Storage, or Tom's Simple Storage, respecting each network's range and access rules.
- Configurable filters and network or backpack priority for collection, replenishment, consumption, and fluid transfers.
- Remote fluid transfers with AE2 and Refined Storage, including containers, nearby tanks, and world fluids.
- Crafting an upgrade by itself removes its network link while keeping its settings.
- A dedicated creative tab, custom upgrade textures, and English and Brazilian Portuguese translations.
