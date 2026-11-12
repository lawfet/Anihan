package inventory;

import entities.Player;
import farming.HarvestedCrop;
import inputs.KeyHandler;
import inputs.MouseHandler;
import managers.AssetManager;
import time.GameClock;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * InventoryUI renders the hotbar, backpack inventory screen, health/stamina stat bars,
 * and time/gold HUD.
 */
public class InventoryUI {

    private final Player player;
    private final KeyHandler keyH;
    private final MouseHandler mouseH;
    private final AssetManager assetM;
    private final GameClock clock;

    private static final int SLOT_SIZE = 48;
    private static final int SLOT_GAP = 6;
    private static final int HOTBAR_SLOTS = 9;
    private static final int HOTBAR_WIDTH = HOTBAR_SLOTS * SLOT_SIZE + (HOTBAR_SLOTS - 1) * SLOT_GAP;
    private static final Color INK = new Color(51, 50, 61);
    private static final Color PAPER = new Color(255, 240, 210);
    // Updated by render; correct for the initial 1024x640 viewport, too.
    private int viewportWidth = 1024;
    private int viewportHeight = 640;
    private final BufferedImage[] produceIcons = new BufferedImage[HOTBAR_SLOTS];

    public boolean isOpen = false;
    private boolean consumedClick;

    // Item definitions in hotbar
    public static class InventoryItem {
        public final String name;
        public final String description;
        public final BufferedImage icon;
        public int quantity;

        public InventoryItem(String name, String description, BufferedImage icon, int quantity) {
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.quantity = quantity;
        }
    }

    private final InventoryItem[] hotbar = new InventoryItem[HOTBAR_SLOTS];

    public InventoryUI(Player player, KeyHandler keyH, MouseHandler mouseH, AssetManager assetM, GameClock clock) {
        this.player = player;
        this.keyH = keyH;
        this.mouseH = mouseH;
        this.assetM = assetM;
        this.clock = clock;
        initItems();
    }

    private void initItems() {
        hotbar[0] = new InventoryItem("Hoe", "Tills grass & dirt into soil", assetM.getUiImage("shovel"), 1);
        hotbar[1] = new InventoryItem("Watering Can", "Hydrates tilled soil (Refill at pond)", assetM.getUiImage("water"), 1);
        hotbar[2] = new InventoryItem("Axe", "Chops fallen wood & branches", assetM.getUiImage("axe"), 1);
        hotbar[3] = new InventoryItem("Fishing Rod", "Face water, cast, then strike when it bites", assetM.getUiImage("rod"), 1);
        hotbar[4] = new InventoryItem("Carrot Seeds", "Plants crunchy carrots (3 days)",
                assetM.getCropStageImage(HarvestedCrop.CropType.CARROT, 0), 10);
        hotbar[5] = new InventoryItem("Cabbage Seeds", "Plants sweet cabbage (4 days)",
                assetM.getCropStageImage(HarvestedCrop.CropType.CABBAGE, 0), 8);
        hotbar[6] = new InventoryItem("Wheat Seeds", "Plants golden wheat (4 days)",
                assetM.getCropStageImage(HarvestedCrop.CropType.WHEAT, 0), 12);
        hotbar[7] = new InventoryItem("Pumpkin Seeds", "Plants giant pumpkins (5 days)",
                assetM.getCropStageImage(HarvestedCrop.CropType.PUMPKIN, 0), 5);
        hotbar[8] = new InventoryItem("Sunflower Seeds", "Plants bright sunflowers (4 days)",
                assetM.getCropStageImage(HarvestedCrop.CropType.SUNFLOWER, 0), 6);
        HarvestedCrop.CropType[] crops = {
            HarvestedCrop.CropType.CARROT, HarvestedCrop.CropType.CABBAGE, HarvestedCrop.CropType.WHEAT,
            HarvestedCrop.CropType.PUMPKIN, HarvestedCrop.CropType.SUNFLOWER
        };
        for (int i = 0; i < crops.length; i++) {
            produceIcons[i + 4] = assetM.getCropStageImage(crops[i], 5);
        }
    }

    private Rectangle hotbarBounds() {
        // Leave the native panel's 30px sides, 24px top and 30px foot unobscured.
        return new Rectangle((viewportWidth - HOTBAR_WIDTH) / 2 - 30,
                viewportHeight - SLOT_SIZE - 74, HOTBAR_WIDTH + 60, SLOT_SIZE + 62);
    }

