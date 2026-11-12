package farming;

import farming.HarvestedCrop.CropType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/** In-memory farm economy. All compound actions validate before changing state. */
public final class FarmProgress {
    public enum Material {
        WOOD("Wood", "wood", 4),
        STONE("Stone", "rock", 6),
        FISH("Fish", "fish", 35),
        EGG("Egg", "egg", 20),
        MILK("Milk", "milk", 35),
        MUSHROOM("Mushroom", "mushroom", 18),
        FLOUR("Flour", "flour", 35);

        public final String label;
        public final String asset;
        public final int sellPrice;

        Material(String label, String asset, int sellPrice) {
            this.label = label;
            this.asset = asset;
            this.sellPrice = sellPrice;
        }
    }

    public enum Upgrade {
        WATERING_CAN("Watering Can"),
        BOOTS("Boots"),
        STAMINA("Stamina");

        public final String label;

        Upgrade(String label) {
            this.label = label;
        }
    }

    private static final String PREFIX = "progress.";
    private static final int SAVE_VERSION = 1;
    private static final int MAX_UPGRADE = 3;
    private static final int SEED_PACK_SIZE = 5;
    private static final int CROP_XP = 3;
    private static final int MATERIAL_XP = 2;
    private static final int PET_XP = 4;
    private static final int ORDER_XP = 15;
    private static final int REPAIR_XP = 30;
    private static final int[] MILESTONE_REWARDS = {50, 60, 80, 70, 200};
    private static final int[] MILESTONE_XP = {10, 15, 20, 20, 50};
    private static final String[] MILESTONE_TITLES = {
        "A first harvest", "Build a foundation", "Gone fishing",
        "A little kindness", "Bring back the windmill"
    };

    private static final class Order {
        final String title;
        final CropType crop;
        final Material material;
        final int quantity;
        final int reward;

        Order(String title, CropType crop, int quantity, int reward) {
            this.title = title;
            this.crop = crop;
            this.material = null;
            this.quantity = quantity;
            this.reward = reward;
        }

        Order(String title, Material material, int quantity, int reward) {
            this.title = title;
            this.crop = null;
            this.material = material;
            this.quantity = quantity;
            this.reward = reward;
        }

        String itemName() {
            return crop == null ? material.label : crop.name;
        }
    }

    // A saved cursor keeps the active request stable when the windmill is repaired.
    private static final Order[] ORDERS = {
        new Order("A crunchy delivery", CropType.CARROT, 3, 75),
        new Order("Greens for the village", CropType.CABBAGE, 2, 80),
        new Order("The baker's grain", CropType.WHEAT, 4, 105),
        new Order("Fresh catch", Material.FISH, 2, 100),
        new Order("Breakfast basket", Material.EGG, 3, 90),
        new Order("Dairy delivery", Material.MILK, 2, 100),
        new Order("Pumpkin supper", CropType.PUMPKIN, 2, 120),
        new Order("A sunny bouquet", CropType.SUNFLOWER, 3, 140),
        new Order("Forest flavors", Material.MUSHROOM, 3, 80),
        new Order("Flour for the bakery", Material.FLOUR, 4, 170)
    };

    private final EnumMap<CropType, Integer> seeds = new EnumMap<>(CropType.class);
    private final EnumMap<CropType, Integer> crops = new EnumMap<>(CropType.class);
    private final EnumMap<CropType, Integer> lifetimeCrops = new EnumMap<>(CropType.class);
    private final EnumMap<Material, Integer> materials = new EnumMap<>(Material.class);
    private final EnumMap<Material, Integer> lifetimeMaterials = new EnumMap<>(Material.class);
    private final EnumMap<Upgrade, Integer> upgrades = new EnumMap<>(Upgrade.class);
    private final EnumSet<CropType> discoveredCrops = EnumSet.noneOf(CropType.class);
    private final EnumSet<Material> discoveredMaterials = EnumSet.noneOf(Material.class);
    private int gold = 120;
    private int xp;
    private int pets;
    private int milestone;
    private int ordersCompleted;
    private int orderIndex;
    private int day = 1;
    private boolean windmillRepaired;

