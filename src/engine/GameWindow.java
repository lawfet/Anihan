package engine;

import managers.AssetManager;

import javax.swing.JFrame;
import java.awt.image.BufferedImage;

/** Owns the desktop window that displays the Anihan game. */
public final class GameWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    private final GamePanel gamePanel;

    public GameWindow() {
        super("ANIHAN - Farming Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        try {
            BufferedImage icon = AssetManager.getInstance().getBaseSingle();
            if (icon != null) setIconImage(icon);
        } catch (RuntimeException ignored) {
            // A missing optional icon must not prevent the game from opening.
        }

        pack();
        setLocationRelativeTo(null);
    }

    public void launch() {
        setVisible(true);
        gamePanel.startGameThread();
    }
}
