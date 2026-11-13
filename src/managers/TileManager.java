package managers;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * TileManager builds and renders the island farm world: coastline, ponds,
 * winding dirt roads, crop plots, fences, village buildings, an orchard, a
 * fishing dock and animated water. The world is generated deterministically so
 * every play session is identical.
 */
public final class TileManager {

    public static final int TILE_SIZE = 32; // Scaled 2x from 16x16 tileset

    // Tile type identifiers
    public static final int TILE_GRASS = 0;
    public static final int TILE_FLOWERS = 1;
    public static final int TILE_DIRT = 2;
    public static final int TILE_WATER = 3;
    public static final int TILE_FENCE_H = 4;
    public static final int TILE_FENCE_V = 5;
    public static final int TILE_HOUSE_WALL = 6;
    public static final int TILE_HOUSE_ROOF = 7;
    public static final int TILE_HOUSE_DOOR = 8;
    public static final int TILE_SHIPPING_BIN = 9;
    public static final int TILE_FARM_PLOT = 10;
    public static final int TILE_TREE = 11;
    public static final int TILE_DOCK = 12;

    private int mapWidth;
    private int mapHeight;
    private int[][] mapTiles;
    private boolean[][] reserved;
    private boolean[][] pond;
    private boolean[][] buildingCollision;

    private final AssetManager assetManager;
    private int animationTick = 0;
    private BufferedImage ground;
    private final List<Scenery> objects = new ArrayList<>();

    // Village buildings, each anchored by the center of its bottom edge.
    // Every sprite here is complete artwork (no cropped wall/roof fragments).
    private static final BuildingSpec[] BUILDINGS = {
        // Cottage row facing the south village lane.
        new BuildingSpec("ts_cottage_blue", 45, 33, 2, true),
        new BuildingSpec("ts_cottage_green", 50, 33, 2, true),
        new BuildingSpec("ts_cottage_orange", 55, 33, 2, true),
        new BuildingSpec("ts_cottage_red", 62, 33, 2, true),
        new BuildingSpec("ts_cottage_purple", 24, 31, 2, true),
        // Civic buildings around the market square.
        new BuildingSpec("arch_hall", 50, 23, 2, false),
        new BuildingSpec("blue_cottage", 58, 28, 2, false),
        new BuildingSpec("ts_cottage_green", 42, 27, 2, true),
        new BuildingSpec("ts_cottage_orange", 61, 20, 2, true),
        // Outlying homes, farm workshop and dock buildings.
        new BuildingSpec("house_blue2", 34, 22, 3, true),
        new BuildingSpec("house_blue3", 57, 21, 3, true),
        new BuildingSpec("house_blue4", 66, 30, 3, true),
        new BuildingSpec("house_brown", 64, 23, 3, true),
        new BuildingSpec("small_shed", 27, 19, 3, true),
        new BuildingSpec("small_shed", 45, 41, 3, true),
        new BuildingSpec("small_shed", 49, 41, 3, true),
        new BuildingSpec("tall_house", 101, 43, 2, true),
        new BuildingSpec("watch_tower", 126, 31, 2, true),
        new BuildingSpec("blue_house", 113, 43, 2, true),
        new BuildingSpec("blue_tower", 139, 53, 2, true),
        new BuildingSpec("arch_gate", 95, 47, 2, false),
        new BuildingSpec("blue_cottage_alt", 104, 58, 2, true),
        new BuildingSpec("blue_cottage_small", 116, 58, 2, true),
        new BuildingSpec("tower_blue", 123, 76, 2, true),
        new BuildingSpec("ts_cottage_purple", 55, 78, 2, true),
        new BuildingSpec("ts_cottage_red", 65, 78, 2, true)
    };

    private static final class BuildingSpec {
        final String name;
        final int tileX;
        final int bottomTileY;
        final int scale;
        final boolean solid;

        BuildingSpec(String name, int tileX, int bottomTileY, int scale, boolean solid) {
            this.name = name;
            this.tileX = tileX;
            this.bottomTileY = bottomTileY;
            this.scale = scale;
            this.solid = solid;
        }
    }

    private int dockEndX;
    private int dockEndY;

    private static final int[][] EGG_SPOTS = {{31, 35}, {35, 31}, {40, 36}};
    private static final int[][] MILK_SPOTS = {{33, 33}, {38, 34}, {30, 37}};
    private final List<Pickup> pickups = new ArrayList<>();

    private static final class Pickup {
        final int tileX;
        final int tileY;
        final String type;

        Pickup(String type, int tileX, int tileY) {
            this.type = type;
            this.tileX = tileX;
            this.tileY = tileY;
        }
    }

    private static class Scenery {
        final BufferedImage[] frames;
        final int x, y, depth, phase, scale;

        Scenery(BufferedImage[] frames, int centerX, int bottomY, int depth, int phase, int scale) {
            this.frames = frames;
            int width = frames[0].getWidth();
            int height = frames[0].getHeight();
            this.x = centerX - width * scale / 2;
            this.y = bottomY - height * scale;
            this.depth = depth;
            this.phase = phase;
            this.scale = scale;
        }
    }

    public TileManager(AssetManager assetManager) {
        this.assetManager = assetManager;
        generateMap();
        initPickups();
        buildGround();
        buildObjects();
    }

    /** Daily pasture produce: fresh eggs and milk waiting to be gathered. */
    private void initPickups() {
        pickups.clear();
        for (int[] spot : EGG_SPOTS) pickups.add(new Pickup("egg", spot[0], spot[1]));
        for (int[] spot : MILK_SPOTS) pickups.add(new Pickup("milk", spot[0], spot[1]));
    }

    /** Refills the pasture with eggs and milk after the player sleeps. */
    public void resetPickups() {
        initPickups();
    }

    /**
     * Collects a pasture item under the player's feet.
     * @return "egg", "milk", or null when there is nothing to pick up.
     */
    public String tryCollectPickup(int worldX, int worldY) {
        int tx = Math.floorDiv(worldX, TILE_SIZE);
        int ty = Math.floorDiv(worldY, TILE_SIZE);
        for (int i = 0; i < pickups.size(); i++) {
            Pickup pickup = pickups.get(i);
            if (pickup.tileX == tx && pickup.tileY == ty) {
                pickups.remove(i);
                return pickup.type;
            }
        }
        return null;
    }

