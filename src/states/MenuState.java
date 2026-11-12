package states;

import engine.GamePanel;
import inputs.KeyHandler;
import inputs.MouseHandler;
import managers.AssetManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MenuState presents the animated title screen, character customizer,
 * options board, and main menu buttons.
 */
public class MenuState extends State {

    private final GamePanel gamePanel;
    private final AssetManager assetM;
    private final KeyHandler keyH;
    private final MouseHandler mouseH;

    // Drifting flower petals in the breeze
    private static class Petal {
        float x, y;
        float vx, vy;
        float size;
        float angle;
        float rotSpeed;
        Color color;

        Petal(Random rng, int w, int h) {
            x = rng.nextFloat() * w;
            y = rng.nextFloat() * h;
            vx = 0.5f + rng.nextFloat() * 1.2f;
            vy = 0.3f + rng.nextFloat() * 0.8f;
            size = 4 + rng.nextFloat() * 5;
            angle = rng.nextFloat() * 360;
            rotSpeed = -2.0f + rng.nextFloat() * 4.0f;
            int r = 255;
            int g = 190 + rng.nextInt(50);
            int b = 210 + rng.nextInt(40);
            color = new Color(r, g, b, 180);
        }

        void update(int w, int h) {
            x += vx;
            y += vy;
            angle += rotSpeed;
            if (x > w + 20) x = -20;
            if (y > h + 20) y = -20;
        }

        void render(Graphics2D g2) {
            g2.setColor(color);
            g2.fillOval((int) x, (int) y, (int) size, (int) (size * 0.6f));
        }
    }

    private final List<Petal> petals = new ArrayList<>();
    private final Random rng = new Random();

    // Menu Sub-windows
    private boolean showOptions = false;
    private boolean showCredits = false;

    // Customization
    private int selectedHairIndex = 1; // "curlyhair"
    private int previewAnimTick = 0;
    private int previewFrameIndex = 0;

    // Button layout
    private final Rectangle[] buttonRects = new Rectangle[4]; // 0: Play, 1: Options, 2: Quit, 3: Credits
    private int hoveredButton = -1;
    private final Rectangle menuBoardRect = new Rectangle(40, 194, 282, 406);
    private final Rectangle customizerRect = new Rectangle();
    private final Rectangle hairLeftRect = new Rectangle();
    private final Rectangle hairRightRect = new Rectangle();
    private final Rectangle optionsRect = new Rectangle();
    private final Rectangle muteRect = new Rectangle();
    private final Rectangle closeOptionsRect = new Rectangle();
    private static final Color INK = new Color(51, 50, 61);
    private static final Color PAPER = new Color(255, 240, 210);

    public MenuState(StateManager stateManager, GamePanel gamePanel) {
        super(stateManager);
        this.gamePanel = gamePanel;
        this.assetM = gamePanel.getAssetManager();
        this.keyH = gamePanel.getKeyHandler();
        this.mouseH = gamePanel.getMouseHandler();
    }

    @Override
    public void init() {
        int w = gamePanel.getScreenWidth();
        int h = gamePanel.getScreenHeight();

        // Spawn petals
        petals.clear();
        for (int i = 0; i < 35; i++) {
            petals.add(new Petal(rng, w, h));
        }

        for (int i = 0; i < buttonRects.length; i++) {
            buttonRects[i] = new Rectangle(menuBoardRect.x + 71, menuBoardRect.y + 100 + i * 72, 140, 56);
        }
        layoutControls(w, h);
    }

    private void layoutControls(int w, int h) {
        customizerRect.setBounds(w - 310, 224, 260, 300);
        hairLeftRect.setBounds(customizerRect.x + 24, customizerRect.y + 220, 22, 24);
        hairRightRect.setBounds(customizerRect.x + 214, customizerRect.y + 220, 22, 24);
        optionsRect.setBounds((w - 282) / 2, (h - 393) / 2, 282, 393);
        muteRect.setBounds(optionsRect.x + 190, optionsRect.y + 110, 42, 42);
        closeOptionsRect.setBounds(optionsRect.x + 113, optionsRect.y + 300, 56, 56);
    }

