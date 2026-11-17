# Changelog

## 2.0.9+26.3

- Fix pause-screen settings button placement by detecting its icon row above the vertical midpoint.

## 2.0.8+26.3

- Add the armor config icon to the pause screen's icon row as well as the title screen.

## 2.0.7+26.3

- Fix title-screen mixin access to inherited `Screen` dimensions and widget registration.

## 2.0.6+26.3

- Fix the title-screen button mixin so it invokes the inherited widget-registration method on Screen.

## 2.0.5+26.3

- Added a main-menu icon button that opens the armor configuration screen.

## 2.0.4+26.3

- Accept CICADA 0.15.2 in Fabric's runtime metadata while compiling against the published 0.15.3+26.3 API, for installs whose CICADA JAR reports the older version.

## 2.0.3+26.3

- Ported the code and build to Minecraft 26.3, using Mojang's unobfuscated names, Java 25, Fabric Loader 0.19.5, and the 26.3 submit-based rendering APIs.
- Updated Fabric API, Mod Menu, CICADA, and Cardinal Components API dependencies for 26.3.
- Reworked the configuration screen and Fabric networking for the new version. A server and its clients must use this port to sync settings.
- Restored per-piece opacity sliders for armor, armor trims, elytra, shields, and head items, alongside visibility and glint controls.
- Added migration for legacy global and per-player configuration, preserving a backup of the original config file.
- Combat behavior, local player overrides, and server sync remain available. Armor and trim share the opacity slider in this screen; partial-opacity armor glint is limited by 26.3's combined render pipeline.
- The 26.3 compatibility work is based on the MIT-licensed community port by [0TrashPanda](https://github.com/0TrashPanda/show-me-your-skin), adapted for this branch.

## 1.11.3+1.21.6

- Added support for 1.21.6–1.21.8.
- Removed shield transparency options for now to make a release more feasible.
