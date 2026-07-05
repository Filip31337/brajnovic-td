# Brajnovic TD

Tower defense maze game, build towers to block enemies to reach finish line.

## Tech Stack

- **Java 21**
- **[libGDX](https://libgdx.com/)** `1.14.2` — cross-platform game framework (OpenGL rendering, Scene2D UI, Tiled map support)
- **[Ashley](https://github.com/libgdx/ashley)** `1.7.4` — Entity Component System (added, coming in V1)
- **[gdx-ai](https://github.com/libgdx/gdx-ai)** `1.8.2` — pathfinding, FSM, behavior trees, steering (added, coming in V1)
- **Gradle** — build system (multi-module: `core` + `lwjgl3`)
- **LWJGL3** — desktop backend
- **Tiled** (`.tmx`) — level/grid editor
- **Python 3.12+** with **[Pillow](https://python-pillow.org/)** — only needed for the sprite tooling in `tools/` (see below), not for running or building the game

## Project Structure

```
brajnovic-td/
├── core/          ← all game logic (platform-independent)
├── lwjgl3/        ← desktop launcher
├── assets/        ← everything the game loads (maps, JSON definitions, UI skin)
│   └── sprites-src/  ← hand-authored sprite sheets (PNG + frame-rect JSON), loaded directly at runtime
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
