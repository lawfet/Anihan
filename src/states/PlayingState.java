package states;

import engine.GamePanel;
import entities.Player;
import farming.CropManager;
import inputs.KeyHandler;
import inputs.MouseHandler;
import inventory.InventoryUI;
import managers.AssetManager;
import managers.TileManager;
import time.GameClock;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * PlayingState is the primary gameplay state containing the farm world,
 * crops, animated animals, player, time cycle, and inventory HUD.
 */
public class PlayingState extends State {

    private final GamePanel gamePanel;
    private final AssetManager assetM;
    private final KeyHandler keyH;
    private final MouseHandler mouseH;

    private TileManager tileM;
    private CropManager cropM;
    private GameClock clock;
    private Player player;
    private InventoryUI inventoryUI;

    // Camera
    private int cameraX;
    private int cameraY;
    private boolean mapOpen;

    // Farm Animals
    public static class Animal {
        int x, y;
        int animTick;
        int frameIndex;
        int moveTimer;
        int dx, dy;
        final BufferedImage[] frames;

        Animal(int x, int y, BufferedImage[] frames) {
            this.x = x;
            this.y = y;
            this.frames = frames;
        }

        void update(Random rng, TileManager tileM) {
            animTick++;
            int frameCount = (frames != null && frames.length > 0) ? frames.length : 1;
            if (animTick % 12 == 0) {
                frameIndex = (frameIndex + 1) % frameCount;
            }

            moveTimer--;
            if (moveTimer <= 0) {
                moveTimer = 60 + rng.nextInt(120);
                if (rng.nextFloat() < 0.4f) {
                    dx = rng.nextInt(3) - 1;
                    dy = rng.nextInt(3) - 1;
                } else {
                    dx = 0;
                    dy = 0;
                }
            }

            if (!tileM.isSolid(x + dx, y + dy) && !tileM.isSolid(x + dx + 24, y + dy)) {
                x += dx;
                y += dy;
            } else {
                dx = 0;
                dy = 0;
            }
        }

        void render(Graphics2D g2, int camX, int camY) {
            int sx = x - camX;
            int sy = y - camY;
            if (frames != null && frames.length > 0) {
                BufferedImage frame = frames[frameIndex];
                int width = frame.getWidth() * 2;
                int height = frame.getHeight() * 2;
                g2.drawImage(frame, sx + 16 - width / 2, sy + 8 - height, width, height, null);
            }
        }
    }

    private final List<Animal> animals = new ArrayList<>();
    private final Random rng = new Random();

    // Fireflies for nighttime ambiance
    public static class Firefly {
        float x, y;
        float vx, vy;
        int phase;
        Firefly(float x, float y, Random rng) {
            this.x = x; this.y = y;
            this.vx = (rng.nextFloat() - 0.5f) * 0.4f;
            this.vy = (rng.nextFloat() - 0.5f) * 0.4f;
            this.phase = rng.nextInt(100);
        }
        void update(Random rng, TileManager tileM, int mapW, int mapH) {
            phase++;
            x += vx; y += vy;
            if (rng.nextFloat() < 0.02f) { vx = (rng.nextFloat() - 0.5f) * 0.4f; vy = (rng.nextFloat() - 0.5f) * 0.4f; }
            if (x < 0 || x > mapW * 32) vx = -vx;
            if (y < 0 || y > mapH * 32) vy = -vy;
        }
    }
    private final List<Firefly> fireflies = new ArrayList<>();

    public PlayingState(StateManager stateManager, GamePanel gamePanel) {
        super(stateManager);
        this.gamePanel = gamePanel;
        this.assetM = gamePanel.getAssetManager();
        this.keyH = gamePanel.getKeyHandler();
        this.mouseH = gamePanel.getMouseHandler();
    }