    @Override
    public void update() {
        int w = gamePanel.getScreenWidth();
        int h = gamePanel.getScreenHeight();

        layoutControls(w, h);
        for (Petal p : petals) {
            p.update(w, h);
        }

        // Advance character preview animation
        previewAnimTick++;
        if (previewAnimTick % 8 == 0) {
            previewFrameIndex = (previewFrameIndex + 1) % 9;
        }

        int mx = mouseH.mouseX;
        int my = mouseH.mouseY;

        // Modal hitboxes are the same rectangles used to draw their sprites.
        if (showOptions) {
            if (mouseH.leftClicked) {
                if (closeOptionsRect.contains(mx, my)) {
                    showOptions = false;
                    assetM.playClick();
                } else if (muteRect.contains(mx, my)) {
                    assetM.toggleMute();
                    assetM.playClick();
                }
            }
            return;
        }

        // If Credits modal is active
        if (showCredits) {
            if (mouseH.leftClicked) {
                showCredits = false;
                assetM.playClick();
            }
            return;
        }

        if (mouseH.leftClicked && hairLeftRect.contains(mx, my)) {
            selectedHairIndex = (selectedHairIndex - 1 + AssetManager.HAIR_STYLES.length) % AssetManager.HAIR_STYLES.length;
            assetM.playClick();
            syncPlayerHair();
        } else if (mouseH.leftClicked && hairRightRect.contains(mx, my)) {
            selectedHairIndex = (selectedHairIndex + 1) % AssetManager.HAIR_STYLES.length;
            assetM.playClick();
            syncPlayerHair();
        }

        // Check menu buttons hover & clicks
        hoveredButton = -1;
        for (int i = 0; i < 4; i++) {
            if (buttonRects[i].contains(mx, my)) {
                hoveredButton = i;
                if (mouseH.leftClicked) {
                    assetM.playClick();
                    handleButtonClick(i);
                }
                break;
            }
        }
    }

    private void handleButtonClick(int index) {
        switch (index) {
            case 0: // PLAY
                syncPlayerHair();
                stateManager.setPlayingState();
                break;
            case 1: // OPTIONS
                showOptions = true;
                break;
            case 2: // QUIT
                System.exit(0);
                break;
            case 3: // CREDITS
                showCredits = true;
                break;
        }
    }

    private void syncPlayerHair() {
        if (stateManager.getPlayingState() instanceof PlayingState) {
            PlayingState ps = (PlayingState) stateManager.getPlayingState();
            if (ps.getPlayer() != null) {
                ps.getPlayer().setHairStyle(AssetManager.HAIR_STYLES[selectedHairIndex]);
            }
        }
    }

    @Override
    public void render(Graphics2D graphics) {
        int w = gamePanel.getScreenWidth();
        int h = gamePanel.getScreenHeight();
        layoutControls(w, h);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // 1. Draw Title Background Scenery
        BufferedImage bg = assetM.getTitleBg();
        if (bg != null) {
            g2.drawImage(bg, 0, 0, w, h, null);
        } else {
            g2.setColor(new Color(110, 190, 90));
            g2.fillRect(0, 0, w, h);
        }

        // 2. Render Petals
        for (Petal p : petals) {
            p.render(g2);
        }

        // 3. Render Game Title Banner "ANIHAN"
        renderTitle(g2);

        // Keep the original menu board and button sprites at native resolution.
        BufferedImage menuBoard = assetM.getMenuBoard();
        if (menuBoard != null) {
            g2.drawImage(menuBoard, menuBoardRect.x, menuBoardRect.y, null);
        } else {
            assetM.drawPanel(g2, menuBoardRect.x, menuBoardRect.y, menuBoardRect.width, menuBoardRect.height);
        }
        for (int i = 0; i < 4; i++) {
            Rectangle r = buttonRects[i];
            int col = (!showOptions && !showCredits && hoveredButton == i) ? (mouseH.leftPressed ? 2 : 1) : 0;
            BufferedImage btnImg = assetM.getMenuButton(i, col);
            if (btnImg != null) {
                g2.drawImage(btnImg, r.x, r.y, r.width, r.height, null);
            }
        }

        // 5. Render Character Customizer Preview Box
        renderCharacterCustomizer(g2);

        // 6. Modals (Options / Credits)
        if (showOptions) {
            renderOptionsModal(g2, w, h);
        } else if (showCredits) {
            renderCreditsModal(g2, w, h);
        }
        g2.dispose();
    }

