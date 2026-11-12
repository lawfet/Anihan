package states;

import engine.GamePanel;
import inputs.KeyHandler;
import inputs.MouseHandler;
import managers.AssetManager;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * PauseState presents the pause overlay, volume controls, audio mute toggles,
 * and navigation buttons (resume, return to menu).
 */
public class PauseState extends State {

    private final GamePanel gamePanel;
    private final AssetManager assetM;
    private final KeyHandler keyH;
    private final MouseHandler mouseH;

    // Nav button rects: 0: Resume (green), 1: Next Day (yellow), 2: Menu (red)
    private final Rectangle[] navRects = new Rectangle[3];
    private int hoveredNav = -1;

    // Volume slider bounds
    private final Rectangle sliderRect = new Rectangle();
    private final Rectangle audioRect = new Rectangle();
    private BufferedImage sliderTrack;
    private final BufferedImage[] sliderHandles = new BufferedImage[3];
    private boolean isDraggingSlider = false;
    // Native track trough spans x=96..286 in the 299x44 slider sheet.
    private static final int SLIDER_INSET = 12;
    private static final int SLIDER_TRAVEL = 190;

    public PauseState(StateManager stateManager, GamePanel gamePanel) {
        super(stateManager);
        this.gamePanel = gamePanel;
        this.assetM = gamePanel.getAssetManager();
        this.keyH = gamePanel.getKeyHandler();
        this.mouseH = gamePanel.getMouseHandler();
    }

    @Override
    public void init() {
        isDraggingSlider = false;
        layoutControls();
        BufferedImage sheet = assetM.getVolumeSlider();
        if (sheet != null && sheet.getWidth() >= 299 && sheet.getHeight() >= 44) {
            // The PNG is a sheet: three handles, then a separate track. Cache slices once.
            sliderTrack = sheet.getSubimage(84, 12, 215, 24);
            for (int i = 0; i < sliderHandles.length; i++) {
                sliderHandles[i] = sheet.getSubimage(4 + i * 28, 0, 20, 44);
            }
        }
    }

    private void layoutControls() {
        int bx = (gamePanel.getScreenWidth() - 258) / 2;
        int by = (gamePanel.getScreenHeight() - 389) / 2;
        for (int i = 0; i < navRects.length; i++) {
            if (navRects[i] == null) navRects[i] = new Rectangle();
            navRects[i].setBounds(bx + 22 + i * 74, by + 401, 56, 56);
        }
        audioRect.setBounds(bx + 178, by + 116, 42, 42);
        // Place the track BELOW the board's VOLUME lettering, at native pixel size.
        sliderRect.setBounds(bx + 22, by + 280, 215, 44);
    }

    private Rectangle sliderHandleBounds() {
        int centerX = sliderRect.x + SLIDER_INSET + Math.round(SLIDER_TRAVEL * assetM.getMasterVolume());
        return new Rectangle(centerX - 10, sliderRect.y, 20, 44);
    }

    @Override
    public void update() {
        layoutControls();
        // Unpause on Escape
        if (keyH.isJustPressed(java.awt.event.KeyEvent.VK_ESCAPE)) {
            assetM.playClick();
            stateManager.setPlayingState();
            return;
        }

        int mx = mouseH.mouseX;
        int my = mouseH.mouseY;

        // Audio toggle click
        if (mouseH.leftClicked && audioRect.contains(mx, my)) {
            assetM.toggleMute();
            assetM.playClick();
        }

        // Start only on this control, then keep dragging even outside the board.
        if (mouseH.leftClicked && sliderRect.contains(mx, my)) {
            isDraggingSlider = true;
        }
        if (isDraggingSlider && (mouseH.leftPressed || mouseH.leftClicked)) {
            float relX = (float) (mx - sliderRect.x - SLIDER_INSET) / SLIDER_TRAVEL;
            assetM.setMasterVolume(Math.max(0.0f, Math.min(1.0f, relX)));
        }
        if (!mouseH.leftPressed) {
            isDraggingSlider = false;
        }

        // Navigation buttons hover and click
        hoveredNav = -1;
        for (int i = 0; i < 3; i++) {
            if (navRects[i].contains(mx, my)) {
                hoveredNav = i;
                if (mouseH.leftClicked) {
                    assetM.playClick();
                    if (i == 0) { // Resume
                        stateManager.setPlayingState();
                    } else if (i == 1) { // Advance/Reset day
                        if (stateManager.getPlayingState() instanceof PlayingState) {
                            ((PlayingState) stateManager.getPlayingState()).getClock().executeDayAdvance();
                        }
                        stateManager.setPlayingState();
                    } else if (i == 2) { // Home / Main Menu
                        stateManager.setMenuState();
                    }
                }
                break;
            }
        }
    }

    @Override
    public void render(Graphics2D graphics) {
        int w = gamePanel.getScreenWidth();
        int h = gamePanel.getScreenHeight();
        layoutControls();
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // 1. Render gameplay behind paused modal
        State ps = stateManager.getPlayingState();
        if (ps != null) {
            ps.render(g2);
        }

        // 2. Translucent dark overlay
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, w, h);

        int boardW = 258;
        int boardH = 389;
        int bx = (w - boardW) / 2;
        int by = (h - boardH) / 2;

        // 3. Draw Pause Board
        BufferedImage pb = assetM.getPauseBoard();
        if (pb != null) {
            g2.drawImage(pb, bx, by, boardW, boardH, null);
        } else {
            assetM.drawPanel(g2, bx, by, boardW, boardH);
        }

        // 4. Draw Audio Mute / Unmute Button, retaining all sprite states.
        int muteRow = assetM.isMuted() ? 1 : 0;
        int audioState = audioRect.contains(mouseH.mouseX, mouseH.mouseY) ? (mouseH.leftPressed ? 2 : 1) : 0;
        BufferedImage audioIcon = assetM.getAudioToggle(muteRow, audioState);
        if (audioIcon != null) {
            g2.drawImage(audioIcon, audioRect.x, audioRect.y, audioRect.width, audioRect.height, null);
        }

        // 5. Native track and handle artwork; input uses this exact travel range.
        if (sliderTrack != null) {
            g2.drawImage(sliderTrack, sliderRect.x, sliderRect.y + 10, null);
            Rectangle handle = sliderHandleBounds();
            int state = isDraggingSlider ? 2 : (sliderRect.contains(mouseH.mouseX, mouseH.mouseY) ? 1 : 0);
            g2.drawImage(sliderHandles[state], handle.x, handle.y, null);
        }

        // 6. Draw Navigation Buttons below board
        for (int i = 0; i < 3; i++) {
            Rectangle r = navRects[i];
            int col = (hoveredNav == i) ? (mouseH.leftPressed ? 2 : 1) : 0;
            BufferedImage navImg = assetM.getNavButton(i, col);
            if (navImg != null) {
                g2.drawImage(navImg, r.x, r.y, r.width, r.height, null);
            }
        }
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(new Color(255, 240, 210));
        String[] labels = {"Resume", "Next Day", "Menu"};
        for (int i = 0; i < labels.length; i++) {
            Rectangle r = navRects[i];
            g2.drawString(labels[i], r.x + (r.width - g2.getFontMetrics().stringWidth(labels[i])) / 2, r.y + r.height + 14);
        }
        g2.dispose();
    }
}