    // ==========================================
    // WORLD GENERATION
    // ==========================================
    private void generateMap() {
        loadLandLayout();
        // Carve broad, winding channels before roads and buildings are placed.
        // This turns the large landmass into connected islands like the
        // archipelago reference instead of adding a few tiny dots offshore.
        carveChannels();
        reserved = new boolean[mapHeight][mapWidth];
        pond = new boolean[mapHeight][mapWidth];
        Random rng = new Random(73_421L);

        // Three ponds, with a tree-free rim so they read clearly from afar.
        carvePond(15, 34, 5, 4);
        carvePond(64, 39, 4, 3);
        carvePond(33, 45, 3, 2);

        // Main east-west road with a soft bend.
        for (int x = 5; x <= mapWidth - 18; x++) {
            int roadY = 24 + (int) Math.round(1.5 * Math.sin((x - 6) * 0.14)
                    + 0.7 * Math.sin((x - 2) * 0.05));
            setIfLand(x, roadY, TILE_DIRT);
            setIfLand(x, roadY + 1, TILE_DIRT);
        }

        // North-south spine through the farm and village.
        for (int y = 7; y < mapHeight - 4; y++) {
            setIfLand(37, y, TILE_DIRT);
            setIfLand(38, y, TILE_DIRT);
        }

        // Farm driveway, field gate, and short branch roads.
        carveRoad(30, 15, 30, 17);
        carveRoad(30, 17, 38, 17);
        carveRoad(20, 17, 20, 23);
        carveRoad(12, 24, 12, 26);
        carveRoad(27, 19, 27, 23);
        carveRoad(24, 26, 24, 31);

        // Village market square, cottage lane and connecting footpaths.
        fillDirtRect(43, 23, 59, 29);
        fillDirtRect(43, 35, 66, 36);
        carveRoad(47, 26, 47, 36);
        carveRoad(52, 26, 52, 36);
        carveRoad(58, 29, 58, 36);
        carveRoad(66, 31, 66, 36);
        carveRoad(45, 34, 45, 36);
        carveRoad(50, 34, 50, 36);
        carveRoad(55, 34, 55, 36);
        carveRoad(62, 34, 62, 36);
        carveRoad(39, 32, 39, 36);
        carveRoad(65, 29, 65, 36);
        carveRoad(34, 23, 34, 25);
        carveRoad(57, 22, 57, 24);
        carveRoad(61, 21, 61, 24);
        carveRoad(64, 24, 64, 25);
        carveRoad(69, 17, 69, 24);
        // Path from the village lane to the dock, watch tower and bait shed.
        carveRoad(45, 37, 45, 40);
        carveRoad(49, 37, 49, 40);
        carveRoad(37, 41, 40, 41);
        carveRoad(40, 41, 50, 41);

        carvePond(126, 88, 9, 6);
        carvePond(76, 84, 6, 4);

        // Build real, walkable links between every separated landmass.
        // This must happen after all large channels and ponds are carved.
        connectAllLandmasses();

        carveRoad(96, 24, 96, 71);
        carveRoad(37, 70, 123, 70);
        carveRoad(123, 70, 123, 77);
        carveRoad(55, 70, 55, 79);
        carveRoad(65, 70, 65, 79);
        fillDirtRect(97, 45, 119, 49);
        carveRoad(101, 43, 101, 49);
        carveRoad(113, 43, 113, 49);
        carveRoad(104, 58, 104, 62);
        carveRoad(116, 58, 116, 62);
        carveRoad(96, 62, 120, 62);
        carveRoad(126, 24, 126, 32);
        carveRoad(139, 24, 139, 54);
        carveRoad(80, 63, 96, 63);
        reserveRect(80, 54, 94, 64);
        reserveRect(59, 58, 66, 64);
        reserveRect(97, 45, 120, 50);

        // Crop field boundaries; gates at the south side.
        for (int x = 16; x <= 25; x++) {
            if (x != 20 && x != 21) {
                mapTiles[9][x] = TILE_FENCE_H;
                mapTiles[16][x] = TILE_FENCE_H;
            }
            reserved[9][x] = true;
            reserved[16][x] = true;
        }
        for (int y = 9; y <= 16; y++) {
            if (y != 12 && y != 13) {
                mapTiles[y][16] = TILE_FENCE_V;
                mapTiles[y][25] = TILE_FENCE_V;
            }
            reserved[y][16] = true;
            reserved[y][25] = true;
        }
        for (int y = 10; y <= 15; y++) {
            for (int x = 17; x <= 24; x++) {
                reserved[y][x] = true;
            }
        }

        // Farmhouse footprint. The clean tileset cottage is 4 tiles wide by
        // 7 tiles tall once scaled, with its doorway facing the porch.
        for (int y = 6; y <= 12; y++) {
            for (int x = 29; x <= 32; x++) {
                mapTiles[y][x] = TILE_HOUSE_WALL;
                reserved[y][x] = true;
            }
        }
        for (int y = 13; y <= 15; y++) {
            mapTiles[y][30] = TILE_HOUSE_DOOR;
            reserved[y][30] = true;
        }
        reserveRect(28, 5, 33, 16);

        // Shipping bin beside the porch.
        mapTiles[15][34] = TILE_SHIPPING_BIN;
        mapTiles[16][34] = TILE_HOUSE_WALL;
        reserved[15][34] = true;
        reserved[16][34] = true;

        // Fenced animal pasture.
        for (int y = 29; y <= 38; y++) {
            for (int x = 28; x <= 42; x++) {
                reserved[y][x] = true;
            }
        }
        for (int x = 28; x <= 42; x++) {
            if (x != 37 && x != 38) {
                mapTiles[29][x] = TILE_FENCE_H;
                mapTiles[38][x] = TILE_FENCE_H;
            }
        }
        for (int y = 29; y <= 38; y++) {
            if (y != 33 && y != 34) {
                mapTiles[y][28] = TILE_FENCE_V;
                mapTiles[y][42] = TILE_FENCE_V;
            }
        }
        // East pasture gate.
        mapTiles[33][42] = TILE_GRASS;
        mapTiles[34][42] = TILE_GRASS;

        // Reserve village buildings before scattering trees. Footprints are
        // derived from the real sprite sizes so nothing pokes through a roof.
        for (BuildingSpec spec : BUILDINGS) {
            BufferedImage sprite = assetManager.getBuilding(spec.name);
            if (sprite == null) continue;
            int widthTiles = (sprite.getWidth() * spec.scale + TILE_SIZE - 1) / TILE_SIZE;
            int heightTiles = (sprite.getHeight() * spec.scale + TILE_SIZE - 1) / TILE_SIZE;
            int halfWidth = widthTiles / 2 + 1;
            reserveRect(spec.tileX - halfWidth, spec.bottomTileY - heightTiles - 1,
                    spec.tileX + halfWidth, spec.bottomTileY + 1);
        }
        reserveRect(65, 27, 72, 34); // windmill
        reserveRect(26, 12, 34, 18); // farmhouse apron
        reserveRect(43, 21, 55, 27); // plaza
        reserveRect(53, 7, 64, 16);  // orchard

        // A little wooden dock on the south shore.
        buildDock();

        // Orchard rows.
        for (int y : new int[]{8, 11, 14}) {
            for (int x : new int[]{40, 43, 46, 49, 54, 57, 60, 63}) {
                if (tileAt(x, y) == TILE_GRASS) placeTree(x, y);
            }
        }

        for (int y : new int[]{34, 38, 42}) {
            for (int x : new int[]{76, 80, 84, 88}) {
                if (tileAt(x, y) == TILE_GRASS) placeTree(x, y);
            }
        }
        plantGrove(110, 83, 9, 7, 0.60f, rng);
        plantGrove(53, 93, 12, 5, 0.55f, rng);
        plantGrove(140, 68, 5, 13, 0.55f, rng);

        // Coastal forest ring: pines lean toward the sea.
        for (int y = 2; y < mapHeight - 2; y++) {
            for (int x = 2; x < mapWidth - 2; x++) {
                if (mapTiles[y][x] != TILE_GRASS || reserved[y][x]) continue;
                if (isCoastal(x, y) && rng.nextFloat() < 0.62f) {
                    placeTree(x, y, rng.nextFloat() < 0.62f ? "pine" : "tree");
                }
            }
        }

        // Inland groves give the island its wooded, lived-in feeling.
        plantGrove(9, 14, 4, 6, 0.65f, rng);
        plantGrove(10, 42, 6, 4, 0.62f, rng);
        plantGrove(68, 35, 4, 7, 0.60f, rng);
        plantGrove(24, 45, 7, 3, 0.58f, rng);
        plantGrove(50, 43, 5, 4, 0.52f, rng);

        // Scattered inland trees.
        for (int i = 0; i < 390; i++) {
            int x = 5 + rng.nextInt(mapWidth - 10);
            int y = 5 + rng.nextInt(mapHeight - 10);
            if (mapTiles[y][x] != TILE_GRASS || reserved[y][x]) continue;
            if (isNearPath(x, y, 1)) continue;
            if (hasNeighborTree(x, y)) continue;
            placeTree(x, y, rng.nextFloat() < 0.3f ? "pine" : "tree");
        }

        // Flower patches across open grass.
        for (int i = 0; i < 1_050; i++) {
            int x = rng.nextInt(mapWidth);
            int y = rng.nextInt(mapHeight);
            if (mapTiles[y][x] == TILE_GRASS && !reserved[y][x] && rng.nextFloat() < 0.24f) {
                mapTiles[y][x] = TILE_FLOWERS;
            }
        }

        // The roaming pasture stays clear and green.
        for (int y = 30; y <= 37; y++) {
            for (int x = 29; x <= 41; x++) {
                if (mapTiles[y][x] == TILE_FLOWERS) mapTiles[y][x] = TILE_GRASS;
            }
        }
    }

    private void loadLandLayout() {
        try (java.util.Scanner layout = new java.util.Scanner(assetManager.openResource("maps/farm_map.txt"))) {
            layout.useLocale(java.util.Locale.ROOT);
            mapWidth = layout.nextInt();
            mapHeight = layout.nextInt();
            if (mapWidth < 80 || mapHeight < 56 || mapWidth > 512 || mapHeight > 512) {
                throw new IllegalArgumentException("Map dimensions must be between 80x56 and 512x512");
            }
            mapTiles = new int[mapHeight][mapWidth];
            for (int[] row : mapTiles) java.util.Arrays.fill(row, TILE_WATER);
            while (layout.hasNext()) {
                double cx = layout.nextDouble(), cy = layout.nextDouble();
                double rx = layout.nextDouble(), ry = layout.nextDouble(), power = layout.nextDouble();
                if (rx <= 0 || ry <= 0 || power <= 0) throw new IllegalArgumentException("Invalid land shape");
                for (int y = 1; y < mapHeight - 1; y++) {
                    for (int x = 1; x < mapWidth - 1; x++) {
                        double nx = (x - cx) / rx, ny = (y - cy) / ry;
                        double angle = Math.atan2(ny, nx);
                        double wobble = 0.050 * Math.sin(angle * 3.0 + 1.2)
                                + 0.032 * Math.sin(angle * 5.0 - 0.8)
                                + 0.018 * Math.sin(angle * 9.0 + 2.1)
                                + 0.011 * Math.sin(angle * 13.0 + 0.4);
                        double radius = Math.pow(Math.pow(Math.abs(nx), power)
                                + Math.pow(Math.abs(ny), power), 1.0 / power);
                        if (radius < 1.0 + wobble) mapTiles[y][x] = TILE_GRASS;
                    }
                }
            }
        } catch (java.io.IOException | java.util.NoSuchElementException exception) {
            throw new IllegalStateException("Cannot read maps/farm_map.txt", exception);
        }
    }