    private Rectangle slotBounds(int index) {
        Rectangle bar = hotbarBounds();
        return new Rectangle(bar.x + 30 + index * (SLOT_SIZE + SLOT_GAP), bar.y + 32, SLOT_SIZE, SLOT_SIZE);
    }

    /** The open backpack is modal; hotbar padding and gaps also block world clicks. */
    public boolean consumesPointer() {
        return consumedClick || isOpen || hotbarBounds().contains(mouseH.mouseX, mouseH.mouseY);
    }

    public void update() {
        consumedClick = isOpen && mouseH.leftClicked;
        if (keyH.isJustPressed(java.awt.event.KeyEvent.VK_E) || keyH.isJustPressed(java.awt.event.KeyEvent.VK_I)) {
            isOpen = !isOpen;
            assetM.playClick();
        }

        refreshSeeds();
        if (isOpen) {
            updateBackpack();
            return;
        }
        // Handle mouse wheel scrolling through hotbar
        if (mouseH.wheelRotation != 0) {
            keyH.selectedSlot = (keyH.selectedSlot + mouseH.wheelRotation) % hotbar.length;
            if (keyH.selectedSlot < 0) keyH.selectedSlot += hotbar.length;
        }

        // Do not click through the backpack to the hotbar beneath its dimmer.
        if (!isOpen && mouseH.leftClicked) {
            for (int i = 0; i < hotbar.length; i++) {
                if (slotBounds(i).contains(mouseH.mouseX, mouseH.mouseY)) {
                    keyH.selectedSlot = i;
                    assetM.playClick();
                    break;
                }
            }
        }
    }

    private void refreshSeeds() {
        for (int slot = 4; slot < HOTBAR_SLOTS; slot++) {
            HarvestedCrop.CropType crop = player.getSeedForSlot(slot);
            hotbar[slot] = new InventoryItem(crop.name + " Seeds", crop.daysToMature + " watered days to mature",
                    assetM.getCropStageImage(crop, 0), player.getProgress().getSeeds(crop));
            produceIcons[slot] = assetM.getCropStageImage(crop, 5);
        }
    }

    private Rectangle backpackBounds() {
        return new Rectangle((viewportWidth - 800) / 2, (viewportHeight - 490) / 2, 800, 490);
    }

    private Rectangle cropBounds(int index) {
        Rectangle bag = backpackBounds();
        return new Rectangle(bag.x + 28 + index % 6 * 124, bag.y + 88 + index / 6 * 104, 116, 96);
    }

    private void updateBackpack() {
        Rectangle bag = backpackBounds();
        if (mouseH.leftClicked && new Rectangle(bag.x + 738, bag.y + 24, 32, 30).contains(mouseH.mouseX, mouseH.mouseY)) {
            isOpen = false;
            return;
        }
        if (mouseH.leftClicked || mouseH.rightClicked) {
            HarvestedCrop.CropType[] crops = HarvestedCrop.CropType.values();
            for (int i = 0; i < crops.length; i++) {
                if (!cropBounds(i).contains(mouseH.mouseX, mouseH.mouseY)) continue;
                if (mouseH.rightClicked) {
                    boolean bought = player.getProgress().buySeeds(crops[i]);
                    player.showFloatingText(bought ? "Bought 5 " + crops[i].name + " seeds" : "Not enough gold", Color.YELLOW);
                    if (bought) assetM.playCoin();
                } else {
                    if (keyH.selectedSlot < 4) keyH.selectedSlot = 4;
                    player.setSeedForSlot(keyH.selectedSlot, crops[i]);
                    assetM.playClick();
                }
                refreshSeeds();
                break;
            }
        }
    }

    public void render(Graphics2D graphics, int screenW, int screenH) {
        viewportWidth = screenW;
        viewportHeight = screenH;
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        renderStatBars(g2);
        renderClockAndGold(g2, screenW);
        renderHotbar(g2, screenW);
        if (isOpen) renderBackpack(g2, screenW, screenH);
        g2.dispose();
    }

