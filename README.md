# Anihan

**Anihan** is a 2D Filipino farming adventure built with Java Swing, AWT, and Graphics2D.

This repository now contains a small playable foundation. It is intentionally dependency-free so it can be opened and run in VS Code or Eclipse with a JDK.

## Playable prototype

- Top-down movement with WASD or arrow keys
- Pixel-art-inspired farm tiles, trees, a bahay kubo, and a palengke stall
- Till soil, plant crops, water them, and harvest mature produce
- Four crops: palay, ube, kamote, and pineapple
- A day/night cycle with a night-time color overlay
- Sell harvested goods at the palengke
- Simple inventory, crop growth, and on-screen controls

## Requirements

- JDK 17 or newer
- No external libraries or game engine required

## Run in VS Code

1. Install a JDK 17+ and the Java extensions for VS Code.
2. Clone the repository:

   ```bash
   git clone https://github.com/lawfet/Anihan.git
   cd Anihan
   ```

3. Open the folder in VS Code.
4. Open `src/main/java/anihan/AnihanGame.java`.
5. Run the `AnihanGame` class using **Run Java**.

You can also run it from a terminal:

```bash
javac -d out src/main/java/anihan/AnihanGame.java
java -cp out anihan.AnihanGame
```

## Controls

| Key | Action |
|---|---|
| WASD / Arrow keys | Move |
| Space | Till nearby grass |
| Q | Plant selected seed |
| R | Water nearby planted crop |
| E | Harvest mature crop / sell at market |
| 1–4 | Select seed |
| I | Toggle inventory panel |
| H | Sleep and begin the next day |

Stand next to a tile to work on it. Stand beside the palengke stall and press **E** to sell all harvested goods.

## Project structure

```
Anihan/
├── src/main/java/anihan/
│   └── AnihanGame.java
├── .gitignore
└── README.md
```

## Planned next systems

- Multi-directional character sprite sheets and tool animations
- Larger layered tile maps and forest exploration
- Historical artifacts, lore journal, and shrine puzzles
- More crop stages, seasons, weather, pests, and crop protection
- Tool upgrades, crafting, quests, and order packaging
- Save/load system and original pixel-art/audio assets

## Project note

The current build is a playable prototype, not the complete game described in the design concept. The folklore and historical elements will be added as exploration and story systems. Historical objects and symbols will be handled with care and contextual research.