    private void renderTitle(Graphics2D g2) {
        int x = 40;
        int y = 42;
        int width = 400;
        assetM.drawPanel(g2, x, y, width, 130);
        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
        drawCenteredText(g2, "ANIHAN", x, y + 67, width);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        drawCenteredText(g2, "A Cozy Farming Adventure", x, y + 97, width);
    }

    private void renderCharacterCustomizer(Graphics2D g2) {
        int cx = customizerRect.x;
        int cy = customizerRect.y;
        int cw = customizerRect.width;
        assetM.drawPanel(g2, cx, cy, cw, customizerRect.height);
        assetM.drawLabel(g2, cx + 32, cy + 28, cw - 64);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        drawCenteredText(g2, "YOUR FARMER", cx, cy + 47, cw);

        Rectangle preview = new Rectangle(cx + 60, cy + 66, 140, 128);
        drawSelection(g2, preview);
        String hair = AssetManager.HAIR_STYLES[selectedHairIndex];
        BufferedImage base = assetM.getBaseFrame(AssetManager.Action.IDLE, previewFrameIndex);
        BufferedImage hairImg = assetM.getHairFrame(hair, AssetManager.Action.IDLE, previewFrameIndex);
        // The 96x64 frame is mostly padding. Center the visible union
        // (x=39..57, y=20..39) at 4x, keeping BOTH layers at one frame origin.
        int scale = 4;
        int charDrawX = preview.x + preview.width / 2 - 194;
        int charDrawY = preview.y + (preview.height - 20 * scale) / 2 - 20 * scale;
        Graphics2D characterGraphics = (Graphics2D) g2.create();
        characterGraphics.clipRect(preview.x, preview.y, preview.width, preview.height);
        if (base != null) {
            characterGraphics.drawImage(base, charDrawX, charDrawY, base.getWidth() * scale, base.getHeight() * scale, null);
        }
        if (hairImg != null) {
            characterGraphics.drawImage(hairImg, charDrawX, charDrawY, hairImg.getWidth() * scale, hairImg.getHeight() * scale, null);
        }
        characterGraphics.dispose();

        assetM.drawLabel(g2, cx + 54, cy + 219, cw - 108);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        drawCenteredText(g2, hair.replace("hair", " Hair").toUpperCase(java.util.Locale.ROOT), cx + 54, cy + 237, cw - 108);
        drawUiControl(g2, "arrow_left", hairLeftRect);
        drawUiControl(g2, "arrow_right", hairRightRect);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        drawCenteredText(g2, "Click arrows to style", cx, cy + 262, cw);
    }