    private void renderStatBars(Graphics2D g2) {
        int x = 20;
        int y = 20;
        BufferedImage statBars = assetM.getStatBars();
        if (statBars != null) g2.drawImage(statBars, x, y, null);

        // Use the native red/green bar artwork for health and stamina.
        float healthRatio = player.getMaxHealth() > 0 ? (float) player.getHealth() / player.getMaxHealth() : 0f;
        float energyRatio = player.getMaxEnergy() > 0 ? (float) player.getEnergy() / player.getMaxEnergy() : 0f;
        drawBarState(g2, "redbar", 6, healthRatio, x + 34, y + 10, 150, 10);
        drawBarState(g2, "greenbar", 6, energyRatio, x + 44, y + 30, 104, 10);

        // Mood face reacts to stamina.
        int mood = 1 + Math.round(Math.max(0f, Math.min(1f, energyRatio)) * 3f);
        drawUi2x(g2, String.format("happiness_%02d", mood), x + 8, y + 38);

        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        drawText(g2, player.getHealth() + " / " + player.getMaxHealth(), x + 200, y + 21);
        drawText(g2, player.getEnergy() + " / " + player.getMaxEnergy(), x + 164, y + 39);
    }

    /** Picks the correct pre-drawn bar state so every health/energy frame is used. */
    private void drawBarState(Graphics2D g2, String family, int maxIndex, float ratio,
                              int x, int y, int width, int height) {
        int index = Math.round(Math.max(0f, Math.min(1f, ratio)) * maxIndex);
        BufferedImage image = assetM.getUiImage(family + "_" + String.format("%02d", index));
        if (image != null) {
            g2.drawImage(image, x, y, width, height, null);
        }
    }

    private void renderClockAndGold(Graphics2D g2, int screenW) {
        int w = 272;
        int x = screenW - w - 20;
        int y = 20;
        assetM.drawPanel(g2, x, y, w, 130);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        drawText(g2, clock.getDateFormatted(), x + 36, y + 44);
        drawUi2x(g2, "sandtimer", x + 238, y + 32);
        drawUi2x(g2, "stopwatch", x + 36, y + 48);
        drawText(g2, clock.getTimeFormatted(), x + 64, y + 67);
        drawUi2x(g2, "basket", x + 36, y + 74);
        drawText(g2, player.getGold() + " G", x + 64, y + 92);
        drawUi2x(g2, "water", x + 150, y + 74);
        drawText(g2, player.getWaterCanCurrent() + "/" + player.getWaterCanMax(), x + 184, y + 92);
        float waterRatio = player.getWaterCanMax() > 0
                ? (float) player.getWaterCanCurrent() / player.getWaterCanMax() : 0f;
        drawBarState(g2, "bluebar", 5, waterRatio, x + 36, y + 104, 200, 10);
    }

    private void renderHotbar(Graphics2D g2, int screenW) {
        Rectangle bar = hotbarBounds();
        assetM.drawPanel(g2, bar.x, bar.y, bar.width, bar.height);
        for (int i = 0; i < hotbar.length; i++) {
            Rectangle slot = slotBounds(i);
            renderSlot(g2, slot.x, slot.y, slot.width, i, i == keyH.selectedSlot);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            drawText(g2, String.valueOf(i + 1), slot.x + 3, slot.y + 11);
        }

        InventoryItem activeItem = hotbar[keyH.selectedSlot];
        if (activeItem != null) {
            String tip = activeItem.name + " - " + activeItem.description;
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            int textWidth = g2.getFontMetrics().stringWidth(tip);
            int x = (screenW - textWidth - 32) / 2;
            int y = bar.y - 30;
            assetM.drawLabel(g2, x, y, textWidth + 32);
            drawText(g2, tip, x + 16, y + 18);
        }
    }

    private void renderSlot(Graphics2D g2, int x, int y, int size, int index, boolean selected) {
        BufferedImage disc = assetM.getUiImage(selected ? "itemdisc_01" : "itemdisc_02");
        drawCentered2x(g2, disc, x + size / 2, y + size / 2);
        if (selected) {
            drawSelection(g2, x, y, size, size);
            BufferedImage indicator = assetM.getUiImage("indicator");
            if (indicator != null) {
                g2.drawImage(indicator, x + size / 2 - indicator.getWidth(),
                        y - indicator.getHeight() * 2 - 1,
                        indicator.getWidth() * 2, indicator.getHeight() * 2, null);
            }
        }
        if (index < hotbar.length && hotbar[index] != null) {
            InventoryItem item = hotbar[index];
            if (produceIcons[index] != null) {
                // Seed and produce both keep their native aspect ratios at 2x.
                drawCentered2x(g2, produceIcons[index], x + size / 2 + 2, y + size / 2 - 2);
                if (item.icon != null) {
                    g2.drawImage(item.icon, x + 4, y + size - item.icon.getHeight() * 2 - 3,
                            item.icon.getWidth() * 2, item.icon.getHeight() * 2, null);
                }
            } else {
                drawCentered2x(g2, item.icon, x + size / 2, y + size / 2);
            }
            if (index >= 4) {
                String quantity = String.valueOf(item.quantity);
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                drawText(g2, quantity, x + size - g2.getFontMetrics().stringWidth(quantity) - 4, y + size - 4);
            }
        }
    }

