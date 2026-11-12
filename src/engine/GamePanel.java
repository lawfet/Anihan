package engine;

import inputs.KeyHandler;
import inputs.MouseHandler;
import managers.AssetManager;
import states.StateManager;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;

public final class GamePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    public static final int SCREEN_WIDTH = 1024;
    public static final int SCREEN_HEIGHT = 640;
    public static final int TARGET_FPS = 60;
    private transient Timer gameTimer;
    private final transient KeyHandler keyHandler;
    private final transient MouseHandler mouseHandler;
    private final transient AssetManager assetManager;
    private final transient StateManager stateManager;

    public GamePanel() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        keyHandler = new KeyHandler();
        mouseHandler = new MouseHandler();
        addKeyListener(keyHandler);
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
        setFocusable(true);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                keyHandler.reset();
                mouseHandler.reset();
            }
        });
        try {
            BufferedImage blank = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            setCursor(Toolkit.getDefaultToolkit().createCustomCursor(blank, new Point(0, 0), "anihan-blank"));
        } catch (java.awt.HeadlessException ignored) {}
        assetManager = AssetManager.getInstance();
        stateManager = new StateManager(this);
    }

    public void startGameThread() {
        if (gameTimer != null) return;
        stateManager.init();
        // Swing events, simulation and painting share the EDT so collections cannot race.
        gameTimer = new Timer(Math.round(1000f / TARGET_FPS), event -> {
            stateManager.update();
            keyHandler.update();
            mouseHandler.update();
            repaint();
        });
        gameTimer.start();
        requestFocusInWindow();
    }

    @Override
    public void removeNotify() {
        if (gameTimer != null) gameTimer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        stateManager.render(g);
        int frame = (int) ((System.currentTimeMillis() / 130) % 5) + 1;
        BufferedImage cursor = assetManager.getUiImage(String.format("cursor_%02d", frame));
        if (cursor != null) {
            g.drawImage(cursor, mouseHandler.mouseX - 2, mouseHandler.mouseY - 2,
                    cursor.getWidth() * 2, cursor.getHeight() * 2, null);
        }
        g.dispose();
    }

    public int getScreenWidth() { return SCREEN_WIDTH; }
    public int getScreenHeight() { return SCREEN_HEIGHT; }
    public KeyHandler getKeyHandler() { return keyHandler; }
    public MouseHandler getMouseHandler() { return mouseHandler; }
    public AssetManager getAssetManager() { return assetManager; }
    public StateManager getStateManager() { return stateManager; }
}