    public FarmProgress() {
        for (CropType type : CropType.values()) {
            boolean starter = type == CropType.CARROT || type == CropType.CABBAGE
                    || type == CropType.WHEAT || type == CropType.PUMPKIN
                    || type == CropType.SUNFLOWER;
            seeds.put(type, starter ? 10 : 0);
            crops.put(type, 0);
            lifetimeCrops.put(type, 0);
        }
        for (Material material : Material.values()) {
            materials.put(material, 0);
            lifetimeMaterials.put(material, 0);
        }
        for (Upgrade upgrade : Upgrade.values()) {
            upgrades.put(upgrade, 0);
        }
    }

    public int getGold() {
        return gold;
    }

    public void addGold(int amount) {
        requireAmount(amount);
        gold = checkedAdd(gold, amount, "Gold");
    }

    public boolean spendGold(int amount) {
        requireAmount(amount);
        if (gold < amount) {
            return false;
        }
        gold -= amount;
        return true;
    }

    public int getSeeds(CropType type) {
        return seeds.get(requireItem(type));
    }

    public boolean consumeSeed(CropType type) {
        int stock = getSeeds(type);
        if (stock == 0) {
            return false;
        }
        seeds.put(type, stock - 1);
        return true;
    }

    public void addSeeds(CropType type, int amount) {
        requireAmount(amount);
        seeds.put(requireItem(type), checkedAdd(getSeeds(type), amount, "Seeds"));
    }

    public int getCrop(CropType type) {
        return crops.get(requireItem(type));
    }

    public void addCrop(CropType type, int amount) {
        requireAmount(amount);
        int stock = checkedAdd(getCrop(type), amount, "Crop stock");
        int lifetime = checkedAdd(lifetimeCrops.get(type), amount, "Lifetime crops");
        int newXp = checkedAdd(xp, (long) amount * CROP_XP, "XP");
        crops.put(type, stock);
        lifetimeCrops.put(type, lifetime);
        xp = newXp;
        if (amount > 0) {
            discoveredCrops.add(type);
        }
    }

    public boolean consumeCrop(CropType type, int amount) {
        requireAmount(amount);
        int stock = getCrop(type);
        if (stock < amount) {
            return false;
        }
        crops.put(type, stock - amount);
        return true;
    }

    public int getMaterial(Material material) {
        return materials.get(requireItem(material));
    }

    public void addMaterial(Material material, int amount) {
        requireAmount(amount);
        int stock = checkedAdd(getMaterial(material), amount, "Material stock");
        int lifetime = checkedAdd(lifetimeMaterials.get(material), amount, "Lifetime materials");
        int newXp = checkedAdd(xp, (long) amount * MATERIAL_XP, "XP");
        materials.put(material, stock);
        lifetimeMaterials.put(material, lifetime);
        xp = newXp;
        if (amount > 0) {
            discoveredMaterials.add(material);
        }
    }

    public boolean consumeMaterial(Material material, int amount) {
        requireAmount(amount);
        int stock = getMaterial(material);
        if (stock < amount) {
            return false;
        }
        materials.put(material, stock - amount);
        return true;
    }

    public int getXp() {
        return xp;
    }

    public int getLevel() {
        return 1 + xp / 50;
    }

    public int getUpgradeLevel(Upgrade upgrade) {
        return upgrades.get(requireItem(upgrade));
    }

    // Zero denotes an already-maximized upgrade; buyUpgrade still returns false.
    public int upgradePrice(Upgrade upgrade) {
        int level = getUpgradeLevel(upgrade);
        if (level == MAX_UPGRADE) {
            return 0;
        }
        int base = upgrade == Upgrade.WATERING_CAN ? 90
                : upgrade == Upgrade.BOOTS ? 80 : 100;
        return base * (level + 1);
    }

    public boolean buyUpgrade(Upgrade upgrade) {
        int level = getUpgradeLevel(upgrade);
        int price = upgradePrice(upgrade);
        if (level == MAX_UPGRADE || gold < price) {
            return false;
        }
        gold -= price;
        upgrades.put(upgrade, level + 1);
        return true;
    }

    public int seedPrice(CropType type) {
        requireItem(type);
        return checkedAdd(0, Math.max(5L, (long) type.sellPrice * 2), "Seed pack price");
    }

