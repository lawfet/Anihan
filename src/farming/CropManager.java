package farming;

import managers.AssetManager;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * CropManager manages all tilled soil, hydration, planting, growth, and harvesting.
 */
public final class CropManager {

    private final Map<String, Soil> soils = new HashMap<>();
    private int animationTick;

    public CropManager() {
        // The back rows of the fenced field start tilled and planted so the farm
        // looks alive from the first second. The front two rows stay open for play.
        HarvestedCrop.CropType[] columns = {
            HarvestedCrop.CropType.CARROT, HarvestedCrop.CropType.CABBAGE,
            HarvestedCrop.CropType.WHEAT, HarvestedCrop.CropType.PUMPKIN,
            HarvestedCrop.CropType.SUNFLOWER, HarvestedCrop.CropType.BEETROOT,
            HarvestedCrop.CropType.CAULIFLOWER, HarvestedCrop.CropType.POTATO
        };
        for (int y = 10; y <= 13; y++) {
            for (int x = 17; x <= 24; x++) {
                tillSoil(x, y);
            }
        }
        for (int x = 17; x <= 24; x++) {
            HarvestedCrop.CropType crop = columns[x - 17];
            // Mature back row, then progressively younger rows toward the path.
            growCrop(x, 10, crop, crop.daysToMature, true);
            growCrop(x, 11, crop, 3, x % 2 == 0);
            growCrop(x, 12, crop, 1, true);
            plantSeed(x, 13, crop);
            Soil fresh = getSoil(x, 13);
            if (fresh != null && x % 3 != 0) fresh.water();
        }
        HarvestedCrop.CropType[] allCrops = HarvestedCrop.CropType.values();
        for (int i = 0; i < allCrops.length; i++) {
            HarvestedCrop.CropType crop = allCrops[i];
            int x = 82 + i;
            for (int y = 56; y <= 61; y++) tillSoil(x, y);
            growCrop(x, 56, crop, crop.daysToMature, true);
            growCrop(x, 57, crop, 2, true);
            growCrop(x, 58, crop, 1, true);
            plantSeed(x, 59, crop);
        }
    }

    /** Waters and advances a sample crop so the first view shows real growth. */
    private void growCrop(int x, int y, HarvestedCrop.CropType crop, int days, boolean leaveWatered) {
        plantSeed(x, y, crop);
        Soil soil = getSoil(x, y);
        if (soil == null) return;
        for (int day = 0; day < days; day++) {
            soil.water();
            soil.advanceDay();
        }
        if (leaveWatered) soil.water();
    }

    private String key(int x, int y) {
        return x + "," + y;
    }

    public boolean tillSoil(int x, int y) {
        String k = key(x, y);
        if (!soils.containsKey(k)) {
            soils.put(k, new Soil(x, y));
            return true;
        }
        return false;
    }

    public boolean waterSoil(int x, int y) {
        Soil soil = getSoil(x, y);
        if (soil != null && !soil.isWatered()) {
            soil.water();
            return true;
        }
        return false;
    }

    public boolean plantSeed(int x, int y, HarvestedCrop.CropType crop) {
        Soil soil = getSoil(x, y);
        if (soil != null && !soil.hasCrop()) {
            return soil.plant(crop);
        }
        return false;
    }

    public HarvestedCrop.CropType harvest(int x, int y) {
        Soil soil = getSoil(x, y);
        if (soil != null) {
            return soil.harvest();
        }
        return null;
    }

    public Soil getSoil(int x, int y) {
        return soils.get(key(x, y));
    }

    public boolean isSoil(int x, int y) {
        return soils.containsKey(key(x, y));
    }

    public void advanceDay() {
        for (Soil s : soils.values()) {
            s.advanceDay();
        }
    }

    public void update() {
        animationTick++;
    }

    public void render(Graphics2D g2, int cameraX, int cameraY, int tileSize, AssetManager assetManager) {
        java.awt.Rectangle viewport = g2.getClipBounds();
        if (viewport == null) viewport = new java.awt.Rectangle(0, 0, 1024, 640);
        for (Soil soil : soils.values()) {
            int screenX = soil.getTileX() * tileSize - cameraX;
            int screenY = soil.getTileY() * tileSize - cameraY;

            // Frustum culling: skip offscreen soil
            if (!viewport.intersects(screenX - 16, screenY - 96, tileSize + 32, tileSize + 96)) {
                continue;
            }

            // Draw soil tile (alternating native dry/watered variants keep the
            // field from looking like one repeated stamp).
            boolean alternate = Math.floorMod(soil.getTileX() * 7 + soil.getTileY() * 13, 3) == 0;
            BufferedImage soilImg;
            if (soil.isWatered()) {
                soilImg = alternate ? assetManager.getSoilWateredAlt() : assetManager.getSoilWatered();
            } else {
                soilImg = alternate ? assetManager.getSoilDryAlt() : assetManager.getSoilDry();
            }
            if (soilImg != null) {
                g2.drawImage(soilImg, screenX, screenY + 8, soilImg.getWidth() * 2, soilImg.getHeight() * 2, null);
            }

            // Draw planted crop if present
            if (soil.hasCrop()) {
                // Assets 0 and 5 are inventory items; only 1..4 are rooted plants.
                int stage = 1 + soil.getGrowthStage() * 3 / 5;
                BufferedImage cropImg = assetManager.getCropStageImage(soil.getPlantedCrop(), stage);
                if (cropImg != null) {
                    int cropW = cropImg.getWidth() * 2;
                    int cropH = cropImg.getHeight() * 2;
                    int cropX = screenX + (tileSize - cropW) / 2;
                    int cropY = screenY + 24 - cropH;
                    double phase = (animationTick + soil.getTileX() * 7 + soil.getTileY() * 13) * 0.045;
                    double sway = Math.sin(phase) * 0.035;
                    Graphics2D cropGraphics = (Graphics2D) g2.create();
                    cropGraphics.rotate(sway, screenX + tileSize / 2.0, screenY + 24.0);
                    cropGraphics.drawImage(cropImg, cropX, cropY, cropW, cropH, null);
                    cropGraphics.dispose();
                }
            }
        }
    }
}
