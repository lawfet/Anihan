package states;

import engine.GamePanel;
import engine.GameState;

import java.awt.Graphics2D;

/** Controls the active screen and exposes its high-level game state. */
public class StateManager {
    private final GamePanel gamePanel;
    private State menuState;
    private State playingState;
    private State pauseState;
    private State currentState;
    private GameState currentGameState = GameState.MENU;

    public StateManager(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    public void init() {
        menuState = new MenuState(this, gamePanel);
        playingState = new PlayingState(this, gamePanel);
        pauseState = new PauseState(this, gamePanel);
        menuState.init();
        playingState.init();
        pauseState.init();
        setState(menuState, GameState.MENU);
    }

    public void update() {
        if (currentState != null) currentState.update();
    }

    public void render(Graphics2D graphics) {
        if (currentState != null) currentState.render(graphics);
    }

    private void setState(State next, GameState gameState) {
        if (currentState != null) currentState.onExit();
        currentState = next;
        currentGameState = gameState;
        if (currentState != null) currentState.onEnter();
    }

    public void setMenuState() { setState(menuState, GameState.MENU); }
    public void setPlayingState() { setState(playingState, GameState.PLAYING); }
    public void setPauseState() { setState(pauseState, GameState.PAUSED); }

    public GameState getCurrentGameState() { return currentGameState; }
    public GamePanel getGamePanel() { return gamePanel; }
    public State getPlayingState() { return playingState; }
}