    @Override
    public void init() {
        tileM = new TileManager(assetM);
        cropM = new CropManager();

        clock = new GameClock(() -> {
            // New day callback: advance crops & notify player
            cropM.advanceDay();
            tileM.resetPickups();
            if (player != null) {
                player.onNewDay();
            }
            assetM.playRooster();
        });

        // Spawn player near farmhouse (tile x=29, y=14)
        player = new Player(29, 14, keyH, mouseH, tileM, cropM, assetM);
        inventoryUI = new InventoryUI(player, keyH, mouseH, assetM, clock);

        // Spawn roaming farm animals
        animals.clear();
        // Chickens scratching near the garden gate
        animals.add(new Animal(18 * 32, 19 * 32, assetM.getChickenFrames()));
        animals.add(new Animal(22 * 32, 20 * 32, assetM.getChickenFrames()));
        animals.add(new Animal(26 * 32, 19 * 32, assetM.getChickenFrames()));
        // Cows, sheep & pigs in the fenced pasture
        animals.add(new Animal(31 * 32, 32 * 32, assetM.getCowFrames()));
        animals.add(new Animal(36 * 32, 36 * 32, assetM.getCowFrames()));
        animals.add(new Animal(33 * 32, 35 * 32, assetM.getSheepFrames()));
        animals.add(new Animal(39 * 32, 31 * 32, assetM.getSheepFrames()));
        animals.add(new Animal(30 * 32, 36 * 32, assetM.getPigFrames()));
        animals.add(new Animal(37 * 32, 33 * 32, assetM.getPigFrames()));
        // Ducks and little critters around the western pond
        animals.add(new Animal(9 * 32, 32 * 32, assetM.getSceneryFrames("duck")));
        animals.add(new Animal(22 * 32, 36 * 32, assetM.getSceneryFrames("duck")));
        animals.add(new Animal(12 * 32, 38 * 32, assetM.getSceneryFrames("blinking")));
        // Birds
        animals.add(new Animal(24 * 32, 22 * 32, assetM.getSceneryFrames("bird")));
        animals.add(new Animal(45 * 32, 19 * 32, assetM.getSceneryFrames("bird")));

        // Fireflies scattered across the farm
        fireflies.clear();
        for (int i = 0; i < 40; i++) {
            fireflies.add(new Firefly(rng.nextFloat() * tileM.getMapWidth() * 32,
                    rng.nextFloat() * tileM.getMapHeight() * 32, rng));
        }
    }

    @Override
    public void update() {
        // Pause check
        if (keyH.isJustPressed(java.awt.event.KeyEvent.VK_ESCAPE)) {
            assetM.playClick();
            stateManager.setPauseState();
            return;
        }

        if (keyH.isJustPressed(java.awt.event.KeyEvent.VK_M)) mapOpen = !mapOpen;
        if (mapOpen) return;

        // Sleep / Bed interaction check
        if (keyH.isJustPressed(java.awt.event.KeyEvent.VK_F)) {
            int tx = (player.getWorldX() + 48) / TileManager.TILE_SIZE;
            int ty = (player.getWorldY() + 48) / TileManager.TILE_SIZE;
            if (tileM.isHouseDoor(tx, ty) || tileM.isHouseDoor(tx, ty - 1)) {
                clock.sleep();
                assetM.playClick();
            }
        }

        inventoryUI.update();
        clock.update();
        tileM.update();
        cropM.update();
        if (!clock.isSleeping() && !inventoryUI.isOpen && !(mouseH.leftClicked && inventoryUI.consumesPointer())) {
            player.update();
            String pickup = tileM.tryCollectPickup(player.getWorldX() + 48, player.getWorldY() + 56);
            if (pickup != null) {
                farming.FarmProgress.Material material = "egg".equals(pickup)
                        ? farming.FarmProgress.Material.EGG : farming.FarmProgress.Material.MILK;
                player.getProgress().addMaterial(material, 1);
                player.showFloatingText("+1 " + material.label + " stored in backpack", new Color(255, 236, 150));
                player.showExpression("expression_love");
                assetM.playCoin();
            }
        }

        for (Animal a : animals) {
            a.update(rng, tileM);
        }

        boolean night = clock.getHour() >= 20 || clock.getHour() < 6;
        if (night) {
            for (Firefly f : fireflies) {
                f.update(rng, tileM, tileM.getMapWidth(), tileM.getMapHeight());
            }
        }

        // Update Camera centered on player
        int screenW = gamePanel.getScreenWidth();
        int screenH = gamePanel.getScreenHeight();

        cameraX = player.getWorldX() - screenW / 2 + 48;
        cameraY = player.getWorldY() - screenH / 2 - 32;

        // Clamp camera to map boundaries
        int maxCamX = tileM.getMapWidth() * TileManager.TILE_SIZE - screenW;
        int maxCamY = tileM.getMapHeight() * TileManager.TILE_SIZE - screenH;
        cameraX = Math.max(0, Math.min(maxCamX, cameraX));
        cameraY = Math.max(0, Math.min(maxCamY, cameraY));
    }

