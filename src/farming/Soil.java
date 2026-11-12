package farming;

/**
 * Soil represents a tilled farming tile on the farm.
 * It tracks hydration status and the crop planted on it.
 */
public class Soil {

    private final int tileX;
    private final int tileY;
    private boolean isWatered = false;
    private HarvestedCrop.CropType plantedCrop = null;
    private int growthStage = 0; // 0 to 5 (5 is mature and ready to harvest)
    private int daysGrowing = 0;

    public Soil(int tileX, int tileY) {
        this.tileX = tileX;
        this.tileY = tileY;
    }

    public boolean plant(HarvestedCrop.CropType crop) {
        if (plantedCrop == null) {
            this.plantedCrop = crop;
            this.growthStage = 0;
            this.daysGrowing = 0;
            return true;
        }
        return false;
    }

    public void water() {
        this.isWatered = true;
    }

    public void advanceDay() {
        if (plantedCrop != null) {
            if (isWatered) {
                daysGrowing++;
                // Calculate growth stage based on days growing vs days to mature
                float ratio = (float) daysGrowing / plantedCrop.daysToMature;
                growthStage = Math.min(5, (int) Math.floor(ratio * 5.0f));
            }
        }
        // Soil dries up over night
        this.isWatered = false;
    }

    public boolean isMature() {
        return plantedCrop != null && growthStage >= 5;
    }

    public HarvestedCrop.CropType harvest() {
        if (isMature()) {
            HarvestedCrop.CropType harvested = this.plantedCrop;
            // Clear crop after harvesting
            this.plantedCrop = null;
            this.growthStage = 0;
            this.daysGrowing = 0;
            return harvested;
        }
        return null;
    }

    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public boolean isWatered() { return isWatered; }
    public HarvestedCrop.CropType getPlantedCrop() { return plantedCrop; }
    public int getGrowthStage() { return growthStage; }
    public boolean hasCrop() { return plantedCrop != null; }
}