    private void renderOptionsModal(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, w, h);
        BufferedImage board = assetM.getOptionsBoard();
        if (board != null) {
            g2.drawImage(board, optionsRect.x, optionsRect.y, optionsRect.width, optionsRect.height, null);
        } else {
            assetM.drawPanel(g2, optionsRect.x, optionsRect.y, optionsRect.width, optionsRect.height);
        }
        drawImageInRect(g2, assetM.getAudioToggle(assetM.isMuted() ? 1 : 0, controlState(muteRect)), muteRect);
        drawImageInRect(g2, assetM.getNavButton(1, controlState(closeOptionsRect)), closeOptionsRect);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        drawCenteredText(g2, "BACK", optionsRect.x, optionsRect.y + 289, optionsRect.width);
    }

    private void renderCreditsModal(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, w, h);

        int bw = 560;
        int bh = 450;
        int bx = (w - bw) / 2;
        int by = (h - bh) / 2;

        assetM.drawPanel(g2, bx, by, bw, bh);
        assetM.drawLabel(g2, bx + 30, by + 24, bw - 60);
        g2.setFont(new Font("SansSerif", Font.BOLD, 17));
        drawCenteredText(g2, "ANIHAN - CREDITS", bx, by + 44, bw);

        BufferedImage scene = assetM.getExampleScene();
        if (scene != null) {
            int iw = 440;
            int ih = (int) Math.round(iw * (double) scene.getHeight() / scene.getWidth());
            int ix = bx + (bw - iw) / 2;
            int iy = by + 62;
            g2.setColor(new Color(72, 47, 36));
            g2.fillRect(ix - 4, iy - 4, iw + 8, ih + 8);
            g2.drawImage(scene, ix, iy, iw, ih, null);
            g2.setColor(new Color(255, 240, 210));
            g2.drawRect(ix - 1, iy - 1, iw + 1, ih + 1);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            drawCenteredText(g2, "A Cozy Farming Simulator created in Pure Java", bx, iy + ih + 30, bw);
            drawCenteredText(g2, "Sprites & Artwork: Sunnyside World Pixel Assets", bx, iy + ih + 50, bw);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            drawCenteredText(g2, "Click anywhere to return", bx, iy + ih + 84, bw);
        } else {
            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            drawCenteredText(g2, "A Cozy Farming Simulator created in Pure Java", bx, by + 92, bw);
            drawCenteredText(g2, "Sprites & Artwork: Sunnyside World Pixel Assets", bx, by + 130, bw);
            drawCenteredText(g2, "Layered Character Animations: Base, Hair & Tools", bx, by + 166, bw);
            drawCenteredText(g2, "Audio Synthesis: Java Sound Synthesizer Engine", bx, by + 202, bw);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            drawCenteredText(g2, "Click anywhere to return", bx, by + 270, bw);
        }
    }

    private int controlState(Rectangle rect) {
        return rect.contains(mouseH.mouseX, mouseH.mouseY) ? (mouseH.leftPressed ? 2 : 1) : 0;
    }

    private void drawUiControl(Graphics2D g2, String name, Rectangle rect) {
        drawImageInRect(g2, assetM.getUiImage(name), rect);
    }

    private void drawImageInRect(Graphics2D g2, BufferedImage image, Rectangle rect) {
        if (image != null) g2.drawImage(image, rect.x, rect.y, rect.width, rect.height, null);
    }

    private void drawSelection(Graphics2D g2, Rectangle rect) {
        String[] names = {"selectbox_tl", "selectbox_tr", "selectbox_bl", "selectbox_br"};
        for (int i = 0; i < names.length; i++) {
            BufferedImage image = assetM.getUiImage(names[i]);
            if (image == null) continue;
            int width = image.getWidth() * 2;
            int height = image.getHeight() * 2;
            int x = rect.x + (i % 2 == 0 ? 0 : rect.width - width);
            int y = rect.y + (i < 2 ? 0 : rect.height - height);
            g2.drawImage(image, x, y, width, height, null);
        }
    }

    private void drawCenteredText(Graphics2D g2, String text, int x, int baseline, int width) {
        int textX = x + (width - g2.getFontMetrics().stringWidth(text)) / 2;
        g2.setColor(INK);
        g2.drawString(text, textX + 1, baseline + 1);
        g2.setColor(PAPER);
        g2.drawString(text, textX, baseline);
    }
}