    @Override
    public void render(Graphics2D g2) {
        int screenW = gamePanel.getScreenWidth();
        int screenH = gamePanel.getScreenHeight();

        // 1. Render Ground and Paths
        tileM.renderGround(g2, cameraX, cameraY, screenW, screenH);

        // 2. Render Tilled Soil & Crops
        cropM.render(g2, cameraX, cameraY, TileManager.TILE_SIZE, assetM);

        tileM.renderObjects(g2, cameraX, cameraY, screenW, screenH, player.getWorldY() + 56, false);

        // 3. Render Farm Animals
        for (Animal a : animals) {
            a.render(g2, cameraX, cameraY);
        }

        // 4. Render Player
        player.render(g2, cameraX, cameraY);

        // 5. Render Canopy / High Structures (tree tops, roofs) over player
        tileM.renderObjects(g2, cameraX, cameraY, screenW, screenH, player.getWorldY() + 56, true);

        // 5.5 Render Fireflies at night
        boolean nightRender = clock.getHour() >= 20 || clock.getHour() < 6;
        if (nightRender) {
            for (Firefly f : fireflies) {
                int sx = (int) f.x - cameraX;
                int sy = (int) f.y - cameraY;
                float alpha = 0.4f + 0.6f * (float) Math.abs(Math.sin(f.phase * 0.1));
                int a = (int) (alpha * 255);
                g2.setColor(new Color(200, 255, 120, Math.min(255, a)));
                g2.fillOval(sx, sy, 4, 4);
                g2.setColor(new Color(255, 255, 200, a / 3));
                g2.fillOval(sx - 3, sy - 3, 10, 10);
            }
        }

        // 6. Render Day/Night Ambient Lighting & Sleep Overlay
        int playerScreenX = player.getWorldX() - cameraX + 48;
        int playerScreenY = player.getWorldY() - cameraY + 32;
        clock.renderAmbientLighting(g2, screenW, screenH, playerScreenX, playerScreenY);

        // 7. Render HUD / Inventory UI
        inventoryUI.render(g2, screenW, screenH);

        // 8. Render Quick Help Prompt at top center
        renderPrompt(g2, screenW);
        if (mapOpen) renderWorldMap(g2, screenW, screenH);
    }

    private void renderWorldMap(Graphics2D g, int width, int height) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, width, height);
        int mapH = height - 140;
        int mapW = mapH * tileM.getMapWidth() / tileM.getMapHeight();
        int x = (width - mapW) / 2, y = 80;
        assetM.drawPanel(g, x - 30, y - 32, mapW + 60, mapH + 80);
        Graphics2D overview = (Graphics2D) g.create();
        overview.translate(x, y);
        overview.scale((double) mapW / (tileM.getMapWidth() * TileManager.TILE_SIZE),
                (double) mapH / (tileM.getMapHeight() * TileManager.TILE_SIZE));
        overview.setClip(0, 0, tileM.getMapWidth() * 32, tileM.getMapHeight() * 32);
        tileM.renderGround(overview, 0, 0, tileM.getMapWidth() * 32, tileM.getMapHeight() * 32);
        cropM.render(overview, 0, 0, TileManager.TILE_SIZE, assetM);
        tileM.renderObjects(overview, 0, 0, tileM.getMapWidth() * 32, tileM.getMapHeight() * 32, Integer.MAX_VALUE, false);
        overview.dispose();
        int px = x + (player.getWorldX() + 48) * mapW / (tileM.getMapWidth() * 32);
        int py = y + (player.getWorldY() + 56) * mapH / (tileM.getMapHeight() * 32);
        g.setColor(Color.BLACK);
        g.fillOval(px - 5, py - 5, 10, 10);
        g.setColor(Color.YELLOW);
        g.fillOval(px - 3, py - 3, 6, 6);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(255, 240, 210));
        g.drawString("ANIHAN WORLD  |  " + tileM.getMapWidth() + " x " + tileM.getMapHeight() + " tiles  |  M to close", x, y + mapH + 25);
        g.drawString("Explore the islands using the timber bridges", x, y + mapH + 43);
    }

    private void renderPrompt(Graphics2D g2, int screenW) {
        if (inventoryUI.isOpen) return;
        String msg = "WASD Move / 1-9 Tool / SPACE Use / F Sleep / E Bag / M Map / ESC Pause";
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        int tw = g2.getFontMetrics().stringWidth(msg);
        int px = (screenW - tw) / 2;
        assetM.drawLabel(g2, px - 16, 14, tw + 32);
        g2.setColor(new Color(61, 35, 32));
        g2.drawString(msg, px, 31);
    }

    public Player getPlayer() { return player; }
    public GameClock getClock() { return clock; }
    public CropManager getCropManager() { return cropM; }
}