    public boolean buySeeds(CropType type) {
        int price = seedPrice(type);
        if (gold < price) {
            return false;
        }
        int stock = checkedAdd(getSeeds(type), SEED_PACK_SIZE, "Seeds");
        gold -= price;
        seeds.put(type, stock);
        return true;
    }

    public int sellAll() {
        long earned = 0;
        for (CropType type : CropType.values()) {
            earned += (long) crops.get(type) * type.sellPrice;
        }
        for (Material material : Material.values()) {
            if (isSellable(material)) {
                earned += (long) materials.get(material) * material.sellPrice;
            }
        }
        int proceeds = checkedAdd(0, earned, "Sale proceeds");
        int newGold = checkedAdd(gold, earned, "Gold");
        for (CropType type : CropType.values()) {
            crops.put(type, 0);
        }
        for (Material material : Material.values()) {
            if (isSellable(material)) {
                materials.put(material, 0);
            }
        }
        gold = newGold;
        return proceeds;
    }

    public void recordPet() {
        int newPets = checkedAdd(pets, 1, "Pets");
        int newXp = checkedAdd(xp, PET_XP, "XP");
        pets = newPets;
        xp = newXp;
    }

    public int getPets() {
        return pets;
    }

    public int getCollectionCount() {
        return discoveredCrops.size() + discoveredMaterials.size();
    }

    public int getCollectionTotal() {
        return CropType.values().length + Material.values().length;
    }

    public boolean hasDiscovered(CropType type) {
        return discoveredCrops.contains(requireItem(type));
    }

    public boolean hasDiscovered(Material material) {
        return discoveredMaterials.contains(requireItem(material));
    }

    public String milestoneTitle() {
        return isRestored() ? "Farm restored" : MILESTONE_TITLES[milestone];
    }

    public String milestoneDescription() {
        String task;
        switch (milestone) {
            case 0:
                task = "Harvest 3 crops (" + Math.min(3L, totalHarvested()) + "/3).";
                break;
            case 1:
                task = "Gather 5 Wood (" + Math.min(5, lifetimeMaterials.get(Material.WOOD))
                        + "/5) and 3 Stone (" + Math.min(3, lifetimeMaterials.get(Material.STONE)) + "/3).";
                break;
            case 2:
                task = "Catch 2 Fish (" + Math.min(2, lifetimeMaterials.get(Material.FISH)) + "/2).";
                break;
            case 3:
                task = "Pet animals 3 times (" + Math.min(3, pets) + "/3).";
                break;
            case 4:
                task = "Repair the windmill (" + (windmillRepaired ? "complete" : "not repaired") + ").";
                break;
            default:
                return "All five milestones claimed. Your farm is restored!";
        }
        return task + " Reward: " + MILESTONE_REWARDS[milestone] + "G and "
                + MILESTONE_XP[milestone] + " XP.";
    }

    public boolean milestoneReady() {
        return !isRestored() && milestoneRequirementMet(milestone);
    }

    public boolean claimMilestone() {
        if (!milestoneReady()) {
            return false;
        }
        int newGold = checkedAdd(gold, MILESTONE_REWARDS[milestone], "Gold");
        int newXp = checkedAdd(xp, MILESTONE_XP[milestone], "XP");
        gold = newGold;
        xp = newXp;
        milestone++;
        return true;
    }

    public int getMilestone() {
        return milestone;
    }

    public boolean isRestored() {
        return milestone == MILESTONE_REWARDS.length;
    }

    public String orderTitle() {
        return ORDERS[orderIndex].title;
    }

    public String orderDescription() {
        Order order = ORDERS[orderIndex];
        return "Deliver " + order.quantity + " " + order.itemName() + " ("
                + orderStock(order) + "/" + order.quantity + "). Reward: "
                + order.reward + "G and " + ORDER_XP + " XP.";
    }

    public boolean orderReady() {
        Order order = ORDERS[orderIndex];
        return (order.material != Material.FLOUR || windmillRepaired)
                && orderStock(order) >= order.quantity;
    }

