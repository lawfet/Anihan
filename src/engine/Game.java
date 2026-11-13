package engine;

import javax.swing.SwingUtilities;

/** Application entry point for ANIHAN. */
public final class Game {
    private Game() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameWindow().launch());
    }
}
