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

## Project Structure

```
brajnovic-td/
├── core/      ← all game logic (platform-independent)
├── lwjgl3/    ← desktop launcher
├── assets/    ← everything the game loads (maps, JSON definitions, UI skin)
├── build.gradle
└── settings.gradle
```

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
