package entities;

import farming.CropManager;
import farming.HarvestedCrop;
import inputs.KeyHandler;
import inputs.MouseHandler;
import inventory.InventoryUI;
import managers.AssetManager;
import managers.TileManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Player handles movement, stamina/health stats, hair/tool layered animations,
 * and farming interactions with tools and the world.
 */
public class Player extends Entity {

    private final KeyHandler keyH;
    private final MouseHandler mouseH;
    private final TileManager tileM;
    private final CropManager cropM;
    private final AssetManager assetM;

    // Player Stats
    private int maxHealth = 100;
    private int health = 100;
    private int maxEnergy = 100;
    private int energy = 100;
    private final farming.FarmProgress progress = new farming.FarmProgress();
    private final HarvestedCrop.CropType[] seedSlots = {
        HarvestedCrop.CropType.CARROT, HarvestedCrop.CropType.CABBAGE,
        HarvestedCrop.CropType.WHEAT, HarvestedCrop.CropType.PUMPKIN,
        HarvestedCrop.CropType.SUNFLOWER
    };
    private HarvestedCrop.CropType actionSeed;
    private int waterCanMax = 6;
    private int waterCanCurrent = 6;

    // Customization
    private String hairStyle = "curlyhair";

    // Action animation states
    private AssetManager.Action currentAction = AssetManager.Action.IDLE;
    private boolean isPerformingAction = false;
    private int movementTick;
    private int sprintTick;
    private double movementRemainderX;
    private double movementRemainderY;
    private int actionTargetTileX;
    private int actionTargetTileY;

    // Floating notification message (e.g. "+1 Carrot", "Can Refilled!")
    private String floatingText = null;
    private int floatingTextTimer = 0;
    private Color floatingTextColor = Color.WHITE;
    private String expression = null;
    private int expressionTimer = 0;

    // Fishing mini-game state
    private static final int FISHING_NONE = 0;
    private static final int FISHING_WAITING = 1;
    private static final int FISHING_BITE = 2;
    private static final int FISHING_REELING = 3;
    private static final int FISHING_CAUGHT = 4;
    private int fishingState = FISHING_NONE;
    private int fishingTimer = 0;
    private int bobberTileX;
    private int bobberTileY;
    private int fishCaught = 0;
    private int woodGathered = 0;
    private final Random rng = new Random();

    public Player(int startTileX, int startTileY, KeyHandler keyH, MouseHandler mouseH,
                  TileManager tileM, CropManager cropM, AssetManager assetM) {
        super(startTileX * TileManager.TILE_SIZE, startTileY * TileManager.TILE_SIZE);
        this.keyH = keyH;
        this.mouseH = mouseH;
        this.tileM = tileM;
        this.cropM = cropM;
        this.assetM = assetM;

        // Player collision bounds: 20x16 centered at player feet
        this.solidArea = new Rectangle(38, 44, 20, 16);
    }

