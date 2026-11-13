package validation;

import managers.AssetManager;
import managers.TileManager;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

/** Headless validation for bundled artwork, map data and island navigation. */
public final class AssetSmokeTest {
    private AssetSmokeTest() {}

    public static void main(String[] args) throws Exception {
        AssetManager assets = AssetManager.getInstance();

        List<String> required = List.of(
                "sprites/player/idle_base_idle_strip9.png",
                "sprites/player/walking_base_walk_strip8.png",
                "sprites/crops/carrot_05.png",
                "sprites/crops/pumpkin_05.png",
                "sprites/buildings/ts_cottage_green.png",
                "sprites/animals/spr_deco_chicken_01_strip4.png",
                "ui/menu_buttons.png",
                "ui/title_bg.png",
                "maps/tileset.png"
        );

        for (String name : required) {
            BufferedImage image = assets.getResourceImage(name);
            if (image == null) throw new IllegalStateException("Missing or unreadable required asset: " + name);
            if (image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new IllegalStateException("Invalid image dimensions: " + name);
            }
        }

        if (!assets.getBrokenResourceNames().isEmpty()) {
            throw new IllegalStateException("Unreadable PNG assets: " + assets.getBrokenResourceNames());
        }

        try (InputStream map = assets.openResource("maps/farm_map.txt")) {
            byte[] header = map.readNBytes(32);
            if (header.length < 4) throw new IllegalStateException("Farm map file is empty or truncated.");
        }

        TileManager world = new TileManager(assets);
        int regions = world.getLandRegionCount();
        if (regions != 1) {
            throw new IllegalStateException("Island bridge generation left " + regions + " disconnected land regions.");
        }
        if (!world.canReachEveryLandRegionFrom(29, 14)) {
            throw new IllegalStateException("The player spawn cannot reach all island regions. Check bridge and collision tiles.");
        }

        System.out.println("Anihan validation passed: " + assets.getResourceNames().size()
                + " PNG assets loaded, no broken PNGs, " + regions
                + " connected land region, and all regions reachable from the farm spawn.");
    }
}
