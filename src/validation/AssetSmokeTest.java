package validation;

import managers.AssetManager;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

/**
 * Headless startup check for the game resources required by the playable build.
 * This test deliberately avoids creating a window or starting the game loop.
 */
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
            if (image == null) {
                throw new IllegalStateException("Missing or unreadable required asset: " + name);
            }
            if (image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new IllegalStateException("Invalid image dimensions: " + name);
            }
        }

        try (InputStream map = assets.openResource("maps/farm_map.txt")) {
            byte[] header = map.readNBytes(32);
            if (header.length < 4) {
                throw new IllegalStateException("Farm map file is empty or truncated.");
            }
        }

        System.out.println("Anihan smoke test passed. Indexed " + assets.getResourceNames().size()
                + " artwork files and verified " + required.size() + " required assets.");
    }
}