    @Override
    public void update() {
        if (floatingTextTimer > 0) {
            floatingTextTimer--;
            if (floatingTextTimer == 0) {
                floatingText = null;
            }
        }
        if (expressionTimer > 0) {
            expressionTimer--;
            if (expressionTimer == 0) {
                expression = null;
            }
        }

        // If currently in an action animation (digging, watering, chopping), update animation only
        if (isPerformingAction) {
            updateActionAnimation();
            return;
        }

        // Fishing locks the farmer in place while the line is out.
        if (fishingState == FISHING_WAITING || fishingState == FISHING_BITE) {
            updateFishing();
            return;
        }

        // Check tool / interact trigger
        boolean actionRequested = keyH.space || mouseH.leftClicked;
        if (actionRequested) {
            triggerToolAction();
            return;
        }

        // Movement input
        boolean moving = false;
        int dx = 0;
        int dy = 0;

        if (keyH.up) {
            dy -= 1;
            direction = Direction.UP;
            moving = true;
        }
        if (keyH.down) {
            dy += 1;
            direction = Direction.DOWN;
            moving = true;
        }
        if (keyH.left) {
            dx -= 1;
            direction = Direction.LEFT;
            facingLeft = true;
            moving = true;
        }
        if (keyH.right) {
            dx += 1;
            direction = Direction.RIGHT;
            facingLeft = false;
            moving = true;
        }

        boolean running = keyH.shift && (energy > 5);
        int currentSpeed = running ? 5 : 3;

        if (moving) {
            int previousX = worldX;
            int previousY = worldY;
            double movementLength = dx != 0 && dy != 0 ? Math.sqrt(2.0) : 1.0;
            double requestedX = dx * currentSpeed / movementLength + movementRemainderX;
            double requestedY = dy * currentSpeed / movementLength + movementRemainderY;
            int stepX = (int) Math.round(requestedX);
            int stepY = (int) Math.round(requestedY);

            // Test horizontal collision
            if (dx == 0) {
                movementRemainderX = 0;
            } else if (stepX == 0 || !checkCollision(worldX + stepX, worldY)) {
                worldX += stepX;
                movementRemainderX = requestedX - stepX;
            } else {
                movementRemainderX = 0;
            }
            // Test vertical collision
            if (dy == 0) {
                movementRemainderY = 0;
            } else if (stepY == 0 || !checkCollision(worldX, worldY + stepY)) {
                worldY += stepY;
                movementRemainderY = requestedY - stepY;
            } else {
                movementRemainderY = 0;
            }

            boolean moved = worldX != previousX || worldY != previousY;
            AssetManager.Action movementAction = moved
                    ? (running ? AssetManager.Action.RUN : AssetManager.Action.WALK)
                    : AssetManager.Action.IDLE;
            if (currentAction != movementAction) {
                frameIndex = 0;
                animTick = 0;
            }
            currentAction = movementAction;

            if (moved) {
                movementTick++;
                if (movementTick % (running ? 16 : 22) == 0) {
                    assetM.playStep();
                }
                if (running) {
                    sprintTick++;
                    if (sprintTick % 40 == 0) {
                        energy = Math.max(0, energy - 1);
                    }
                } else {
                    sprintTick = 0;
                }
            } else {
                movementTick = 0;
                sprintTick = 0;
            }
        } else {
            movementTick = 0;
            sprintTick = 0;
            movementRemainderX = 0;
            movementRemainderY = 0;
            if (currentAction != AssetManager.Action.IDLE) {
                frameIndex = 0;
                animTick = 0;
            }
            currentAction = AssetManager.Action.IDLE;
        }

        // Advance animation frames
        int ticksPerFrame = (currentAction == AssetManager.Action.RUN) ? 4 :
                            (currentAction == AssetManager.Action.WALK) ? 6 : 8;

        animTick++;
        if (animTick >= ticksPerFrame) {
            animTick = 0;
            frameIndex = (frameIndex + 1) % currentAction.frameCount;
        }
    }

    private boolean checkCollision(int targetX, int targetY) {
        int feetLeft = targetX + solidArea.x;
        int feetRight = targetX + solidArea.x + solidArea.width - 1;
        int feetTop = targetY + solidArea.y;
        int feetBottom = targetY + solidArea.y + solidArea.height - 1;

        return tileM.isSolid(feetLeft, feetTop) ||
               tileM.isSolid(feetRight, feetTop) ||
               tileM.isSolid(feetLeft, feetBottom) ||
               tileM.isSolid(feetRight, feetBottom);
    }