    /**
     * Cuts two broad, gently winding sea channels through the island.
     * The channels create separate districts and coves while preserving
     * enough land for the existing farm, village, forest and beach areas.
     */
    /**
     * Carves wide, winding sea channels through the archipelago.
     * The north/south channel separates the west and east districts; the
     * east/west channel separates the northern village from southern coves.
     */
    private void carveChannels() {
        // East-west sea channel. Vary the centerline so it feels like a river
        // mouth, not a ruler-straight trench. Five tiles wide at its broadest.
        for (int x = 5; x < mapWidth - 5; x++) {
            double wave = Math.sin(x * 0.061) * 4.0
                    + Math.sin(x * 0.137 + 0.8) * 1.8;
            int centerY = 58 + (int) Math.round(wave);
            int halfWidth = 2 + (int) Math.round((1.0 + Math.sin(x * 0.09)) * 0.5);
            for (int y = centerY - halfWidth; y <= centerY + halfWidth; y++) {
                if (x > 1 && y > 1 && x < mapWidth - 2 && y < mapHeight - 2) {
                    mapTiles[y][x] = TILE_WATER;
                }
            }
        }

        // North-south channel, bending gently toward the east near the south.
        for (int y = 5; y < mapHeight - 5; y++) {
            double wave = Math.sin(y * 0.073 + 0.4) * 3.4
                    + Math.sin(y * 0.151) * 1.4;
            int centerX = 91 + (int) Math.round(wave);
            int halfWidth = 2 + (int) Math.round((1.0 + Math.sin(y * 0.11 + 1.0)) * 0.5);
            for (int x = centerX - halfWidth; x <= centerX + halfWidth; x++) {
                if (x > 1 && y > 1 && x < mapWidth - 2 && y < mapHeight - 2) {
                    mapTiles[y][x] = TILE_WATER;
                }
            }
        }

        // Small turquoise coves open into the outer coastline.
        carveEllipse(13, 76, 5, 8, TILE_WATER);
        carveEllipse(145, 16, 5, 5, TILE_WATER);
        carveEllipse(145, 101, 6, 5, TILE_WATER);
        carveEllipse(71, 7, 5, 3, TILE_WATER);
    }

    /**
     * Finds each disconnected land region and joins it to the nearest region
     * with a three-tile-wide timber bridge. The search uses the actual carved
     * coastline, so bridge endpoints always touch walkable land.
     */
    private void connectAllLandmasses() {
        int totalTiles = mapWidth * mapHeight;
        while (true) {
            int[][] component = labelLandComponents();
            int componentCount = 0;
            for (int[] row : component) {
                for (int value : row) componentCount = Math.max(componentCount, value);
            }
            if (componentCount <= 1) return;

            int[] bestPath = null;
            for (int source = 1; source <= componentCount; source++) {
                int[] previous = new int[totalTiles];
                java.util.Arrays.fill(previous, -2);
                java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();

                for (int y = 0; y < mapHeight; y++) {
                    for (int x = 0; x < mapWidth; x++) {
                        if (component[y][x] == source) {
                            int index = y * mapWidth + x;
                            previous[index] = -1;
                            queue.addLast(index);
                        }
                    }
                }

                int target = -1;
                while (!queue.isEmpty() && target < 0) {
                    int current = queue.removeFirst();
                    int x = current % mapWidth;
                    int y = current / mapWidth;
                    int[] dx = {1, -1, 0, 0};
                    int[] dy = {0, 0, 1, -1};
                    for (int direction = 0; direction < 4; direction++) {
                        int nx = x + dx[direction];
                        int ny = y + dy[direction];
                        if (nx < 0 || ny < 0 || nx >= mapWidth || ny >= mapHeight) continue;
                        int next = ny * mapWidth + nx;
                        if (previous[next] != -2) continue;
                        if (component[ny][nx] != 0 && component[ny][nx] != source) {
                            previous[next] = current;
                            target = next;
                            break;
                        }
                        previous[next] = current;
                        queue.addLast(next);
                    }
                }

                if (target >= 0) {
                    java.util.ArrayList<Integer> reversed = new java.util.ArrayList<>();
                    int cursor = target;
                    while (cursor >= 0) {
                        reversed.add(cursor);
                        cursor = previous[cursor];
                    }
                    int[] path = new int[reversed.size()];
                    for (int i = 0; i < reversed.size(); i++) {
                        path[i] = reversed.get(reversed.size() - 1 - i);
                    }
                    if (bestPath == null || path.length < bestPath.length) bestPath = path;
                }
            }

            if (bestPath == null) {
                throw new IllegalStateException("Unable to connect all Anihan island regions.");
            }
            placeBridgeAlong(bestPath);
        }
    }

