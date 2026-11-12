package farming;

import java.awt.image.BufferedImage;

/**
 * HarvestedCrop represents an item yield from ripe crops.
 */
public class HarvestedCrop {

    public enum CropType {
        CARROT("Carrot", 15, 20, "carrot_05.png", "carrot_00.png", 3),
        CABBAGE("Cabbage", 25, 25, "cabbage_05.png", "cabbage_00.png", 4),
        BEETROOT("Beetroot", 20, 22, "beetroot_05.png", "beetroot_00.png", 3),
        WHEAT("Wheat", 18, 15, "wheat_05.png", "wheat_00.png", 4),
        PUMPKIN("Pumpkin", 45, 35, "pumpkin_05.png", "pumpkin_00.png", 5),
        SUNFLOWER("Sunflower", 35, 10, "sunflower_05.png", "sunflower_00.png", 4),
        CAULIFLOWER("Cauliflower", 30, 25, "cauliflower_05.png", "cauliflower_00.png", 4),
        POTATO("Potato", 16, 20, "potato_05.png", "potato_00.png", 3),
        RADISH("Radish", 22, 18, "radish_05.png", "radish_00.png", 3),
        KALE("Kale", 28, 24, "kale_05.png", "kale_00.png", 4),
        PARSNIP("Parsnip", 19, 20, "parsnip_05.png", "parsnip_00.png", 3);

        public final String name;
        public final int sellPrice;
        public final int energyRestore;
        public final String matureSpriteName;
        public final String seedSpriteName;
        public final int daysToMature;

        CropType(String name, int sellPrice, int energyRestore, String matureSpriteName, String seedSpriteName, int daysToMature) {
            this.name = name;
            this.sellPrice = sellPrice;
            this.energyRestore = energyRestore;
            this.matureSpriteName = matureSpriteName;
            this.seedSpriteName = seedSpriteName;
            this.daysToMature = daysToMature;
        }
    }

    private final CropType type;
    private int quantity;

    public HarvestedCrop(CropType type, int quantity) {
        this.type = type;
        this.quantity = quantity;
    }

    public CropType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void addQuantity(int amount) {
        this.quantity += amount;
    }

    public boolean reduceQuantity(int amount) {
        if (this.quantity >= amount) {
            this.quantity -= amount;
            return true;
        }
        return false;
    }

    public String getName() {
        return type.name;
    }

    public int getSellPrice() {
        return type.sellPrice;
    }

    public int getEnergyRestore() {
        return type.energyRestore;
    }
}