    private void triggerToolAction() {
        // Target tile directly in front of player
        int targetX = (worldX + 48) / TileManager.TILE_SIZE;
        int targetY = (worldY + 48) / TileManager.TILE_SIZE;

        switch (direction) {
            case UP:    targetY -= 1; break;
            case DOWN:  targetY += 1; break;
            case LEFT:  targetX -= 1; break;
            case RIGHT: targetX += 1; break;
        }

        this.actionTargetTileX = targetX;
        this.actionTargetTileY = targetY;

        // Check what hotbar slot is selected
        int slot = keyH.selectedSlot;

        // Slot 0: Hoe (Dig)
        if (slot == 0) {
            if (energy >= 2) {
                startAction(AssetManager.Action.DIG);
            } else {
                showFloatingText("Too exhausted!", Color.RED);
                showExpression("expression_stress");
            }
        }
        // Slot 1: Watering Can
        else if (slot == 1) {
            // Check if facing water pond: refill can!
            if (tileM.isWater(targetX, targetY)) {
                waterCanCurrent = waterCanMax;
                assetM.playWater();
                showFloatingText("Watering Can Refilled! (" + waterCanCurrent + "/" + waterCanMax + ")", new Color(100, 220, 255));
                showExpression("expression_alerted");
                startAction(AssetManager.Action.WATERING);
                return;
            }
            if (waterCanCurrent <= 0) {
                showFloatingText("Watering Can empty! Refill at pond.", Color.ORANGE);
                showExpression("expression_confused");
                return;
            }
            if (energy >= 1) {
                startAction(AssetManager.Action.WATERING);
            } else {
                showFloatingText("Too exhausted!", Color.RED);
                showExpression("expression_stress");
            }
        }
        // Slot 2: Axe
        else if (slot == 2) {
            if (energy >= 3) {
                startAction(AssetManager.Action.AXE);
            } else {
                showFloatingText("Too exhausted!", Color.RED);
                showExpression("expression_stress");
            }
        }
        // Slot 3: Fishing rod
        else if (slot == 3) {
            if (tileM.isWater(targetX, targetY)) {
                if (energy >= 1) {
                    bobberTileX = targetX;
                    bobberTileY = targetY;
                    fishingState = FISHING_WAITING;
                    assetM.playWater();
                    showExpression("expression_working");
                    startAction(AssetManager.Action.CASTING);
                } else {
                    showFloatingText("Too exhausted to fish!", Color.RED);
                    showExpression("expression_stress");
                }
            } else {
                showFloatingText("Face the water to cast your line.", Color.LIGHT_GRAY);
                showExpression("expression_confused");
            }
        }
        // Slots 4..8: Seeds or Interaction
        else {
            actionSeed = getSeedForSlot(slot);
            startAction(AssetManager.Action.DOING);
        }
    }

    private void startAction(AssetManager.Action action) {
        this.currentAction = action;
        this.isPerformingAction = true;
        this.animTick = 0;
        this.frameIndex = 0;
        this.movementTick = 0;
        this.sprintTick = 0;
        this.movementRemainderX = 0;
        this.movementRemainderY = 0;
    }

    private void updateActionAnimation() {
        int ticksPerFrame = (currentAction == AssetManager.Action.DIG) ? 3 :
                            (currentAction == AssetManager.Action.WATERING) ? 5 : 4;

        animTick++;
        if (animTick >= ticksPerFrame) {
            animTick = 0;
            frameIndex++;

            // Trigger mid-action effect
            if (frameIndex == currentAction.frameCount / 2) {
                executeActionEffect();
            }

            // End of action animation
            if (frameIndex >= currentAction.frameCount) {
                isPerformingAction = false;
                AssetManager.Action finished = currentAction;
                currentAction = AssetManager.Action.IDLE;
                frameIndex = 0;
                animTick = 0;

                if (finished == AssetManager.Action.CASTING) {
                    currentAction = AssetManager.Action.WAITING;
                    fishingState = FISHING_WAITING;
                    fishingTimer = 120 + rng.nextInt(240); // 2-6 seconds for a bite
                    showFloatingText("Waiting for a bite...", new Color(214, 240, 255));
                } else if (finished == AssetManager.Action.REELING) {
                    isPerformingAction = true;
                    currentAction = AssetManager.Action.CAUGHT;
                    fishingState = FISHING_CAUGHT;
                    showExpression("expression_alerted");
                } else if (finished == AssetManager.Action.CAUGHT) {
                    finishFishing();
                }
            }
        }
    }

    /** Runs the bite-wait timer and lets the player strike with SPACE. */
    private void updateFishing() {
        animTick++;
        if (animTick >= 8) {
            animTick = 0;
            frameIndex = (frameIndex + 1) % AssetManager.Action.WAITING.frameCount;
        }

        fishingTimer--;
        if (fishingState == FISHING_WAITING && fishingTimer <= 0) {
            fishingState = FISHING_BITE;
            fishingTimer = 90; // 1.5 second reaction window
            frameIndex = 0;
            animTick = 0;
            assetM.playWater();
            showFloatingText("A fish is biting! Press SPACE!", Color.YELLOW);
            showExpression("expression_alerted");
            return;
        }

        if (fishingState == FISHING_BITE) {
            if (fishingTimer % 12 == 0) {
                assetM.playStep();
            }
            if (keyH.space || mouseH.leftClicked) {
                fishingState = FISHING_REELING;
                assetM.playWater();
                startAction(AssetManager.Action.REELING);
            } else if (fishingTimer <= 0) {
                fishingState = FISHING_NONE;
                currentAction = AssetManager.Action.IDLE;
                frameIndex = 0;
                animTick = 0;
                showFloatingText("It got away...", new Color(205, 225, 245));
                showExpression("expression_stress");
            }
        }
    }

