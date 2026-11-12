package engine;

import managers.AssetManager;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;

/**
 * Main application launcher for ANIHAN Farming Game.
 */
public class Main {

    public static void main(String[] args) {
        // Run on the Swing Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("ANIHAN - Farming Game");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);

            GamePanel gamePanel = new GamePanel();
            window.add(gamePanel);

            // Set window icon if available
            try {
                BufferedImage icon = AssetManager.getInstance().getBaseSingle();
                if (icon != null) {
                    window.setIconImage(icon);
                }
            } catch (Exception ignored) {}

            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);

            gamePanel.startGameThread();
        });
    }
}
