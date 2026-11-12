package anihan;

/**
 * Backwards-compatible launcher for older VS Code and Eclipse run configurations.
 * The class lives under the single configured src source root.
 */
public final class AnihanGame {
    private AnihanGame() {
    }

    public static void main(String[] args) {
        engine.Main.main(args);
    }
}