    private void finishFishing() {
        progress.addMaterial(farming.FarmProgress.Material.FISH, 1);
        fishCaught++;
        energy = Math.max(0, energy - 1);
        fishingState = FISHING_NONE;
        currentAction = AssetManager.Action.IDLE;
        frameIndex = 0;
        animTick = 0;
        assetM.playHarvest();
        showFloatingText("+1 Fish stored in backpack", new Color(255, 226, 118));
        showExpression("expression_love");
    }

    private void executeActionEffect() {
        int tx = actionTargetTileX;
        int ty = actionTargetTileY;

        // Fishing has its own success/failure flow.
        if (currentAction == AssetManager.Action.CASTING || currentAction == AssetManager.Action.WAITING
                || currentAction == AssetManager.Action.REELING || currentAction == AssetManager.Action.CAUGHT) {
            return;
        }

        // 1. Check Shipping Bin interaction first
        if (tileM.isShippingBin(tx, ty) || tileM.isShippingBin(tx, ty + 1)) {
            int earned = progress.sellAll();
            showFloatingText(earned > 0 ? "Sold goods for " + earned + " G!" : "No goods to sell.", Color.YELLOW);
            showExpression("expression_chat");
            if (earned > 0) assetM.playCoin();
            return;
        }

        // 2. Check bed / house door interaction
        if (tileM.isHouseDoor(tx, ty)) {
            showFloatingText("Home sweet home!", Color.GREEN);
            showExpression("expression_love");
            return;
        }

        // 3. Check mature crop harvest
        if (cropM.isSoil(tx, ty)) {
            HarvestedCrop.CropType harvested = cropM.harvest(tx, ty);
            if (harvested != null) {
                progress.addCrop(harvested, 1);
                showFloatingText("+1 " + harvested.name + " stored in backpack", Color.YELLOW);
                showExpression("expression_love");
                assetM.playHarvest();
                return;
            }
        }

        // 4. Hoe tool
        if (currentAction == AssetManager.Action.DIG) {
            if (tileM.isTillable(tx, ty)) {
                if (cropM.tillSoil(tx, ty)) {
                    energy = Math.max(0, energy - 2);
                    assetM.playDig();
                    showFloatingText("Tilled Soil", new Color(180, 240, 150));
                    showExpression("expression_working");
                }
            }
            return;
        }

        // 5. Watering can tool
        if (currentAction == AssetManager.Action.WATERING) {
            if (cropM.isSoil(tx, ty)) {
                if (cropM.waterSoil(tx, ty)) {
                    waterCanCurrent = Math.max(0, waterCanCurrent - 1);
                    energy = Math.max(0, energy - 1);
                    assetM.playWater();
                    showFloatingText("Soil Watered (" + waterCanCurrent + "/" + waterCanMax + ")", new Color(120, 210, 255));
                    showExpression("expression_working");
                }
            }
            return;
        }

        // 6. Axe tool
        if (currentAction == AssetManager.Action.AXE) {
            if (tileM.chopTree(tx, ty)) {
                energy = Math.max(0, energy - 3);
                woodGathered += 2;
                progress.addMaterial(farming.FarmProgress.Material.WOOD, 2);
                assetM.playChop();
                showFloatingText("+2 Wood Gathered", new Color(220, 170, 120));
                showExpression("expression_attack");
            } else {
                showFloatingText("Face a tree to chop wood.", Color.LIGHT_GRAY);
                showExpression("expression_confused");
            }
            return;
        }

        // 7. Seed planting
        if (currentAction == AssetManager.Action.DOING) {
            HarvestedCrop.CropType seedToPlant = actionSeed;

            if (seedToPlant != null) {
                if (progress.getSeeds(seedToPlant) == 0) {
                    showFloatingText("No seeds left. Buy a pack in the backpack.", Color.ORANGE);
                    return;
                }
                if (cropM.plantSeed(tx, ty, seedToPlant)) {
                    progress.consumeSeed(seedToPlant);
                    assetM.playDig();
                    showFloatingText("Planted " + seedToPlant.name + " Seed", Color.GREEN);
                    showExpression("expression_working");
                } else {
                    showFloatingText("Till soil first!", Color.LIGHT_GRAY);
                    showExpression("expression_confused");
                }
            }
        }
    }

