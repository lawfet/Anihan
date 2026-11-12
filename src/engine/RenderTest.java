package engine;

import managers.AssetManager;
import states.StateManager;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

public class RenderTest {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--window".equals(args[0])) {
            try {
                testWindow();
            } catch (Throwable error) {
                error.printStackTrace();
                System.exit(1);
            }
            System.exit(0);
        }
        GamePanel panel = new GamePanel();
        StateManager sm = panel.getStateManager();
        sm.init();

        // 1. Render MenuState
        BufferedImage menuImg = new BufferedImage(1024, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = menuImg.createGraphics();
        sm.update();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        sm.render(g2);
        g2.dispose();
        ImageIO.write(menuImg, "PNG", new File("bin/screenshot_menu.png"));
        System.out.println("Saved screenshot_menu.png");

        // 1b. Credits modal with the Example Scene artwork.
        inputs.MouseHandler menuMouse = panel.getMouseHandler();
        menuMouse.mouseX = 181;
        menuMouse.mouseY = 538;
        menuMouse.leftClicked = true;
        sm.update();
        menuMouse.leftClicked = false;
        require(panel.getAssetManager().getExampleScene() != null, "Example Scene artwork must load");
        BufferedImage creditsImg = new BufferedImage(1024, 640, BufferedImage.TYPE_INT_ARGB);
        g2 = creditsImg.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        sm.render(g2);
        g2.dispose();
        ImageIO.write(creditsImg, "PNG", new File("bin/screenshot_credits.png"));
        System.out.println("Saved screenshot_credits.png");
        menuMouse.leftClicked = true;
        sm.update();
        menuMouse.leftClicked = false;

        // 2. Render PlayingState
        sm.setPlayingState();
        for (int i = 0; i < 10; i++) sm.update();
        BufferedImage playImg = new BufferedImage(1024, 640, BufferedImage.TYPE_INT_ARGB);
        g2 = playImg.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        sm.render(g2);
        g2.dispose();
        ImageIO.write(playImg, "PNG", new File("bin/screenshot_play.png"));
        System.out.println("Saved screenshot_play.png");

        // 3. Render PauseState
        sm.setPauseState();
        sm.update();
        BufferedImage pauseImg = new BufferedImage(1024, 640, BufferedImage.TYPE_INT_ARGB);
        g2 = pauseImg.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        sm.render(g2);
        g2.dispose();
        ImageIO.write(pauseImg, "PNG", new File("bin/screenshot_pause.png"));
        System.out.println("Saved screenshot_pause.png");

        testFarming(panel.getAssetManager());
        testMovement(panel.getAssetManager());
        testFishing(panel.getAssetManager());
        testChopping(panel.getAssetManager());
        testRegression(panel.getAssetManager());
        captureWorld(panel.getAssetManager(), 640, 160, "screenshot_farmhouse.png");
        captureWorld(panel.getAssetManager(), 1500, 520, "screenshot_village.png");
        captureWorld(panel.getAssetManager(), 640, 800, "screenshot_pasture.png");
        captureFullMap(panel.getAssetManager(), "screenshot_map.png");
        System.out.println("PASS: asset rendering, movement, stamina, till, plant, water, grow, harvest, fishing and chopping");
        System.exit(0);
    }

    private static void captureFullMap(AssetManager assets, String name) throws Exception {
        managers.TileManager tiles = new managers.TileManager(assets);
        int width = tiles.getMapWidth() * managers.TileManager.TILE_SIZE;
        int height = tiles.getMapHeight() * managers.TileManager.TILE_SIZE;
        BufferedImage image = new BufferedImage(width / 2, height / 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.scale(0.5, 0.5);
        g.setClip(0, 0, width, height);
        tiles.renderGround(g, 0, 0, width, height);
        new farming.CropManager().render(g, 0, 0, managers.TileManager.TILE_SIZE, assets);
        tiles.renderObjects(g, 0, 0, width, height, Integer.MAX_VALUE, false);
        g.dispose();
        ImageIO.write(image, "PNG", new File("bin/" + name));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void testFarming(AssetManager assets) {
        managers.TileManager tiles = new managers.TileManager(assets);
        require(tiles.isSolid(-1, 100), "Negative world coordinates must collide");
        require(tiles.isSolid(tiles.getMapWidth() * 32, 100), "Map edge must collide");
        require(tiles.isWater(12, 34), "Pond must retain water interactions");
        require(tiles.isHouseDoor(30, 13), "Farmhouse door must remain accessible");
        require(tiles.isShippingBin(34, 15), "Shipping crate must match its interaction tile");
        farming.CropManager crops = new farming.CropManager();
        inputs.KeyHandler keys = new inputs.KeyHandler();
        entities.Player farmer = new entities.Player(17, 12, keys, new inputs.MouseHandler(), tiles, crops, assets);
        useTool(farmer, keys, 0);
        require(crops.isSoil(18, 14), "Hoe must till the highlighted tile");
        require(farmer.getEnergy() == 98, "Hoe must consume energy");
        useTool(farmer, keys, 4);
        require(crops.getSoil(18, 14).hasCrop(), "Seed action must plant on the selected soil");
        useTool(farmer, keys, 1);
        require(crops.getSoil(18, 14).isWatered(), "Watering must hydrate the highlighted soil");
        require(farmer.getWaterCanCurrent() == 5, "Watering must consume one charge");
        for (int day = 0; day < 3; day++) {
            crops.waterSoil(18, 14);
            crops.advanceDay();
        }
        require(crops.getSoil(18, 14).isMature(), "Watered carrots must mature");
        int gold = farmer.getGold();
        useTool(farmer, keys, 0);
        require(!crops.getSoil(18, 14).hasCrop(), "Harvest must remove the mature plant");
        require(farmer.getGold() == gold, "Harvest must not auto-sell goods");
        require(farmer.getProgress().getCrop(farming.HarvestedCrop.CropType.CARROT) == 1, "Harvest must enter inventory");
        require(farmer.getProgress().getSeeds(farming.HarvestedCrop.CropType.CARROT) == 9, "Planting must consume a seed");
        require(farmer.getProgress().sellAll() == 15, "Shipping must sell stored crops once");
        require(farmer.getProgress().sellAll() == 0, "Empty shipping must not duplicate gold");
    }

    private static void testFishing(AssetManager assets) {
        managers.TileManager tiles = new managers.TileManager(assets);
        inputs.KeyHandler keys = new inputs.KeyHandler();
        entities.Player fisher = new entities.Player(9, 34, keys, new inputs.MouseHandler(),
                tiles, new farming.CropManager(), assets);

        // Face the western pond so the cast lands on water.
        keys.right = true;
        fisher.update();
        keys.right = false;

        int gold = fisher.getGold();
        int energy = fisher.getEnergy();
        keys.selectedSlot = 3;
        keys.space = true;
        fisher.update();
        keys.space = false;

        // Play out the casting strip so the line settles in the water.
        for (int tick = 0; tick < 80; tick++) fisher.update();

        boolean caught = false;
        for (int tick = 0; tick < 1400 && !caught; tick++) {
            keys.space = tick % 8 == 0;
            fisher.update();
            caught = fisher.getFishCaught() > 0;
        }
        keys.space = false;

        require(caught, "Reeling during a bite must land a fish");
        require(fisher.getGold() == gold, "Catching a fish must not auto-sell it");
        require(fisher.getProgress().getMaterial(farming.FarmProgress.Material.FISH) == 1, "Fish must enter inventory");
        require(fisher.getEnergy() < energy, "Fishing must consume energy");
    }

    private static void testMovement(AssetManager assets) {
        managers.TileManager tiles = new managers.TileManager(assets);
        int[] start = findOpenMovementArea(tiles);
        require(start != null, "The map must have room to test movement");

        inputs.KeyHandler horizontalKeys = new inputs.KeyHandler();
        entities.Player horizontal = new entities.Player(start[0], start[1], horizontalKeys,
                new inputs.MouseHandler(), tiles, new farming.CropManager(), assets);
        horizontalKeys.right = true;
        for (int tick = 0; tick < 60; tick++) horizontal.update();
        horizontalKeys.right = false;
        double horizontalDistance = horizontal.getWorldX() - start[0] * managers.TileManager.TILE_SIZE;

        inputs.KeyHandler diagonalKeys = new inputs.KeyHandler();
        entities.Player diagonal = new entities.Player(start[0], start[1], diagonalKeys,
                new inputs.MouseHandler(), tiles, new farming.CropManager(), assets);
        diagonalKeys.right = true;
        diagonalKeys.up = true;
        for (int tick = 0; tick < 60; tick++) diagonal.update();
        diagonalKeys.right = false;
        diagonalKeys.up = false;
        double diagonalDistance = Math.hypot(
                diagonal.getWorldX() - start[0] * managers.TileManager.TILE_SIZE,
                diagonal.getWorldY() - start[1] * managers.TileManager.TILE_SIZE);
        require(Math.abs(horizontalDistance - diagonalDistance) <= 3,
                "Diagonal movement must not be faster than horizontal movement");

        inputs.KeyHandler runningKeys = new inputs.KeyHandler();
        entities.Player runner = new entities.Player(start[0], start[1], runningKeys,
                new inputs.MouseHandler(), tiles, new farming.CropManager(), assets);
        runningKeys.up = true;
        for (int tick = 0; tick < 39; tick++) runner.update();
        runningKeys.up = false;
        runningKeys.right = true;
        runningKeys.shift = true;
        for (int tick = 0; tick < 39; tick++) runner.update();
        require(runner.getEnergy() == 100, "Sprinting must not drain stamina before its interval");
        runner.update();
        require(runner.getEnergy() == 99, "Sprinting must drain stamina on its intended interval");
    }

    private static int[] findOpenMovementArea(managers.TileManager tiles) {
        for (int y = 8; y < tiles.getMapHeight() - 10; y++) {
            for (int x = 4; x < tiles.getMapWidth() - 10; x++) {
                boolean open = true;
                for (int ty = y - 6; ty <= y + 2 && open; ty++) {
                    for (int tx = x - 1; tx <= x + 8; tx++) {
                        if (tiles.isSolid(tx * managers.TileManager.TILE_SIZE + 16,
                                ty * managers.TileManager.TILE_SIZE + 16)) {
                            open = false;
                            break;
                        }
                    }
                }
                if (open) return new int[]{x, y};
            }
        }
        return null;
    }

    private static void testChopping(AssetManager assets) {
        managers.TileManager tiles = new managers.TileManager(assets);
        int[] tree = null;
        for (int y = 2; y < tiles.getMapHeight() - 2 && tree == null; y++) {
            for (int x = 2; x < tiles.getMapWidth() - 2; x++) {
                if (tiles.isTree(x, y) && !tiles.isSolid(x * 32 + 16, (y - 1) * 32 + 16)) {
                    tree = new int[]{x, y};
                    break;
                }
            }
        }
        require(tree != null, "The island must grow at least one reachable tree");

        inputs.KeyHandler keys = new inputs.KeyHandler();
        entities.Player woodcutter = new entities.Player(tree[0], tree[1] - 1, keys,
                new inputs.MouseHandler(), tiles, new farming.CropManager(), assets);
        keys.left = true;
        woodcutter.update();
        keys.left = false;

        useTool(woodcutter, keys, 2);
        require(woodcutter.getWoodGathered() == 2, "Axe must gather wood from a tree");
        require(!tiles.isTree(tree[0], tree[1]), "A chopped tree must be removed from the map");
    }

    private static void testRegression(AssetManager assets) {
        for (String name : assets.getResourceNames()) require(assets.getResourceImage(name) != null, "Missing resource: " + name);
        for (AssetManager.Action action : AssetManager.Action.values()) {
            for (int frame = 0; frame < action.frameCount; frame++) {
                require(assets.getBaseFrame(action, frame) != null, "Missing body animation: " + action);
                require(assets.getToolFrame(action, frame) != null, "Missing tool animation: " + action);
                for (String hair : AssetManager.HAIR_STYLES) {
                    require(assets.getHairFrame(hair, action, frame) != null, "Missing hair animation: " + hair + action);
                }
            }
        }
        managers.TileManager tiles = new managers.TileManager(assets);
        require(tiles.getMapWidth() == 160 && tiles.getMapHeight() == 112, "World must have four times its original area");
        boolean[][] visited = new boolean[112][160];
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        queue.add(new int[]{30, 15});
        visited[15][30] = true;
        while (!queue.isEmpty()) {
            int[] cell = queue.remove();
            for (int[] direction : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int x = cell[0] + direction[0], y = cell[1] + direction[1];
                if (x < 0 || y < 0 || x >= 160 || y >= 112 || visited[y][x]) continue;
                if (tiles.isSolid(x * 32 + 6, y * 32 + 8) || tiles.isSolid(x * 32 + 25, y * 32 + 23)) continue;
                visited[y][x] = true;
                queue.add(new int[]{x, y});
            }
        }
        for (int[] destination : new int[][]{{20, 15}, {34, 14}, {87, 63}, {96, 49}, {65, 79}, {38, 98}}) {
            require(visited[destination[1]][destination[0]], "Disconnected district at " + java.util.Arrays.toString(destination));
        }
        inputs.KeyHandler keys = new inputs.KeyHandler();
        farming.CropManager crops = new farming.CropManager();
        entities.Player farmer = new entities.Player(17, 12, keys, new inputs.MouseHandler(), tiles, crops, assets);
        useTool(farmer, keys, 0);
        keys.selectedSlot = 4;
        keys.space = true;
        farmer.update();
        keys.space = false;
        keys.selectedSlot = 5;
        for (int tick = 0; tick < 60; tick++) farmer.update();
        require(crops.getSoil(18, 14).getPlantedCrop() == farming.HarvestedCrop.CropType.CARROT, "Changing slots must not change an ongoing action");
        require(farmer.getProgress().getSeeds(farming.HarvestedCrop.CropType.CARROT) == 9, "Captured seed must be consumed");
        useTool(farmer, keys, 1);
        int energy = farmer.getEnergy(), water = farmer.getWaterCanCurrent();
        useTool(farmer, keys, 1);
        require(farmer.getEnergy() == energy && farmer.getWaterCanCurrent() == water, "Already-watered soil must not consume water or stamina");
        inputs.KeyHandler blockedKeys = new inputs.KeyHandler();
        entities.Player blocked = new entities.Player(27, 9, blockedKeys, new inputs.MouseHandler(), tiles, crops, assets);
        blockedKeys.right = true;
        blockedKeys.shift = true;
        for (int tick = 0; tick < 120; tick++) blocked.update();
        require(blocked.getEnergy() == 100, "Sprinting into a wall must not drain stamina");
        final int[] days = {0};
        time.GameClock clock = new time.GameClock(() -> days[0]++);
        clock.sleep();
        for (int tick = 0; tick < 5; tick++) clock.update();
        clock.executeDayAdvance();
        for (int tick = 0; tick < 80; tick++) clock.update();
        require(days[0] == 1 && clock.getDay() == 2, "Advancing during sleep must not advance twice");
        java.awt.Canvas source = new java.awt.Canvas();
        inputs.KeyHandler aliases = new inputs.KeyHandler();
        for (int code : new int[]{java.awt.event.KeyEvent.VK_W, java.awt.event.KeyEvent.VK_UP}) {
            aliases.keyPressed(new java.awt.event.KeyEvent(source, java.awt.event.KeyEvent.KEY_PRESSED, 0, 0, code, ' '));
        }
        aliases.keyReleased(new java.awt.event.KeyEvent(source, java.awt.event.KeyEvent.KEY_RELEASED, 0, 0, java.awt.event.KeyEvent.VK_W, ' '));
        require(aliases.up, "Releasing W must not release a held Up arrow");
        aliases.reset();
        require(!aliases.up, "Focus reset must clear held keys");
        System.out.println("PASS: " + assets.getResourceNames().size() + " resources, all animation layers, connected 160x112 world and regression cases");
    }

    private static void useTool(entities.Player player, inputs.KeyHandler keys, int slot) {
        keys.selectedSlot = slot;
        keys.space = true;
        player.update();
        keys.space = false;
        for (int tick = 0; tick < 60; tick++) player.update();
    }

    private static void captureWorld(AssetManager assets, int cameraX, int cameraY, String name) throws Exception {
        managers.TileManager tiles = new managers.TileManager(assets);
        BufferedImage image = new BufferedImage(1024, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        tiles.renderGround(g, cameraX, cameraY, 1024, 640);
        tiles.renderObjects(g, cameraX, cameraY, 1024, 640, Integer.MAX_VALUE, false);
        g.dispose();
        ImageIO.write(image, "PNG", new File("bin/" + name));
    }

    private static void testWindow() throws Exception {
        final javax.swing.JFrame[] window = new javax.swing.JFrame[1];
        final GamePanel[] game = new GamePanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            window[0] = new javax.swing.JFrame("ANIHAN - Visual regression test");
            game[0] = new GamePanel();
            window[0].add(game[0]);
            window[0].pack();
            window[0].setLocationRelativeTo(null);
            window[0].setVisible(true);
            game[0].startGameThread();
            game[0].requestFocusInWindow();
        });
        try {
            java.awt.Robot robot = new java.awt.Robot();
            robot.setAutoDelay(70);
            robot.waitForIdle();
            java.awt.Point origin = game[0].getLocationOnScreen();
            robot.delay(250);
            entities.Player preview = ((states.PlayingState) game[0].getStateManager().getPlayingState()).getPlayer();
            String originalHair = preview.getHairStyle();
            for (int i = 0; i < 6; i++) {
                click(game[0], robot, 939, 456);
            }
            require(preview.getHairStyle().equals(originalHair), "All hairstyle previews must cycle back");
            click(game[0], robot, 749, 456);
            require(!preview.getHairStyle().equals(originalHair), "Left hairstyle arrow must work");
            click(game[0], robot, 939, 456);
            click(game[0], robot, 180, 392);
            boolean muted = game[0].getAssetManager().isMuted();
            click(game[0], robot, 582, 254);
            require(game[0].getAssetManager().isMuted() != muted, "Options mute hitbox must match its sprite");
            click(game[0], robot, 582, 254);
            captureWindow(robot, origin, "screenshot_native_options.png");
            click(game[0], robot, 512, 450);
            captureWindow(robot, origin, "screenshot_native_menu.png");
            click(game[0], robot, 181, 538);
            require(game[0].getAssetManager().getExampleScene() != null, "Credits artwork must be loaded");
            captureWindow(robot, origin, "screenshot_native_credits.png");
            click(game[0], robot, 512, 450);
            click(game[0], robot, 180, 320);
            robot.delay(250);
            captureWindow(robot, origin, "screenshot_native_start.png");
            entities.Player farmer = ((states.PlayingState) game[0].getStateManager().getPlayingState()).getPlayer();
            int startX = farmer.getWorldX();
            key(robot, java.awt.event.KeyEvent.VK_D, true);
            robot.delay(220);
            key(robot, java.awt.event.KeyEvent.VK_D, false);
            require(farmer.getWorldX() > startX, "Click Play and keyboard movement must work in the native window");
            tap(game[0], robot, java.awt.event.KeyEvent.VK_E);
            int bagX = farmer.getWorldX();
            key(robot, java.awt.event.KeyEvent.VK_D, true);
            robot.delay(180);
            key(robot, java.awt.event.KeyEvent.VK_D, false);
            require(farmer.getWorldX() == bagX, "Opening backpack must block world movement");
            captureWindow(robot, origin, "screenshot_native_bag.png");
            click(game[0], robot, 430, 307);
            require(farmer.getSeedForSlot(4) == farming.HarvestedCrop.CropType.RADISH, "Backpack must equip all crop varieties");
            robot.mousePress(java.awt.event.InputEvent.BUTTON3_DOWN_MASK);
            robot.mouseRelease(java.awt.event.InputEvent.BUTTON3_DOWN_MASK);
            robot.waitForIdle();
            robot.delay(150);
            require(farmer.getProgress().getSeeds(farming.HarvestedCrop.CropType.RADISH) == 5, "Backpack must buy real seed stock");
            tap(game[0], robot, java.awt.event.KeyEvent.VK_RIGHT);
            require(farmer.getWorldX() == bagX, "Backpack input must not move the player");
            tap(game[0], robot, java.awt.event.KeyEvent.VK_E);
            tap(game[0], robot, java.awt.event.KeyEvent.VK_M);
            captureWindow(robot, origin, "screenshot_native_world_map.png");
            tap(game[0], robot, java.awt.event.KeyEvent.VK_M);
            tap(game[0], robot, java.awt.event.KeyEvent.VK_ESCAPE);
            int pausedX = farmer.getWorldX();
            key(robot, java.awt.event.KeyEvent.VK_D, true);
            robot.delay(180);
            key(robot, java.awt.event.KeyEvent.VK_D, false);
            require(farmer.getWorldX() == pausedX, "Pause must block world movement");
            click(game[0], robot, 417, 427);
            require(game[0].getAssetManager().getMasterVolume() == 0f, "Slider minimum must reach zero");
            click(game[0], robot, 607, 427);
            require(game[0].getAssetManager().getMasterVolume() == 1f, "Slider maximum must reach one");
            click(game[0], robot, 569, 427);
            click(game[0], robot, 582, 262);
            require(game[0].getAssetManager().isMuted() != muted, "Pause mute must match the visible control");
            click(game[0], robot, 582, 262);
            captureWindow(robot, origin, "screenshot_native_pause.png");
            tap(game[0], robot, java.awt.event.KeyEvent.VK_ESCAPE);
            int energy = farmer.getEnergy();
            click(game[0], robot, 350, 574);
            robot.delay(800);
            require(game[0].getKeyHandler().selectedSlot == 1, "Hotbar visual and click target must agree");
            require(farmer.getEnergy() == energy, "Hotbar clicks must not use world tools");
            captureWindow(robot, origin, "screenshot_native_play.png");
            System.out.println("PASS: native Play, movement, backpack, pause/resume and hotbar pointer isolation");
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> window[0].dispose());
        }
    }

    private static void key(java.awt.Robot robot, int code, boolean pressed) {
        if (pressed) robot.keyPress(code); else robot.keyRelease(code);
        robot.waitForIdle();
    }

    private static void tap(GamePanel panel, java.awt.Robot robot, int code) throws Exception {
        key(robot, code, true);
        key(robot, code, false);
        robot.delay(150);
    }

    private static void click(GamePanel panel, java.awt.Robot robot, int x, int y) throws Exception {
        java.awt.Point origin = panel.getLocationOnScreen();
        robot.mouseMove(origin.x + x, origin.y + y);
        robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
        robot.delay(180);
    }

    private static void captureWindow(java.awt.Robot robot, java.awt.Point origin, String name) throws Exception {
        ImageIO.write(robot.createScreenCapture(new java.awt.Rectangle(origin.x, origin.y, 1024, 640)),
            "PNG", new File("bin/" + name));
    }
}