    public boolean fulfillOrder() {
        if (!orderReady()) {
            return false;
        }
        Order order = ORDERS[orderIndex];
        int newGold = checkedAdd(gold, order.reward, "Gold");
        int newXp = checkedAdd(xp, ORDER_XP, "XP");
        int completed = checkedAdd(ordersCompleted, 1, "Completed orders");
        if (order.crop != null) {
            crops.put(order.crop, getCrop(order.crop) - order.quantity);
        } else {
            materials.put(order.material, getMaterial(order.material) - order.quantity);
        }
        gold = newGold;
        xp = newXp;
        ordersCompleted = completed;
        do {
            orderIndex = (orderIndex + 1) % ORDERS.length;
        } while (!windmillRepaired && ORDERS[orderIndex].material == Material.FLOUR);
        return true;
    }

    public int getOrdersCompleted() {
        return ordersCompleted;
    }

    public int getOrderReward() {
        return ORDERS[orderIndex].reward;
    }

    public boolean isWindmillRepaired() {
        return windmillRepaired;
    }

    public String windmillDescription() {
        if (windmillRepaired) {
            return "Windmill repaired. Mill 3 Wheat into 2 Flour.";
        }
        return "Repair with 8 Wood (" + getMaterial(Material.WOOD) + "/8), 6 Stone ("
                + getMaterial(Material.STONE) + "/6) and 120G (" + gold + "/120).";
    }

    public boolean repairWindmill() {
        if (windmillRepaired || gold < 120 || getMaterial(Material.WOOD) < 8
                || getMaterial(Material.STONE) < 6) {
            return false;
        }
        int newXp = checkedAdd(xp, REPAIR_XP, "XP");
        materials.put(Material.WOOD, getMaterial(Material.WOOD) - 8);
        materials.put(Material.STONE, getMaterial(Material.STONE) - 6);
        gold -= 120;
        xp = newXp;
        windmillRepaired = true;
        return true;
    }

    public boolean millWheat() {
        if (!windmillRepaired || getCrop(CropType.WHEAT) < 3) {
            return false;
        }
        // addMaterial performs all overflow checks before touching any fields.
        addMaterial(Material.FLOUR, 2);
        crops.put(CropType.WHEAT, getCrop(CropType.WHEAT) - 3);
        return true;
    }

    public int consumeSnack() {
        if (getMaterial(Material.MUSHROOM) > 0) {
            consumeMaterial(Material.MUSHROOM, 1);
            return 25;
        }
        CropType snack = null;
        for (CropType type : CropType.values()) {
            if (getCrop(type) > 0 && type.energyRestore > 0
                    && (snack == null || type.sellPrice < snack.sellPrice)) {
                snack = type;
            }
        }
        if (snack == null) {
            return 0;
        }
        consumeCrop(snack, 1);
        return snack.energyRestore;
    }

    public int getDay() {
        return day;
    }

    public void nextDay() {
        day = checkedAdd(day, 1, "Day");
    }

    public void writeTo(Properties properties) {
        requireItem(properties);
        Properties snapshot = snapshot();
        // Replace just our namespace, preserving unrelated game save fields.
        for (Object key : new HashSet<Object>(properties.keySet())) {
            if (key instanceof String && ((String) key).startsWith(PREFIX)) {
                properties.remove(key);
            }
        }
        properties.putAll(snapshot);
    }

