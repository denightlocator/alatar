# Atlas Client

A client-only Fabric visual-customization interface for **Minecraft 1.21.11**, styled as a polished blue-gradient dashboard.

## Controls

- Press **Right Shift** to open or close Atlas; Escape closes it.
- Use the sidebar to move between Overview, Settings, Visuals, HUD, Profiles, Configs, and About.
- The Overview contains appearance groups for palettes, surfaces, lighting, typography, layout, and visual feedback. Controls change only the dashboard’s appearance.
- Type to search visual settings. Backspace edits the query and Delete clears it.
- Click an appearance option to select or toggle it. Scroll over a group to browse its options. The detail panel includes intensity and treatment controls.

## Build

The project targets Minecraft `1.21.11`, Fabric Loader `0.20.1`, Fabric API `0.141.6+1.21.11`, and Fabric Loom `1.19-SNAPSHOT`.

**Windows quick build:** double-click `start.bat`. If you downloaded only that file, it fetches the full branch into `%USERPROFILE%\AtlasClientBuild` first. It then uses an installed Java 21 JDK or downloads a portable Temurin JDK 21, runs a clean Gradle build, and copies the finished mod jar to `dist/AtlasClient-1.0.0.jar` inside the project folder. Gradle downloads its wrapper and mod dependencies as needed. The first run requires an internet connection and can take a while.

**Manual build:** with JDK 21 installed, run `./gradlew build` (PowerShell: `./gradlew.bat build`). The produced mod jar is in `build/libs/`.

Appearance controls are client-side and stay in memory for the current game session. Profiles and config import/export are intentionally left for later. The Right Shift toggle is edge-triggered, so holding the key cannot repeatedly open or close the screen.
