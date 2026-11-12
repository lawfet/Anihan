package anihan;

/**
 * Backwards-compatible launcher.
 *
 * Older Eclipse and VS Code launch configurations may still point to
 * anihan.AnihanGame. Delegate to the current asset-based game so those
 * launch configurations no longer open the obsolete placeholder prototype.
 */
public final class AnihanGame {
    private AnihanGame() {
    }

    public static void main(String[] args) {
        engine.Main.main(args);
    }
}
