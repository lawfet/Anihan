# ANIHAN

**Anihan** is a top-down 2D Filipino-inspired farming adventure made with Java 17, Swing, AWT, and Graphics2D. It uses the artwork already included in `res/` and does not require a third-party game engine or external libraries.

## Run the game

### Requirements
- JDK 17 or newer (a JDK, not only a JRE)
- Visual Studio Code with the Extension Pack for Java, or Eclipse

### Fastest option on Windows
1. Clone or pull this repository.
2. Open the repository folder in VS Code.
3. Double-click `run.bat`, or run it from the VS Code terminal:

   ```bat
   run.bat
   ```

The script compiles the game into `bin/` and launches `engine.Main`. You can also compile without launching by running `compile.bat`.

### Run from the VS Code terminal
From the repository root:

```bat
if not exist bin mkdir bin
javac -Xlint:all -d bin src\engine\*.java src\managers\*.java src\states\*.java src\entities\*.java src\farming\*.java src\time\*.java src\inventory\*.java src\inputs\*.java src\validation\*.java
java -cp bin engine.Main
```

Keep the `res/` directory beside `bin/`. The game loads its maps, sprites, crops, UI, and animal artwork from this resource folder.

## Controls

| Input | Action |
|---|---|
| W / A / S / D or Arrow keys | Move |
| Shift + movement | Sprint (uses energy) |
| 1–9 | Select a hotbar tool or seed |
| Mouse wheel | Change selected hotbar slot |
| Space or left mouse click | Use selected tool |
| E or I | Open / close backpack |
| Click a crop in backpack | Equip that crop's seeds in the selected seed slot |
| Right-click a crop in backpack | Buy a seed pack |
| F | Sleep when standing at the farmhouse door |
| M | Open / close world map |
| Esc | Pause menu |

### Farming loop
1. Select the hoe in hotbar slot 1 and face an open grass tile. Press Space or left-click to till it.
2. Select a seed slot (5–9), face the tilled soil, and use the tool to plant.
3. Select the watering can (slot 2) and water the planted soil. Refill the can at a pond.
4. Watered crops grow when a new day begins. Return to the farmhouse door and press F to sleep.
5. Harvest mature crops with a seed slot selected, then sell goods at the shipping bin beside the farmhouse.
6. Collect pasture eggs and milk, gather wood, fish at ponds, buy seeds, and work toward farm restoration goals.

The first field contains sample crops so you can try harvesting immediately. Some progression systems unlock as you explore and gather materials.

## What's included

- Large island farm map with coastline, ponds, paths, fields, village buildings, orchard, and dock
- Animated player assembled from base, hairstyle, and tool sprite layers
- Multiple crop types with growth stages, tilling, watering, and harvesting
- Hotbar, backpack, seed purchasing, gold, health, energy, and watering-can UI
- Day/night lighting, seasons, sleep transition, and fireflies
- Roaming farm animals and collectible eggs and milk
- Fishing, tree chopping, shipping bin, orders, upgrades, and farm restoration progression
- Main menu, character hairstyle selection, options, credits, and pause menu
- Synthesized sound effects and ambient farm music (audio availability depends on the computer)

## Project structure

```
Anihan/
├── src/
│   ├── engine/       # Window, game loop, and rendering panel
│   ├── entities/     # Player and entity logic
│   ├── farming/      # Soil, crops, inventory progression
│   ├── inputs/       # Keyboard and mouse input
│   ├── inventory/    # Hotbar and backpack UI
│   ├── managers/     # Asset loading and world/tile management
│   ├── states/       # Menu, gameplay, and pause states
│   ├── time/         # Day/night clock
│   └── validation/   # Headless resource smoke test
├── res/              # Maps, tilesets, sprites, crops, UI and animals
├── compile.bat
├── run.bat
└── README.md
```

## Validation

A GitHub Actions workflow compiles the Java source on pushes and pull requests to `main`, then runs a headless smoke test for required game artwork and map resources.

To run the resource smoke test locally after compiling:

```bat
java -Djava.awt.headless=true -cp bin validation.AssetSmokeTest
```

## Notes

This is an actively developed playable prototype, not a finished commercial release. Some included assets are loaded and used by different systems; others are retained in `res/` for future content. The game is designed to run locally and offline after the repository and JDK are installed.