    /** Labels grass, paths, trees and docks as land; water remains unwalkable. */
    private int[][] labelLandComponents() {
        int[][] labels = new int[mapHeight][mapWidth];
        int nextLabel = 0;
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                if (mapTiles[y][x] == TILE_WATER || labels[y][x] != 0) continue;
                nextLabel++;
                labels[y][x] = nextLabel;
                queue.add(y * mapWidth + x);
                while (!queue.isEmpty()) {
                    int current = queue.removeFirst();
                    int cx = current % mapWidth;
                    int cy = current / mapWidth;
                    int[] dx = {1, -1, 0, 0};
                    int[] dy = {0, 0, 1, -1};
                    for (int d = 0; d < 4; d++) {
                        int nx = cx + dx[d], ny = cy + dy[d];
                        if (nx < 0 || ny < 0 || nx >= mapWidth || ny >= mapHeight) continue;
                        if (labels[ny][nx] != 0 || mapTiles[ny][nx] == TILE_WATER) continue;
                        labels[ny][nx] = nextLabel;
                        queue.addLast(ny * mapWidth + nx);
                    }
                }
            }
        }
        return labels;
    }

    /** Paints only water cells along the shortest land-to-land route. */
    private void placeBridgeAlong(int[] path) {
        for (int i = 0; i < path.length; i++) {
            int current = path[i];
            int x = current % mapWidth;
            int y = current / mapWidth;
            if (mapTiles[y][x] == TILE_WATER) mapTiles[y][x] = TILE_DOCK;

            int before = path[Math.max(0, i - 1)];
            int after = path[Math.min(path.length - 1, i + 1)];
            int dx = (after % mapWidth) - (before % mapWidth);
            int dy = (after / mapWidth) - (before / mapWidth);

            // Widen across the bridge, not along it. Only water becomes planks.
            if (Math.abs(dx) >= Math.abs(dy)) {
                markDockIfWater(x, y - 1);
                markDockIfWater(x, y + 1);
            } else {
                markDockIfWater(x - 1, y);
                markDockIfWater(x + 1, y);
            }
        }
    }

    private void markDockIfWater(int x, int y) {
        if (x >= 0 && y >= 0 && x < mapWidth && y < mapHeight
                && mapTiles[y][x] == TILE_WATER) {
            mapTiles[y][x] = TILE_DOCK;
        }
    }

    private void carvePond(int cx, int cy, int rx, int ry) {
        carveEllipse(cx, cy, rx, ry, TILE_WATER);
        for (int y = cy - ry; y <= cy + ry; y++) {
            for (int x = cx - rx; x <= cx + rx; x++) {
                if (x < 0 || y < 0 || x >= mapWidth || y >= mapHeight) continue;
                pond[y][x] = true;
            }
        }
        // Keep the shoreline visible by reserving a two-tile rim.
        reserveRect(cx - rx - 2, cy - ry - 2, cx + rx + 2, cy + ry + 2);
    }

    private void carveEllipse(int cx, int cy, int rx, int ry, int tile) {
        for (int y = cy - ry; y <= cy + ry; y++) {
            for (int x = cx - rx; x <= cx + rx; x++) {
                if (x < 0 || y < 0 || x >= mapWidth || y >= mapHeight) continue;
                double dx = (double) (x - cx) / rx;
                double dy = (double) (y - cy) / ry;
                if (dx * dx + dy * dy <= 1.0) {
                    mapTiles[y][x] = tile;
                }
            }
        }
    }

    private void carveRoad(int x0, int y0, int x1, int y1) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            float t = steps == 0 ? 0f : (float) i / steps;
            int x = Math.round(x0 + (x1 - x0) * t);
            int y = Math.round(y0 + (y1 - y0) * t);
            setIfLand(x, y, TILE_DIRT);
            setIfLand(x + 1, y, TILE_DIRT);
        }
    }

    /** Fills a rectangle with packed dirt, used for the village plaza and lanes. */
    private void fillDirtRect(int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                setIfLand(x, y, TILE_DIRT);
            }
        }
    }

    private void setIfLand(int x, int y, int tile) {
        if (x < 0 || y < 0 || x >= mapWidth || y >= mapHeight) return;
        if (mapTiles[y][x] == TILE_WATER || mapTiles[y][x] == TILE_DOCK) return;
        mapTiles[y][x] = tile;
    }

    private void reserveRect(int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (x >= 0 && y >= 0 && x < mapWidth && y < mapHeight) {
                    reserved[y][x] = true;
                }
            }
        }
    }

    private void buildDock() {
        // Find the southern shoreline under the main farm road.
        int shoreY = -1;
        for (int y = 38; y < mapHeight; y++) {
            if (mapTiles[y][39] == TILE_WATER) {
                shoreY = y;
                break;
            }
        }
        if (shoreY < 0) return;

        // Connect the pasture road to the sand, then run two planks into the sea.
        for (int y = 38; y < shoreY; y++) {
            setIfLand(39, y, TILE_DIRT);
            setIfLand(40, y, TILE_DIRT);
        }
        for (int i = 0; i < 4 && shoreY + i < mapHeight; i++) {
            mapTiles[shoreY + i][38] = TILE_DOCK;
            mapTiles[shoreY + i][39] = TILE_DOCK;
            reserved[shoreY + i][38] = true;
            reserved[shoreY + i][39] = true;
        }
        dockEndY = shoreY + 3;
        dockEndX = 39;
        for (int x = 37; x <= 40 && dockEndY < mapHeight; x++) {
            mapTiles[dockEndY][x] = TILE_DOCK;
            reserved[dockEndY][x] = true;
        }
        // Clear the approach.
        reserveRect(36, shoreY - 3, 42, shoreY + 3);
    }

    private void placeTree(int x, int y) {
        placeTree(x, y, (x + y) % 3 == 0 ? "pine" : "tree");
    }

    private void placeTree(int x, int y, String type) {
        mapTiles[y][x] = TILE_TREE;
        reserved[y][x] = true;
        treeType.put(y * mapWidth + x, type);
    }

    private final java.util.Map<Integer, String> treeType = new java.util.HashMap<>();

    private void plantGrove(int cx, int cy, int rx, int ry, float density, Random rng) {
        for (int y = cy - ry; y <= cy + ry; y++) {
            for (int x = cx - rx; x <= cx + rx; x++) {
                if (x < 1 || y < 1 || x >= mapWidth - 1 || y >= mapHeight - 1) continue;
                if (mapTiles[y][x] != TILE_GRASS || reserved[y][x]) continue;
                if (isNearPath(x, y, 1)) continue;
                if (rng.nextFloat() <= density && !hasNeighborTree(x, y)) {
                    placeTree(x, y, rng.nextFloat() < 0.35f ? "pine" : "tree");
                }
            }
        }
    }

    private boolean hasNeighborTree(int x, int y) {
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                if (ox == 0 && oy == 0) continue;
                if (tileAt(x + ox, y + oy) == TILE_TREE) return true;
            }
        }
        return false;
    }

    private boolean isNearPath(int x, int y, int radius) {
        for (int oy = -radius; oy <= radius; oy++) {
            for (int ox = -radius; ox <= radius; ox++) {
                int tile = tileAt(x + ox, y + oy);
                if (tile == TILE_DIRT || tile == TILE_HOUSE_DOOR) return true;
            }
        }
        return false;
    }

    private boolean isCoastal(int x, int y) {
        for (int oy = -2; oy <= 2; oy++) {
            for (int ox = -2; ox <= 2; ox++) {
                if (tileAt(x + ox, y + oy) == TILE_WATER) return true;
            }
        }
        return false;
    }

    public void update() {
        animationTick++;
    }

    // ==========================================
    // GROUND RENDERING
    // ==========================================
    private void buildGround() {
        ground = new BufferedImage(mapWidth * TILE_SIZE, mapHeight * TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = ground.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        BufferedImage sheet = assetManager.getTileset();
        if (sheet == null) {
            g.setColor(new Color(106, 186, 90));
            g.fillRect(0, 0, ground.getWidth(), ground.getHeight());
            g.dispose();
            return;
        }
        BufferedImage grassEdge = sheet.getSubimage(64, 112, 16, 16);
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                int dx = x * TILE_SIZE, dy = y * TILE_SIZE;
                int tile = mapTiles[y][x];
                int variation = Math.floorMod(x * 73 + y * 37 + x * y * 11, 23);

                if (tile == TILE_DOCK) {
                    drawWaterTile(g, sheet, x, y, dx, dy);
                    drawDock(g, x, y, dx, dy);
                    continue;
                }

                // Grass base with sparse tile variation.
                drawTileFromSheet(g, sheet, 1, 1, dx, dy);
                if (variation < 4) drawTileFromSheet(g, sheet, 1 + variation, 2, dx, dy);

                if (isPath(x, y)) {
                    drawTileFromSheet(g, sheet, 9 + Math.floorMod(x + y * 3, 3), 7, dx, dy);
                    for (int side = 0; side < 4; side++) {
                        int nx = x + (side == 1 ? -1 : side == 3 ? 1 : 0);
                        int ny = y + (side == 0 ? 1 : side == 2 ? -1 : 0);
                        if (!isPath(nx, ny)) {
                            Graphics2D edge = (Graphics2D) g.create();
                            edge.rotate(side * Math.PI / 2, dx + 16, dy + 16);
                            edge.drawImage(grassEdge, dx, dy, 32, 32, null);
                            edge.dispose();
                        }
                    }
                } else if (tile == TILE_WATER) {
                    drawWaterTile(g, sheet, x, y, dx, dy);

                    // Sandy beach rim on the water side of every shoreline tile.
                    Color sand = new Color(232, 213, 166);
                    Color sandEdge = new Color(198, 171, 118);
                    if (!isWater(x, y - 1)) {
                        g.setColor(sand);
                        g.fillRect(dx, dy, TILE_SIZE, 5);
                        g.setColor(sandEdge);
                        g.fillRect(dx, dy + 4, TILE_SIZE, 1);
                    }
                    if (!isWater(x, y + 1)) {
                        g.setColor(sand);
                        g.fillRect(dx, dy + TILE_SIZE - 5, TILE_SIZE, 5);
                        g.setColor(sandEdge);
                        g.fillRect(dx, dy + TILE_SIZE - 5, TILE_SIZE, 1);
                    }
                    if (!isWater(x - 1, y)) {
                        g.setColor(sand);
                        g.fillRect(dx, dy, 5, TILE_SIZE);
                        g.setColor(sandEdge);
                        g.fillRect(dx + 4, dy, 1, TILE_SIZE);
                    }
                    if (!isWater(x + 1, y)) {
                        g.setColor(sand);
                        g.fillRect(dx + TILE_SIZE - 5, dy, 5, TILE_SIZE);
                        g.setColor(sandEdge);
                        g.fillRect(dx + TILE_SIZE - 5, dy, 1, TILE_SIZE);
                    }

                    // Lily pads and tiny blossoms on ponds.
                    if (pond[y][x] && Math.floorMod(x * 7 + y * 13, 19) == 0) {
                        g.setColor(new Color(46, 120, 62));
                        g.fillOval(dx + 8, dy + 12, 14, 9);
                        g.setColor(new Color(72, 156, 82));
                        g.fillOval(dx + 10, dy + 13, 10, 6);
                        if (Math.floorMod(x + y, 3) == 0) {
                            g.setColor(new Color(255, 236, 140));
                            g.fillOval(dx + 15, dy + 11, 4, 4);
                        }
                    }
                } else if (tile == TILE_FLOWERS) {
                    int flower = Math.floorMod(x * 11 + y * 7, 5);
                    drawTileFromSheet(g, sheet, 31 + flower, 3, dx, dy);
                }

                // A wider sandy band on the land side of every shoreline.
                if (tile != TILE_DOCK && tile != TILE_WATER) {
                    drawSandFringe(g, x, y, dx, dy);
                }
            }
        }
        // Give the island a raised, layered shoreline like a classic
        // colorful farming-adventure map. These edge details are visual only;
        // collision and farming still use the original tile grid.
        drawCliffRim(g);
        g.dispose();
    }

    /**
     * Draws warm soil/cliff faces along exposed land edges. The small pixel
     * bands add depth to the coastline without obscuring the grass or paths.
     */
    private void drawCliffRim(Graphics2D g) {
        Color cliffDark = new Color(119, 67, 43);
        Color cliffMid = new Color(157, 91, 55);
        Color cliffLight = new Color(190, 124, 73);
        Color cliffShadow = new Color(91, 57, 43);
        Color grassLip = new Color(75, 157, 71);

        for (int y = 1; y < mapHeight - 1; y++) {
            for (int x = 1; x < mapWidth - 1; x++) {
                int tile = mapTiles[y][x];
                if (tile == TILE_WATER || tile == TILE_DOCK) continue;

                int dx = x * TILE_SIZE;
                int dy = y * TILE_SIZE;
                int seed = Math.floorMod(x * 37 + y * 71 + x * y * 13, 11);

                // The south-facing cliff is the strongest visible edge.
                if (isWater(x, y + 1)) {
                    int h = 7 + seed % 4;
                    g.setColor(cliffDark);
                    g.fillRect(dx, dy + TILE_SIZE - h, TILE_SIZE, h);
                    g.setColor(cliffMid);
                    g.fillRect(dx, dy + TILE_SIZE - h + 2, TILE_SIZE, h - 2);
                    g.setColor(cliffLight);
                    for (int px = 3 + seed % 5; px < TILE_SIZE; px += 9) {
                        int mark = 2 + Math.floorMod(px + seed, 4);
                        g.fillRect(dx + px, dy + TILE_SIZE - h + mark, 3, 2);
                    }
                    g.setColor(cliffShadow);
                    g.drawLine(dx, dy + TILE_SIZE - 1, dx + TILE_SIZE - 1, dy + TILE_SIZE - 1);
                    g.setColor(grassLip);
                    g.drawLine(dx, dy + TILE_SIZE - h, dx + TILE_SIZE - 1, dy + TILE_SIZE - h);
                }

                // Short side faces break up long straight coastlines.
                if (isWater(x - 1, y)) {
                    g.setColor(cliffDark);
                    g.fillRect(dx, dy + 5, 5, 23);
                    g.setColor(cliffMid);
                    g.fillRect(dx + 1, dy + 7, 3, 18);
                    g.setColor(cliffLight);
                    g.fillRect(dx + 2, dy + 10 + seed % 5, 2, 3);
                }
                if (isWater(x + 1, y)) {
                    g.setColor(cliffDark);
                    g.fillRect(dx + TILE_SIZE - 5, dy + 5, 5, 23);
                    g.setColor(cliffMid);
                    g.fillRect(dx + TILE_SIZE - 4, dy + 7, 3, 18);
                    g.setColor(cliffLight);
                    g.fillRect(dx + TILE_SIZE - 4, dy + 12 + seed % 5, 2, 3);
                }
            }
        }
    }

    private void drawWaterTile(Graphics2D g, BufferedImage sheet, int x, int y, int dx, int dy) {
        drawTileFromSheet(g, sheet, 11 + x % 4, 18 + y % 2, dx, dy);
    }

    /** Adds a soft sand shelf where grass meets the sea or a pond. */
    private void drawSandFringe(Graphics2D g, int x, int y, int dx, int dy) {
        Color sand = new Color(232, 213, 166);
        Color sandEdge = new Color(198, 171, 118);
        if (isWater(x, y - 1)) {
            g.setColor(sand);
            g.fillRect(dx, dy, TILE_SIZE, 4);
            g.setColor(sandEdge);
            g.fillRect(dx, dy + 3, TILE_SIZE, 1);
        }
        if (isWater(x, y + 1)) {
            g.setColor(sand);
            g.fillRect(dx, dy + TILE_SIZE - 4, TILE_SIZE, 4);
            g.setColor(sandEdge);
            g.fillRect(dx, dy + TILE_SIZE - 4, TILE_SIZE, 1);
        }
        if (isWater(x - 1, y)) {
            g.setColor(sand);
            g.fillRect(dx, dy, 4, TILE_SIZE);
            g.setColor(sandEdge);
            g.fillRect(dx + 3, dy, 1, TILE_SIZE);
        }
        if (isWater(x + 1, y)) {
            g.setColor(sand);
            g.fillRect(dx + TILE_SIZE - 4, dy, 4, TILE_SIZE);
            g.setColor(sandEdge);
            g.fillRect(dx + TILE_SIZE - 4, dy, 1, TILE_SIZE);
        }
    }

    private void drawDock(Graphics2D g, int x, int y, int dx, int dy) {
        g.setColor(new Color(74, 45, 30));
        g.fillRect(dx, dy + 2, 32, 30);
        g.setColor(new Color(192, 137, 84));
        g.fillRect(dx + 1, dy + 4, 30, 26);
        g.setColor(new Color(153, 98, 56));
        for (int i = 0; i < 4; i++) {
            g.drawLine(dx + 2, dy + 8 + i * 6, dx + 30, dy + 8 + i * 6);
        }
        // Rim posts at the edges of the walkway.
        g.setColor(new Color(100, 60, 36));
        if (!isDock(x - 1, y)) g.fillRect(dx, dy + 2, 4, 30);
        if (!isDock(x + 1, y)) g.fillRect(dx + 28, dy + 2, 4, 30);
        if (!isDock(x, y - 1)) {
            g.setColor(new Color(128, 79, 45));
            g.fillRect(dx + 2, dy + 2, 6, 6);
            g.fillRect(dx + 24, dy + 2, 6, 6);
        }
        if (!isDock(x, y + 1)) {
            g.setColor(new Color(128, 79, 45));
            g.fillRect(dx + 2, dy + 24, 6, 8);
            g.fillRect(dx + 24, dy + 24, 6, 8);
        }
    }

    public void renderGround(Graphics2D g, int cameraX, int cameraY, int width, int height) {
        if (ground != null) {
            g.drawImage(ground, -cameraX, -cameraY, null);
        }
        renderWaterShimmer(g, cameraX, cameraY, width, height);
    }

    private void renderWaterShimmer(Graphics2D g, int cameraX, int cameraY, int width, int height) {
        int startX = Math.max(0, cameraX / TILE_SIZE - 1);
        int endX = Math.min(mapWidth - 1, (cameraX + width) / TILE_SIZE + 1);
        int startY = Math.max(0, cameraY / TILE_SIZE - 1);
        int endY = Math.min(mapHeight - 1, (cameraY + height) / TILE_SIZE + 1);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g.setColor(new Color(220, 245, 255));
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                if (mapTiles[y][x] != TILE_WATER) continue;
                int phase = Math.floorMod(x * 17 + y * 29 + animationTick / 14, 41);
                if (phase >= 6) continue;
                int sx = x * TILE_SIZE - cameraX;
                int sy = y * TILE_SIZE - cameraY;
                int offset = (animationTick / 6 + x * 5 + y * 3) % 20;
                g.fillRect(sx + 4 + offset, sy + 10 + (y % 3) * 6, 10, 2);
                g.fillRect(sx + 18 - offset / 2, sy + 22 - (x % 3) * 5, 7, 2);
            }
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ==========================================
    // OBJECTS
    // ==========================================
    private void addObject(BufferedImage image, int centerX, int bottomY, int depth) {
        addObject(image, centerX, bottomY, depth, 2);
    }

    private void addObject(BufferedImage image, int centerX, int bottomY, int depth, int scale) {
        if (image == null) return;
        objects.add(new Scenery(new BufferedImage[]{image}, centerX, bottomY, depth, 0, scale));
    }

    private void addAnimated(BufferedImage[] frames, int centerX, int bottomY, int depth, int phase) {
        addAnimated(frames, centerX, bottomY, depth, phase, 2);
    }

    private void addAnimated(BufferedImage[] frames, int centerX, int bottomY, int depth, int phase, int scale) {
        if (frames == null || frames.length == 0) return;
        objects.add(new Scenery(frames, centerX, bottomY, depth, phase, scale));
    }

    private void buildObjects() {
        objects.clear();
        buildingCollision = new boolean[mapHeight][mapWidth];
        BufferedImage sheet = assetManager.getTileset();
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                int tile = mapTiles[y][x];
                int cx = x * TILE_SIZE + 16, bottom = (y + 1) * TILE_SIZE;
                if (tile == TILE_TREE) {
                    String type = treeType.getOrDefault(y * mapWidth + x,
                            (x + y) % 3 == 0 ? "pine" : "tree");
                    addAnimated(assetManager.getSceneryFrames(type), cx, bottom + 4, bottom - 8, x + y);
                    // Mushroom clusters shelter at the base of some trees.
                    if (Math.floorMod(x * 31 + y * 17, 7) == 0) {
                        addAnimated(assetManager.getSceneryFrames("mushroom"), cx + 20, bottom + 2, bottom + 2, x + y);
                    } else if (Math.floorMod(x * 19 + y * 23, 23) == 0) {
                        addAnimated(assetManager.getSceneryFrames("mushroom_blue"), cx - 20, bottom + 2, bottom + 2, x + y);
                    } else if (Math.floorMod(x * 13 + y * 29, 37) == 0) {
                        addAnimated(assetManager.getSceneryFrames("mushroom_blue2"), cx - 18, bottom + 2, bottom + 2, x + y);
                    } else if (Math.floorMod(x * 41 + y * 7, 53) == 0) {
                        addAnimated(assetManager.getSceneryFrames("mushroom_blue3"), cx + 18, bottom + 2, bottom + 2, x + y);
                    }
                } else if (tile == TILE_FENCE_H) {
                    if (sheet != null) addObject(sheet.getSubimage(612, 0, 16, 16), cx, bottom, bottom - 10);
                } else if (tile == TILE_FENCE_V) {
                    if (sheet != null) addObject(sheet.getSubimage(640, 48, 16, 16), cx, bottom, bottom - 10);
                } else if (tile == TILE_SHIPPING_BIN) {
                    addObject(assetManager.getShippingCrate(), cx, bottom, bottom);
                    addObject(assetManager.getShippingCrateTop(), cx, bottom - 2, bottom + 1);
                }
            }
        }

        // Farmhouse: the clean tileset cottage, with a brick chimney and smoke.
        BufferedImage house = assetManager.getBuilding("ts_cottage_blue");
        int houseCenterX = 30 * TILE_SIZE + TILE_SIZE / 2;
        int houseBottomY = 13 * TILE_SIZE;
        addObject(house, houseCenterX, houseBottomY, houseBottomY, 4);
        if (house != null) {
            int roofTop = houseBottomY - house.getHeight() * 4;
            addObject(createChimney(), houseCenterX + 26, roofTop + 88, houseBottomY + 1, 2);
            addAnimated(assetManager.getSceneryFrames("chimney"),
                    houseCenterX + 26, roofTop + 58, houseBottomY + 2, 0);
        }

        // Village buildings from clean, complete tileset prefabs.
        for (BuildingSpec spec : BUILDINGS) {
            BufferedImage building = assetManager.getBuilding(spec.name);
            if (building == null) continue;
            int centerX = spec.tileX * TILE_SIZE + TILE_SIZE / 2;
            int bottomY = spec.bottomTileY * TILE_SIZE;
            // Open pavilions sit visually behind anyone standing beneath them.
            int depth = spec.solid ? bottomY : bottomY - 3 * TILE_SIZE;
            addObject(building, centerX, bottomY, depth, spec.scale);
            if (spec.solid) markBuildingSolid(building, centerX, bottomY, spec.scale);
        }

        // A simple wooden gate frames the field entrance (walk-through).
        addObject(assetManager.getBuilding("post"), 20 * TILE_SIZE, 17 * TILE_SIZE,
                17 * TILE_SIZE - 4, 2);
        addObject(assetManager.getBuilding("post"), 22 * TILE_SIZE, 17 * TILE_SIZE,
                17 * TILE_SIZE - 4, 2);
        addObject(assetManager.getBuilding("beam"), 21 * TILE_SIZE, 17 * TILE_SIZE - 38,
                17 * TILE_SIZE - 5, 2);

        // Farm workshop clutter beside the shed.
        addObject(assetManager.getBuilding("ladder"), 28 * TILE_SIZE + 8, 19 * TILE_SIZE,
                19 * TILE_SIZE + 2, 2);
        addObject(assetManager.getBuilding("plank_wide"), 25 * TILE_SIZE + 16, 20 * TILE_SIZE,
                20 * TILE_SIZE, 2);
        addObject(assetManager.getBuilding("crate_lid"), 24 * TILE_SIZE + 8, 18 * TILE_SIZE,
                18 * TILE_SIZE + 2, 2);

        // Market stalls around the plaza: stacks of produce crates.
        addObject(assetManager.getBuilding("crate_tall"), 44 * TILE_SIZE + 16, 28 * TILE_SIZE, 28 * TILE_SIZE + 2, 2);
        addObject(assetManager.getBuilding("crate_small"), 45 * TILE_SIZE + 16, 27 * TILE_SIZE, 27 * TILE_SIZE + 2, 2);
        addObject(assetManager.getBuilding("crate_lid"), 55 * TILE_SIZE + 16, 28 * TILE_SIZE, 28 * TILE_SIZE + 2, 2);
        addObject(assetManager.getBuilding("crate_tall"), 56 * TILE_SIZE + 16, 27 * TILE_SIZE, 27 * TILE_SIZE + 2, 2);
        addObject(assetManager.getBuilding("crate_small"), 46 * TILE_SIZE + 16, 24 * TILE_SIZE, 24 * TILE_SIZE + 2, 2);
        addObject(assetManager.getBuilding("crate_lid"), 54 * TILE_SIZE + 16, 24 * TILE_SIZE, 24 * TILE_SIZE + 2, 2);

        // Dock and bait-shed supplies.
        addObject(assetManager.getBuilding("ladder"), 51 * TILE_SIZE + 16, 40 * TILE_SIZE, 40 * TILE_SIZE + 2, 2);
        addObject(assetManager.getBuilding("plank_wide"), 43 * TILE_SIZE, 40 * TILE_SIZE, 40 * TILE_SIZE, 2);
        addObject(assetManager.getBuilding("stairs"), 44 * TILE_SIZE + 16, 42 * TILE_SIZE, 42 * TILE_SIZE, 2);

        // Windmill, a compact village landmark with spinning blades.
        BufferedImage[] blades = assetManager.getSceneryFrames("windmill");
        if (blades != null && blades.length > 0) {
            BufferedImage[] windmill = buildWindmillFrames(blades);
            int windmillX = 70 * TILE_SIZE + TILE_SIZE / 2;
            int windmillY = 33 * TILE_SIZE;
            addAnimated(windmill, windmillX, windmillY, windmillY - 2 * TILE_SIZE, 0, 1);
            markBuildingSolid(windmill[0], windmillX, windmillY, 1);
        }

        addObject(assetManager.getBuilding("wood_pile"), 82 * TILE_SIZE, 66 * TILE_SIZE, 66 * TILE_SIZE, 2);
        addObject(assetManager.getBuilding("plank_thin"), 84 * TILE_SIZE, 66 * TILE_SIZE, 66 * TILE_SIZE, 2);
        addObject(assetManager.getBuilding("awning_blue"), 111 * TILE_SIZE, 49 * TILE_SIZE, 47 * TILE_SIZE, 2);
        addObject(assetManager.getBuilding("crate_small"), 110 * TILE_SIZE, 49 * TILE_SIZE, 49 * TILE_SIZE, 2);
        BufferedImage annex = composeHouse();
        addObject(annex, 62 * TILE_SIZE, 64 * TILE_SIZE, 64 * TILE_SIZE, 1);
        if (annex != null) markBuildingSolid(annex, 62 * TILE_SIZE, 64 * TILE_SIZE, 1);
        addObject(createSign(), 95 * TILE_SIZE, 52 * TILE_SIZE, 52 * TILE_SIZE);
        addObject(createSign(), 40 * TILE_SIZE, 69 * TILE_SIZE, 69 * TILE_SIZE);

        // Beach coracle, plus little boats bobbing at the shore and dock.
        addObject(assetManager.getCoracleLand(), 7 * TILE_SIZE, 31 * TILE_SIZE, 31 * TILE_SIZE);
        addAnimated(assetManager.getSceneryFrames("boat"), 8 * TILE_SIZE, 33 * TILE_SIZE, 33 * TILE_SIZE, 0);
        if (dockEndX > 0 && dockEndY > 0) {
            int depth = (dockEndY + 1) * TILE_SIZE;
            addAnimated(assetManager.getSceneryFrames("boat"),
                    dockEndX * TILE_SIZE + 16, dockEndY * TILE_SIZE + 30, depth, 0);
        }

        // Hay bales in the pasture.
        BufferedImage hay = createHayBale();
        addObject(hay, 31 * TILE_SIZE, 36 * TILE_SIZE, 36 * TILE_SIZE);
        addObject(hay, 39 * TILE_SIZE, 32 * TILE_SIZE, 32 * TILE_SIZE);
        addObject(hay, 30 * TILE_SIZE, 31 * TILE_SIZE, 31 * TILE_SIZE);

        // Signposts at the crossroads and village square.
        BufferedImage sign = createSign();
        addObject(sign, 36 * TILE_SIZE, 18 * TILE_SIZE, 18 * TILE_SIZE - 4);
        addObject(sign, 43 * TILE_SIZE, 22 * TILE_SIZE, 22 * TILE_SIZE - 4);

        // Old stone well on the plaza's west side.
        addObject(createWell(), 45 * TILE_SIZE + 16, 27 * TILE_SIZE, 27 * TILE_SIZE - 4);
        buildingCollision[26][45] = true;

        // Scatter tileset bushes, rocks and mushrooms across the wilder island.
        BufferedImage[] bushes = {
                assetManager.getBuilding("bush_green"),
                assetManager.getBuilding("bush_berry_red"),
                assetManager.getBuilding("bush_berry_orange")
        };
        BufferedImage[] rocks = {
                assetManager.getBuilding("rock_big"),
                assetManager.getBuilding("rock_med"),
                assetManager.getBuilding("rock_small")
        };
        String[] mushroomKinds = {"mushroom", "mushroom_blue", "mushroom_blue2", "mushroom_blue3"};
        Random propRng = new Random(9_137L);
        for (int i = 0; i < 420; i++) {
            int x = 3 + propRng.nextInt(mapWidth - 6);
            int y = 3 + propRng.nextInt(mapHeight - 6);
            if (mapTiles[y][x] != TILE_GRASS || reserved[y][x] || isNearPath(x, y, 1)) continue;
            boolean nearTree = hasNeighborTree(x, y);
            float roll = propRng.nextFloat();
            int bottom = (y + 1) * TILE_SIZE;
            if (nearTree && roll < 0.30f) {
                addAnimated(assetManager.getSceneryFrames(mushroomKinds[propRng.nextInt(mushroomKinds.length)]),
                        x * TILE_SIZE + 16, bottom, bottom, x + y);
            } else if (!nearTree && roll < 0.12f) {
                addObject(bushes[propRng.nextInt(bushes.length)], x * TILE_SIZE + 16, bottom, bottom - 1, 2);
            } else if (!nearTree && roll < 0.17f) {
                addObject(rocks[propRng.nextInt(rocks.length)], x * TILE_SIZE + 16, bottom, bottom - 2, 2);
            }
        }

        objects.sort(Comparator.comparingInt(object -> object.depth));
    }

    private BufferedImage[] buildWindmillFrames(BufferedImage[] blades) {
        BufferedImage[] frames = new BufferedImage[blades.length];
        Color outline = new Color(82, 48, 31);
        Color body = new Color(205, 143, 88);
        Color plank = new Color(170, 103, 57);
        Color trim = new Color(153, 84, 47);
        Color dark = new Color(58, 41, 38);
        Color glass = new Color(139, 215, 216);
        for (int frame = 0; frame < blades.length; frame++) {
            BufferedImage image = new BufferedImage(112, 160, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            // Ground shadow.
            g.setColor(new Color(0, 0, 0, 55));
            g.fillOval(26, 148, 60, 10);

            // Tapered tower body, drawn row by row for crisp pixel edges.
            int topY = 44, baseY = 152;
            for (int y = topY; y < baseY; y++) {
                float t = (float) (y - topY) / (baseY - topY);
                int half = 17 + Math.round(t * 13);
                g.setColor(outline);
                g.fillRect(56 - half - 1, y, half * 2 + 2, 1);
                g.setColor(body);
                g.fillRect(56 - half, y, half * 2, 1);
            }
            // Vertical plank seams.
            g.setColor(new Color(184, 121, 72));
            for (int y = topY + 4; y < baseY - 2; y += 1) {
                float t = (float) (y - topY) / (baseY - topY);
                int half = 17 + Math.round(t * 13);
                if (y % 7 == 0) {
                    g.setColor(plank);
                    g.drawLine(56 - half / 2, y, 56 - half / 2, y + 6);
                    g.drawLine(56 + half / 2, y, 56 + half / 2, y + 6);
                    g.setColor(new Color(184, 121, 72));
                }
            }
            // Trim bands.
            g.setColor(trim);
            g.fillRect(32, 68, 48, 4);
            g.fillRect(29, 104, 54, 4);
            g.fillRect(26, 138, 60, 4);

            // Door and windows.
            g.setColor(outline);
            g.fillRect(48, 124, 16, 28);
            g.setColor(new Color(104, 62, 41));
            g.fillRect(50, 127, 12, 25);
            g.setColor(new Color(67, 45, 37));
            for (int y = 128; y < 152; y += 5) g.drawLine(51, y, 61, y);

            drawWindmillWindow(g, 44, 82, outline, glass, dark);
            drawWindmillWindow(g, 56, 82, outline, glass, dark);
            drawWindmillWindow(g, 50, 106, outline, glass, dark);

            // Blades spin above the tower.
            g.drawImage(blades[frame], 0, 0, null);
            g.dispose();
            frames[frame] = image;
        }
        return frames;
    }

    private void drawWindmillWindow(Graphics2D g, int x, int y, Color outline, Color glass, Color dark) {
        g.setColor(outline);
        g.fillRect(x - 1, y - 1, 12, 14);
        g.setColor(glass);
        g.fillRect(x, y, 10, 12);
        g.setColor(new Color(90, 140, 150));
        g.drawLine(x + 5, y, x + 5, y + 11);
        g.drawLine(x, y + 5, x + 9, y + 5);
        g.setColor(dark);
        g.drawRect(x, y, 9, 11);
    }

    private BufferedImage createChimney() {
        BufferedImage chimney = new BufferedImage(16, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = chimney.createGraphics();
        g.setColor(new Color(72, 43, 33));
        g.fillRect(1, 2, 14, 27);
        g.setColor(new Color(154, 87, 61));
        g.fillRect(2, 3, 12, 25);
        g.setColor(new Color(112, 62, 47));
        for (int y = 7; y < 27; y += 6) {
            g.drawLine(2, y, 13, y);
            g.drawLine(y + 1, 4, y + 1, 8);
        }
        g.setColor(new Color(202, 138, 94));
        g.fillRect(0, 0, 16, 4);
        g.setColor(new Color(92, 52, 39));
        g.drawRect(0, 0, 15, 3);
        g.dispose();
        return chimney;
    }

    private BufferedImage createHayBale() {
        BufferedImage bale = new BufferedImage(20, 14, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bale.createGraphics();
        g.setColor(new Color(99, 67, 29));
        g.fillRoundRect(0, 0, 20, 14, 4, 4);
        g.setColor(new Color(219, 177, 82));
        g.fillRoundRect(1, 1, 18, 12, 3, 3);
        g.setColor(new Color(183, 138, 55));
        g.drawLine(6, 2, 6, 12);
        g.drawLine(13, 2, 13, 12);
        g.setColor(new Color(237, 205, 116));
        for (int i = 0; i < 5; i++) g.drawLine(3 + i * 3, 3, 4 + i * 3, 5);
        g.dispose();
        return bale;
    }

    private BufferedImage createSign() {
        BufferedImage sign = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sign.createGraphics();
        g.setColor(new Color(78, 48, 31));
        g.fillRect(10, 11, 4, 13);
        g.setColor(new Color(127, 78, 44));
        g.fillRect(11, 12, 2, 12);
        g.setColor(new Color(82, 50, 33));
        g.fillRect(1, 1, 22, 13);
        g.setColor(new Color(191, 133, 77));
        g.fillRect(2, 2, 20, 11);
        g.setColor(new Color(140, 87, 48));
        g.drawLine(3, 4, 21, 4);
        g.drawLine(3, 7, 16, 7);
        g.drawLine(3, 10, 18, 10);
        g.dispose();
        return sign;
    }

    private BufferedImage createWell() {
        BufferedImage well = new BufferedImage(32, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = well.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // Stone rim.
        g.setColor(new Color(74, 66, 60));
        g.fillOval(1, 28, 30, 10);
        g.setColor(new Color(158, 147, 132));
        g.fillOval(2, 27, 28, 9);
        g.setColor(new Color(108, 99, 88));
        g.fillOval(7, 28, 18, 7);
        g.setColor(new Color(38, 83, 105));
        g.fillOval(9, 29, 14, 5);
        g.setColor(new Color(120, 201, 219));
        g.drawOval(9, 29, 14, 5);

        // Wooden posts and little shingled roof.
        g.setColor(new Color(92, 55, 33));
        g.fillRect(4, 12, 3, 18);
        g.fillRect(25, 12, 3, 18);
        g.setColor(new Color(146, 91, 52));
        g.fillRect(5, 13, 1, 16);
        g.fillRect(26, 13, 1, 16);
        g.setColor(new Color(68, 40, 27));
        g.fillPolygon(new int[]{0, 16, 32, 30, 16, 2},
                new int[]{10, 1, 10, 13, 6, 13}, 6);
        g.setColor(new Color(166, 77, 60));
        g.fillPolygon(new int[]{2, 16, 30, 28, 16, 4},
                new int[]{11, 3, 11, 12, 7, 12}, 6);
        g.setColor(new Color(196, 106, 79));
        for (int x = 5; x < 28; x += 5) {
            g.drawLine(x, 11, 16 + (x - 16) / 2, 5);
        }

        // Bucket and rope.
        g.setColor(new Color(58, 48, 40));
        g.drawLine(16, 8, 16, 22);
        g.setColor(new Color(126, 82, 45));
        g.fillRect(13, 22, 7, 6);
        g.setColor(new Color(86, 53, 30));
        g.drawRect(13, 22, 6, 5);
        g.dispose();
        return well;
    }

    private void markBuildingSolid(BufferedImage image, int centerX, int bottomY, int scale) {
        int width = image.getWidth() * scale;
        int height = image.getHeight() * scale;
        int left = (centerX - width / 2) / TILE_SIZE;
        int right = (centerX - width / 2 + width - 1) / TILE_SIZE;
        int top = (bottomY - height) / TILE_SIZE;
        int bottom = (bottomY - 6) / TILE_SIZE;
        for (int y = Math.max(0, top); y <= Math.min(mapHeight - 1, bottom); y++) {
            for (int x = Math.max(0, left); x <= Math.min(mapWidth - 1, right); x++) {
                int tile = mapTiles[y][x];
                if (tile != TILE_WATER && tile != TILE_DOCK) {
                    buildingCollision[y][x] = true;
                }
            }
        }
    }

    private BufferedImage composeHouse() {
        if (assetManager.getFarmhouse() == null) return null;
        BufferedImage house = new BufferedImage(160, 128, BufferedImage.TYPE_INT_ARGB);
        BufferedImage sheet = assetManager.getFarmhouse();
        Graphics2D g = house.createGraphics();
        for (int x = 8; x < 152; x += 16) {
            g.drawImage(sheet, x, 0, x + 16, 80, 96, 28, 112, 108, null);
            for (int y = 80; y < 112; y += 16) {
                g.drawImage(sheet, x, y, x + 16, y + 16, 240, 128, 256, 144, null);
            }
        }
        g.drawImage(sheet, 0, 0, 8, 80, 88, 28, 96, 108, null);
        g.drawImage(sheet, 152, 0, 160, 80, 112, 28, 120, 108, null);
        for (int x : new int[]{0, 152}) {
            g.drawImage(sheet, x, 80, x + 8, 112, 192, 128, 200, 144, null);
        }
        for (int x : new int[]{20, 116, 136}) {
            g.drawImage(sheet, x, 84, x + 8, 100, 276, 16, 284, 32, null);
        }
        g.drawImage(sheet, 44, 80, 100, 128, 12, 124, 68, 172, null);
        g.drawImage(sheet, 64, 112, 80, 128, 194, 156, 210, 172, null);
        g.dispose();
        return house;
    }

    public void renderObjects(Graphics2D g, int cameraX, int cameraY, int width, int height,
                              int playerDepth, boolean foreground) {
        for (Scenery object : objects) {
            if ((object.depth > playerDepth) != foreground) continue;
            BufferedImage frame = object.frames[(animationTick / 12 + object.phase) % object.frames.length];
            int x = object.x - cameraX, y = object.y - cameraY;
            int w = frame.getWidth() * object.scale, h = frame.getHeight() * object.scale;
            if (x + w >= 0 && y + h >= 0 && x < width && y < height) {
                g.drawImage(frame, x, y, w, h, null);
            }
        }
        if (!foreground) {
            renderPickups(g, cameraX, cameraY, width, height);
        }
    }

    /** Draws eggs and milk as small bobbing collectibles on the pasture grass. */
    private void renderPickups(Graphics2D g, int cameraX, int cameraY, int width, int height) {
        for (Pickup pickup : pickups) {
            BufferedImage item = assetManager.getItemImage(pickup.type);
            if (item == null) continue;
            int scale = 3;
            int w = item.getWidth() * scale;
            int h = item.getHeight() * scale;
            int bob = (int) (Math.sin((animationTick + pickup.tileX * 9 + pickup.tileY * 7) * 0.10) * 2);
            int sx = pickup.tileX * TILE_SIZE + TILE_SIZE / 2 - w / 2 - cameraX;
            int baseY = (pickup.tileY + 1) * TILE_SIZE - 4 - cameraY;
            int sy = baseY - h + bob;
            if (sx + w < 0 || sy + h < 0 || sx > width || sy > height) continue;
            g.setColor(new Color(0, 0, 0, 55));
            g.fillOval(sx + w / 2 - 10, baseY - 4, 20, 7);
            g.drawImage(item, sx, sy, w, h, null);
        }
    }

    private void drawTileFromSheet(Graphics2D g2, BufferedImage sheet, int tx, int ty, int dx, int dy) {
        int sx = tx * 16;
        int sy = ty * 16;
        if (sx + 16 <= sheet.getWidth() && sy + 16 <= sheet.getHeight()) {
            g2.drawImage(sheet, dx, dy, dx + TILE_SIZE, dy + TILE_SIZE, sx, sy, sx + 16, sy + 16, null);
        }
    }

    // ==========================================
    // QUERIES
    // ==========================================
    public boolean isSolid(int worldX, int worldY) {
        int tx = Math.floorDiv(worldX, TILE_SIZE);
        int ty = Math.floorDiv(worldY, TILE_SIZE);

        if (tx < 0 || tx >= mapWidth || ty < 0 || ty >= mapHeight) {
            return true;
        }

        int tile = mapTiles[ty][tx];
        return buildingCollision[ty][tx] || tile == TILE_WATER ||
               tile == TILE_FENCE_H ||
               tile == TILE_FENCE_V ||
               tile == TILE_HOUSE_WALL ||
               tile == TILE_HOUSE_ROOF ||
               tile == TILE_SHIPPING_BIN ||
               tile == TILE_TREE;
    }

    public boolean isWater(int tx, int ty) {
        if (tx >= 0 && tx < mapWidth && ty >= 0 && ty < mapHeight) {
            return mapTiles[ty][tx] == TILE_WATER;
        }
        return false;
    }

    public boolean isDock(int tx, int ty) {
        if (tx >= 0 && tx < mapWidth && ty >= 0 && ty < mapHeight) {
            return mapTiles[ty][tx] == TILE_DOCK;
        }
        return false;
    }

    public boolean isShippingBin(int tx, int ty) {
        if (tx >= 0 && tx < mapWidth && ty >= 0 && ty < mapHeight) {
            return mapTiles[ty][tx] == TILE_SHIPPING_BIN;
        }
        return false;
    }

    public boolean isHouseDoor(int tx, int ty) {
        if (tx >= 0 && tx < mapWidth && ty >= 0 && ty < mapHeight) {
            return mapTiles[ty][tx] == TILE_HOUSE_DOOR;
        }
        return false;
    }

    public boolean isTillable(int tx, int ty) {
        if (tx >= 0 && tx < mapWidth && ty >= 0 && ty < mapHeight) {
            int t = mapTiles[ty][tx];
            return !buildingCollision[ty][tx] && (t == TILE_GRASS || t == TILE_FLOWERS || t == TILE_FARM_PLOT);
        }
        return false;
    }

    public boolean isTree(int tx, int ty) {
        return tileAt(tx, ty) == TILE_TREE;
    }

    /** Chops a tree, rebuilding the scenery list so the stump is gone immediately. */
    public boolean chopTree(int tx, int ty) {
        if (tileAt(tx, ty) != TILE_TREE) return false;
        mapTiles[ty][tx] = TILE_GRASS;
        reserved[ty][tx] = false;
        treeType.remove(ty * mapWidth + tx);
        objects.clear();
        buildObjects();
        return true;
    }

    private int tileAt(int x, int y) {
        return x < 0 || y < 0 || x >= mapWidth || y >= mapHeight ? -1 : mapTiles[y][x];
    }

    private boolean isPath(int x, int y) {
        int tile = tileAt(x, y);
        return tile == TILE_DIRT || tile == TILE_HOUSE_DOOR;
    }

    public int getMapWidth() { return mapWidth; }
    public int getMapHeight() { return mapHeight; }
}
