package states;

import engine.GamePanel;

import java.awt.Graphics2D;

/**
 * StateManager controls active game states and transitions.
 */
public class StateManager {

    private final GamePanel gamePanel;

    private State menuState;
    private State playingState;
    private State pauseState;

    private State currentState;

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

        // Start in title menu
        setState(menuState);
    }

    public void update() {
        if (currentState != null) {
            currentState.update();
        }
    }

    public void render(Graphics2D g2) {
        if (currentState != null) {
            currentState.render(g2);
        }
    }

    public void setState(State state) {
        if (currentState != null) {
            currentState.onExit();
        }
        currentState = state;
        if (currentState != null) {
            currentState.onEnter();
        }
    }

    public void setMenuState() {
        setState(menuState);
    }

    public void setPlayingState() {
        setState(playingState);
    }

    public void setPauseState() {
        setState(pauseState);
    }

    public GamePanel getGamePanel() { return gamePanel; }
    public State getPlayingState() { return playingState; }
}
