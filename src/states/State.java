package states;

import java.awt.Graphics2D;

/**
 * State is the abstract base class for all game states.
 */
public abstract class State {

    protected final StateManager stateManager;

    public State(StateManager stateManager) {
        this.stateManager = stateManager;
    }

    public abstract void init();
    public abstract void update();
    public abstract void render(Graphics2D g2);

    public void onEnter() {}
    public void onExit() {}
}