    public void readFrom(Properties properties) {
        requireItem(properties);
        FarmProgress loaded = new FarmProgress();
        Set<String> savedKeys = new HashSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(PREFIX)) {
                savedKeys.add(key);
            }
        }
        for (Object key : properties.keySet()) {
            if (key instanceof String && ((String) key).startsWith(PREFIX)) {
                if (!(properties.get(key) instanceof String)) {
                    throw invalid((String) key, "must contain text");
                }
                savedKeys.add((String) key);
            }
        }
        // Older game saves have no progression namespace at all.
        if (!savedKeys.isEmpty()) {
            Set<String> expectedKeys = loaded.snapshot().stringPropertyNames();
            for (String key : savedKeys) {
                if (!expectedKeys.contains(key)) {
                    throw invalid(key, "unknown progression field");
                }
            }
            for (String key : expectedKeys) {
                if (!savedKeys.contains(key)) {
                    throw invalid(key, "missing progression field");
                }
            }
            loaded.loadSnapshot(properties);
            loaded.validateSnapshot();
        }
        // Commit only after every field and cross-field invariant passes.
        copyFrom(loaded);
    }

    private Properties snapshot() {
        Properties properties = new Properties();
        put(properties, "version", SAVE_VERSION);
        put(properties, "gold", gold);
        put(properties, "xp", xp);
        put(properties, "day", day);
        put(properties, "goals.pets", pets);
        put(properties, "goals.milestone", milestone);
        put(properties, "goals.windmillRepaired", windmillRepaired);
        put(properties, "orders.completed", ordersCompleted);
        put(properties, "orders.index", orderIndex);
        for (CropType type : CropType.values()) {
            put(properties, "seeds." + type.name(), seeds.get(type));
            put(properties, "crops." + type.name(), crops.get(type));
            put(properties, "lifetime.crops." + type.name(), lifetimeCrops.get(type));
            put(properties, "discovered.crops." + type.name(), discoveredCrops.contains(type));
        }
        for (Material material : Material.values()) {
            put(properties, "materials." + material.name(), materials.get(material));
            put(properties, "lifetime.materials." + material.name(), lifetimeMaterials.get(material));
            put(properties, "discovered.materials." + material.name(), discoveredMaterials.contains(material));
        }
        for (Upgrade upgrade : Upgrade.values()) {
            put(properties, "upgrades." + upgrade.name(), upgrades.get(upgrade));
        }
        return properties;
    }

    private void loadSnapshot(Properties properties) {
        readInt(properties, "version", SAVE_VERSION, SAVE_VERSION);
        gold = readInt(properties, "gold", 0, Integer.MAX_VALUE);
        xp = readInt(properties, "xp", 0, Integer.MAX_VALUE);
        day = readInt(properties, "day", 1, Integer.MAX_VALUE);
        pets = readInt(properties, "goals.pets", 0, Integer.MAX_VALUE);
        milestone = readInt(properties, "goals.milestone", 0, MILESTONE_REWARDS.length);
        windmillRepaired = readBoolean(properties, "goals.windmillRepaired");
        ordersCompleted = readInt(properties, "orders.completed", 0, Integer.MAX_VALUE);
        orderIndex = readInt(properties, "orders.index", 0, ORDERS.length - 1);
        for (CropType type : CropType.values()) {
            seeds.put(type, readInt(properties, "seeds." + type.name(), 0, Integer.MAX_VALUE));
            crops.put(type, readInt(properties, "crops." + type.name(), 0, Integer.MAX_VALUE));
            lifetimeCrops.put(type, readInt(properties, "lifetime.crops." + type.name(), 0, Integer.MAX_VALUE));
            if (readBoolean(properties, "discovered.crops." + type.name())) {
                discoveredCrops.add(type);
            }
        }
        for (Material material : Material.values()) {
            materials.put(material, readInt(properties, "materials." + material.name(), 0, Integer.MAX_VALUE));
            lifetimeMaterials.put(material,
                    readInt(properties, "lifetime.materials." + material.name(), 0, Integer.MAX_VALUE));
            if (readBoolean(properties, "discovered.materials." + material.name())) {
                discoveredMaterials.add(material);
            }
        }
        for (Upgrade upgrade : Upgrade.values()) {
            upgrades.put(upgrade, readInt(properties, "upgrades." + upgrade.name(), 0, MAX_UPGRADE));
        }
    }

    private void validateSnapshot() {
        long expectedXp = (long) pets * PET_XP + (long) ordersCompleted * ORDER_XP;
        for (CropType type : CropType.values()) {
            int lifetime = lifetimeCrops.get(type);
            if (crops.get(type) > lifetime || discoveredCrops.contains(type) != (lifetime > 0)) {
                throw invalid(PREFIX + "crops." + type.name(), "inconsistent inventory, lifetime or discovery");
            }
            expectedXp += (long) lifetime * CROP_XP;
        }
        for (Material material : Material.values()) {
            int lifetime = lifetimeMaterials.get(material);
            if (materials.get(material) > lifetime || discoveredMaterials.contains(material) != (lifetime > 0)) {
                throw invalid(PREFIX + "materials." + material.name(), "inconsistent inventory, lifetime or discovery");
            }
            expectedXp += (long) lifetime * MATERIAL_XP;
        }
        for (int index = 0; index < milestone; index++) {
            if (!milestoneRequirementMet(index)) {
                throw invalid(PREFIX + "goals.milestone", "claimed milestone requirements were not met");
            }
            expectedXp += MILESTONE_XP[index];
        }
        if (windmillRepaired) {
            if (lifetimeMaterials.get(Material.WOOD) - materials.get(Material.WOOD) < 8
                    || lifetimeMaterials.get(Material.STONE) - materials.get(Material.STONE) < 6) {
                throw invalid(PREFIX + "goals.windmillRepaired", "repair materials were not consumed");
            }
            expectedXp += REPAIR_XP;
        }
        if (expectedXp != xp) {
            throw invalid(PREFIX + "xp", "does not match lifetime activity and claimed rewards");
        }
        if (!windmillRepaired && orderIndex != ordersCompleted % (ORDERS.length - 1)) {
            throw invalid(PREFIX + "orders.index", "inconsistent order rotation before repair");
        }
        if (windmillRepaired && orderIndex > ordersCompleted) {
            throw invalid(PREFIX + "orders.index", "order cursor exceeds completed deliveries");
        }
    }

    private void copyFrom(FarmProgress other) {
        seeds.putAll(other.seeds);
        crops.putAll(other.crops);
        lifetimeCrops.putAll(other.lifetimeCrops);
        materials.putAll(other.materials);
        lifetimeMaterials.putAll(other.lifetimeMaterials);
        upgrades.putAll(other.upgrades);
        discoveredCrops.clear();
        discoveredCrops.addAll(other.discoveredCrops);
        discoveredMaterials.clear();
        discoveredMaterials.addAll(other.discoveredMaterials);
        gold = other.gold;
        xp = other.xp;
        pets = other.pets;
        milestone = other.milestone;
        ordersCompleted = other.ordersCompleted;
        orderIndex = other.orderIndex;
        day = other.day;
        windmillRepaired = other.windmillRepaired;
    }

    private boolean milestoneRequirementMet(int index) {
        switch (index) {
            case 0:
                return totalHarvested() >= 3;
            case 1:
                return lifetimeMaterials.get(Material.WOOD) >= 5
                        && lifetimeMaterials.get(Material.STONE) >= 3;
            case 2:
                return lifetimeMaterials.get(Material.FISH) >= 2;
            case 3:
                return pets >= 3;
            case 4:
                return windmillRepaired;
            default:
                return false;
        }
    }

    private long totalHarvested() {
        long total = 0;
        for (int count : lifetimeCrops.values()) {
            total += count;
        }
        return total;
    }

    private int orderStock(Order order) {
        return order.crop == null ? getMaterial(order.material) : getCrop(order.crop);
    }

    private static boolean isSellable(Material material) {
        return material != Material.WOOD && material != Material.STONE;
    }

    private static void requireAmount(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must not be negative");
        }
    }

    private static <T> T requireItem(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null");
        }
        return item;
    }

    private static int checkedAdd(int current, long amount, String label) {
        if (amount < 0 || amount > Integer.MAX_VALUE - (long) current) {
            throw new IllegalArgumentException(label + " would exceed the supported range");
        }
        return current + (int) amount;
    }

    private static void put(Properties properties, String key, Object value) {
        properties.setProperty(PREFIX + key, String.valueOf(value));
    }

    private static int readInt(Properties properties, String key, int minimum, int maximum) {
        String fullKey = PREFIX + key;
        String value = properties.getProperty(fullKey);
        if (value == null || !value.matches("[0-9]+")) {
            throw invalid(fullKey, "must be a nonnegative integer");
        }
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw invalid(fullKey, "integer is out of range");
        }
        if (number < minimum || number > maximum) {
            throw invalid(fullKey, "must be between " + minimum + " and " + maximum);
        }
        return number;
    }

    private static boolean readBoolean(Properties properties, String key) {
        String value = properties.getProperty(PREFIX + key);
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        throw invalid(PREFIX + key, "must be true or false");
    }

    private static IllegalArgumentException invalid(String key, String reason) {
        return new IllegalArgumentException("Invalid " + key + ": " + reason);
    }
}
