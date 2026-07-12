# Brajnovic TD

Tower defense maze game — build towers to block enemies from reaching the finish line.

## Tech Stack

- **Java 21**
- **[libGDX](https://libgdx.com/)** `1.14.2` — cross-platform game framework (OpenGL rendering, Scene2D UI, Tiled map support, GLSL shaders)
- **[Ashley](https://github.com/libgdx/ashley)** `1.7.4` — Entity Component System
- **[gdx-ai](https://github.com/libgdx/gdx-ai)** `1.8.2` — A* pathfinding, finite-state machines (tower/enemy states)
- **Gradle** — build system (multi-module: `core` + `lwjgl3`)
- **LWJGL3** — desktop backend
- **Tiled** (`.tmx`) — level/grid editor
- **Python 3.12+** with **[Pillow](https://python-pillow.org/)** — only needed for the sprite tooling in `tools/` (see below), not for running or building the game

## Features

- 4 tower types (arrow, cannon, ice, poison) with AOE damage, slow, and poison DOT status effects
- Tower select/upgrade/sell system (levels 1–15, milestone bonuses)
- 3 levels (grass/desert/winter) chained together, each with wave scaling and boss waves
- Mouse and touch input modes (touch simulated on desktop, ready for a future Android build)
- OGG sound effects, particle effects, hit flash + tower outline shaders
- Croatian and English localization

## Project Structure

```
brajnovic-td/
├── core/          ← all game logic (platform-independent)
├── lwjgl3/        ← desktop launcher
├── assets/        ← everything the game loads
│   ├── data/         ← towers.json, enemies.json, levels/*.json
│   ├── maps/          ← Tiled .tmx level layouts
│   ├── sprites-src/   ← hand-authored sprite sheets (PNG + frame-rect JSON), loaded directly at runtime
│   ├── sounds/        ← OGG sound effects
│   ├── i18n/          ← .properties translation bundles (hr/en)
│   ├── shaders/       ← GLSL (status effect tint, tower outline)
│   ├── particles/     ← particle effect assets
│   └── ui/            ← Scene2D skin, icons
├── tools/         ← sprite pipeline scripts (Python)
├── build.gradle
└── settings.gradle
```

## Sprite Tooling

Enemies are hand-drawn in 5 directions (N, NE, E, SE, S); the remaining 3 (W, NW, SW) are generated
by horizontally flipping their mirrored counterparts:

```
pip install -r tools/requirements.txt
python tools/flip_sprite_directions.py orc_atlas
```

Reads/writes `assets/sprites-src/orc_atlas.png` + `.json` in place. Safe to re-run — already-generated
directions are skipped.

## How to Run

Prerequisite: JDK 21+ installed.

**From IntelliJ IDEA:**
Open the project as a Gradle project and run `Lwjgl3Launcher` (module `lwjgl3`).

**From the terminal:**

Windows:
```
gradlew.bat lwjgl3:run
```

Linux/macOS:
```
./gradlew lwjgl3:run
```

## Build

```
gradlew.bat lwjgl3:jar
```

Generates a runnable `.jar` in `lwjgl3/build/libs/`.