    public void showFloatingText(String text, Color color) {
        this.floatingText = text;
        this.floatingTextColor = color;
        this.floatingTextTimer = 90; // ~1.5 seconds at 60fps
    }

    public void showExpression(String expressionName) {
        this.expression = expressionName;
        this.expressionTimer = 80;
    }

    public void restoreEnergy(int amount) {
        this.energy = Math.min(maxEnergy, this.energy + amount);
    }

    public void restoreHealth(int amount) {
        this.health = Math.min(maxHealth, this.health + amount);
    }

    public void onNewDay() {
        isPerformingAction = false;
        fishingState = FISHING_NONE;
        currentAction = AssetManager.Action.IDLE;
        frameIndex = 0;
        animTick = 0;
        progress.nextDay();
        this.energy = maxEnergy;
        this.waterCanCurrent = waterCanMax;
        showFloatingText("A new day dawns! Energy restored.", Color.CYAN);
        showExpression("expression_love");
    }

    @Override
    public void render(Graphics2D g2, int cameraX, int cameraY) {
        int screenX = worldX - cameraX;
        int screenY = worldY - cameraY;

        // 1. Draw subtle soft shadow under character
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(screenX + 34, screenY + 52, 28, 10);

        // 2. Fetch frames for Base, Hair, and Equipment
        BufferedImage baseImg = assetM.getBaseFrame(currentAction, frameIndex);
        BufferedImage hairImg = assetM.getHairFrame(hairStyle, currentAction, frameIndex);
        BufferedImage toolImg = assetM.getToolFrame(currentAction, frameIndex);

        // The 96x64 strip includes padding; anchor the visible feet, not the frame edge.
        int frameW = 192;
        int frameH = 128;
        int drawX = screenX - 48;
        int drawY = screenY - 22;

        if (facingLeft) {
            // Flip horizontally: draw at (screenX + frameW) with negative width
            if (baseImg != null) {
                g2.drawImage(baseImg, drawX + frameW, drawY, -frameW, frameH, null);
            }
            if (hairImg != null) {
                g2.drawImage(hairImg, drawX + frameW, drawY, -frameW, frameH, null);
            }
            if (toolImg != null) {
                g2.drawImage(toolImg, drawX + frameW, drawY, -frameW, frameH, null);
            }
        } else {
            // Facing right / normal
            if (baseImg != null) {
                g2.drawImage(baseImg, drawX, drawY, frameW, frameH, null);
            }
            if (hairImg != null) {
                g2.drawImage(hairImg, drawX, drawY, frameW, frameH, null);
            }
            if (toolImg != null) {
                g2.drawImage(toolImg, drawX, drawY, frameW, frameH, null);
            }
        }

        // 3. Fishing bobber, line, ripple and fish shadow
        if (fishingState != FISHING_NONE) {
            int bx = bobberTileX * TileManager.TILE_SIZE + 16 - cameraX;
            int by = bobberTileY * TileManager.TILE_SIZE + 16 - cameraY;

            // Expanding ripple rings on the water.
            int ripple = (animTick / 5) % 3;
            g2.setColor(new Color(230, 255, 255, 120));
            g2.drawOval(bx - 9 - ripple * 3, by - 4, 18 + ripple * 6, 8);

            // A shy fish shadow circles just below the surface.
            BufferedImage fishShadow = assetM.getItemImage("fish");
            if (fishShadow != null && (fishingState == FISHING_WAITING || fishingState == FISHING_BITE)) {
                Graphics2D fishLayer = (Graphics2D) g2.create();
                fishLayer.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                int fishSway = (int) (Math.sin(animTick * 0.12) * 14);
                fishLayer.drawImage(fishShadow, bx - 12 + fishSway, by + 7, 24, 24, null);
                fishLayer.dispose();
            }

            // Line from the rod tip to the float.
            g2.setColor(new Color(245, 245, 225, 190));
            g2.drawLine(screenX + 54, screenY + 30, bx, by - 4);

            // Red-and-white bobber; it dips when a fish bites.
            int dip = fishingState == FISHING_BITE ? (int) (Math.sin(animTick * 0.8) * 2) + 2 : 0;
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(bx - 3, by - 4 + dip, 6, 8, 3, 3);
            g2.setColor(new Color(218, 72, 60));
            g2.fillRect(bx - 3, by - 4 + dip, 6, 3);

            if (fishingState == FISHING_BITE) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 18));
                g2.setColor(new Color(255, 235, 100));
                g2.drawString("!", bx - 2, by - 14 + dip);
            }
        }

        // 4. Draw target tile selection indicator on ground
        int targetTileX = (worldX + 48) / TileManager.TILE_SIZE;
        int targetTileY = (worldY + 48) / TileManager.TILE_SIZE;
        switch (direction) {
            case UP: targetTileY -= 1; break;
            case DOWN: targetTileY += 1; break;
            case LEFT: targetTileX -= 1; break;
            case RIGHT: targetTileX += 1; break;
        }
        int selScreenX = targetTileX * TileManager.TILE_SIZE - cameraX;
        int selScreenY = targetTileY * TileManager.TILE_SIZE - cameraY;

        g2.drawImage(assetM.getUiImage("selectbox_tl"), selScreenX, selScreenY, 12, 12, null);
        g2.drawImage(assetM.getUiImage("selectbox_tr"), selScreenX + 20, selScreenY, 12, 12, null);
        g2.drawImage(assetM.getUiImage("selectbox_bl"), selScreenX, selScreenY + 20, 12, 12, null);
        g2.drawImage(assetM.getUiImage("selectbox_br"), selScreenX + 20, selScreenY + 20, 12, 12, null);

        // 5. Render expression bubble above the farmer
        if (expression != null && expressionTimer > 0) {
            BufferedImage img = assetM.getUiImage(expression);
            if (img != null) {
                int scale = 3;
                int bob = (int) (Math.sin(expressionTimer * 0.25) * 2);
                int ex = screenX + 48 - img.getWidth() * scale / 2;
                int ey = screenY - 14 - img.getHeight() * scale + bob;
                g2.setColor(new Color(255, 255, 255, 190));
                g2.fillOval(ex - 5, ey - 5, img.getWidth() * scale + 10, img.getHeight() * scale + 10);
                g2.drawImage(img, ex, ey, img.getWidth() * scale, img.getHeight() * scale, null);
            }
        }

        // 6. Render floating popup text
        if (floatingText != null && floatingTextTimer > 0) {
            int textY = screenY - (90 - floatingTextTimer) / 3 - 6;
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(floatingText);
            int textX = screenX + 48 - tw / 2;

            // Text shadow
            g2.setColor(new Color(0, 0, 0, 180));
            g2.drawString(floatingText, textX + 1, textY + 1);

            // Text
            g2.setColor(floatingTextColor);
            g2.drawString(floatingText, textX, textY);
        }
    }

    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getEnergy() { return energy; }
    public int getMaxEnergy() { return maxEnergy; }
    public int getGold() { return progress.getGold(); }
    public void addGold(int amount) { progress.addGold(amount); }
    public farming.FarmProgress getProgress() { return progress; }
    public HarvestedCrop.CropType getSeedForSlot(int slot) {
        return slot >= 4 && slot <= 8 ? seedSlots[slot - 4] : null;
    }
    public void setSeedForSlot(int slot, HarvestedCrop.CropType crop) {
        seedSlots[slot - 4] = crop;
    }
    public int getWaterCanCurrent() { return waterCanCurrent; }
    public int getWaterCanMax() { return waterCanMax; }
    public int getFishCaught() { return fishCaught; }
    public int getWoodGathered() { return woodGathered; }
    public String getHairStyle() { return hairStyle; }
    public void setHairStyle(String hairStyle) { this.hairStyle = hairStyle; }
}