    private void drawSelection(Graphics2D g2, int x, int y, int width, int height) {
        String[] corners = {"selectbox_tl", "selectbox_tr", "selectbox_bl", "selectbox_br"};
        for (int i = 0; i < corners.length; i++) {
            BufferedImage image = assetM.getUiImage(corners[i]);
            if (image == null) continue;
            int iw = image.getWidth() * 2;
            int ih = image.getHeight() * 2;
            g2.drawImage(image, x + (i % 2 == 0 ? 0 : width - iw),
                    y + (i < 2 ? 0 : height - ih), iw, ih, null);
        }
    }

    private void renderBackpack(Graphics2D g2, int screenW, int screenH) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, screenW, screenH);
        Rectangle bag = backpackBounds();
        assetM.drawPanel(g2, bag.x, bag.y, bag.width, bag.height);
        assetM.drawLabel(g2, bag.x + 28, bag.y + 24, 680);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        drawText(g2, "FARMER'S BACKPACK - SEEDS & HARVEST", bag.x + 42, bag.y + 43);
        drawUi2x(g2, "cancel", bag.x + 738, bag.y + 24);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        drawText(g2, "Click crop: equip seeds in slot 5-9. Right-click: buy 5 seeds. E / I: close.", bag.x + 28, bag.y + 72);
        HarvestedCrop.CropType[] crops = HarvestedCrop.CropType.values();
        for (int i = 0; i < crops.length; i++) {
            Rectangle card = cropBounds(i);
            HarvestedCrop.CropType crop = crops[i];
            g2.setColor(new Color(58, 38, 40, 175));
            g2.fillRoundRect(card.x, card.y, card.width, card.height, 8, 8);
            if (player.getSeedForSlot(keyH.selectedSlot) == crop) drawSelection(g2, card.x, card.y, card.width, card.height);
            drawCentered2x(g2, assetM.getCropStageImage(crop, 5), card.x + 30, card.y + 30);
            drawCentered2x(g2, assetM.getCropStageImage(crop, 0), card.x + 83, card.y + 30);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            drawText(g2, crop.name, card.x + 8, card.y + 54);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            drawText(g2, "Seeds " + player.getProgress().getSeeds(crop) + " / Crop " + player.getProgress().getCrop(crop), card.x + 8, card.y + 72);
            drawText(g2, "Pack: " + player.getProgress().seedPrice(crop) + " G", card.x + 8, card.y + 88);
        }
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        drawText(g2, "STORED MATERIALS", bag.x + 28, bag.y + 324);
        farming.FarmProgress.Material[] materials = farming.FarmProgress.Material.values();
        for (int i = 0; i < materials.length; i++) {
            int x = bag.x + 28 + i * 105;
            drawCentered2x(g2, assetM.getMaterialImage(materials[i]), x + 22, bag.y + 350);
            drawText(g2, "x" + player.getProgress().getMaterial(materials[i]), x + 48, bag.y + 354);
            drawText(g2, materials[i].label, x, bag.y + 380);
        }
        drawText(g2, "Harvests and fish are stored here. Use a tool facing the shipping bin to sell.", bag.x + 28, bag.y + 413);
        drawText(g2, "Gold: " + player.getGold() + " G   |   Farmer level: " + player.getProgress().getLevel(), bag.x + 28, bag.y + 452);
    }

    private void drawUi2x(Graphics2D g2, String name, int x, int y) {
        BufferedImage image = assetM.getUiImage(name);
        if (image != null) g2.drawImage(image, x, y, image.getWidth() * 2, image.getHeight() * 2, null);
    }

    private void drawCentered2x(Graphics2D g2, BufferedImage image, int centerX, int centerY) {
        if (image != null) {
            g2.drawImage(image, centerX - image.getWidth(), centerY - image.getHeight(),
                    image.getWidth() * 2, image.getHeight() * 2, null);
        }
    }

    private void drawText(Graphics2D g2, String text, int x, int y) {
        g2.setColor(INK);
        g2.drawString(text, x + 1, y + 1);
        g2.setColor(PAPER);
        g2.drawString(text, x, y);
    }
}
