# Atlas Client

A client-only Fabric visual-customization interface for **Minecraft 1.21.11**, styled as a smooth deep-navy dashboard with electric-blue gradients and rounded panels.

## Controls

- Press **Right Shift** to open or close Atlas; Escape closes it without passing the keypress through to the game.
- Use the sidebar to move between Overview, Settings, Visuals, HUD, Profiles, Configs, and About.
- The Overview's center intentionally contains no module rows. Scroll over it to smoothly move between the empty category panes for palettes, surfaces, lighting, typography, layout, and feedback.
- Settings, Visuals, and HUD pages contain interface-only toggles; no gameplay modules are included.

## Build

The project targets Minecraft `1.21.11`, Fabric Loader `0.20.1`, Fabric API `0.141.6+1.21.11`, and Fabric Loom `1.19-SNAPSHOT`.

**Windows quick build:** double-click `start.bat`. If you downloaded only that file, it refreshes the full branch into `%USERPROFILE%\AtlasClientBuild` on each run so cached source cannot hide fixes; the downloaded Java 25 runtime is retained. It then uses an installed Java 25 JDK or downloads a portable Temurin JDK 25 build runtime, runs a clean Gradle build, and copies the finished mod jar to `dist/AtlasClient-1.0.0.jar` inside the project folder. Gradle downloads its wrapper and mod dependencies as needed. The first run requires an internet connection and can take a while.

**Manual build:** with JDK 25 installed (the mod is compiled for Java 21), run `./gradlew build` (PowerShell: `./gradlew.bat build`). The produced mod jar is in `build/libs/`.

Appearance controls are client-side and stay in memory for the current game session. Profiles and config import/export are intentionally left for later. The Right Shift toggle is edge-triggered, so holding the key cannot repeatedly open or close the screen.
