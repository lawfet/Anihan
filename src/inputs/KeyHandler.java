package inputs;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;

public class KeyHandler implements KeyListener {
    public boolean up, down, left, right, shift, space, enter, escape, inventory, interact, debug;
    public int selectedSlot;
    private final boolean[] keysPressed = new boolean[256];
    private final boolean[] keysJustPressed = new boolean[256];

    public void update() {
        Arrays.fill(keysJustPressed, false);
    }

    public void reset() {
        Arrays.fill(keysPressed, false);
        update();
        refreshHeldKeys();
    }

    public boolean isJustPressed(int code) {
        return code >= 0 && code < keysJustPressed.length && keysJustPressed[code];
    }

    @Override
    public void keyTyped(KeyEvent event) {}

    @Override
    public void keyPressed(KeyEvent event) {
        int code = event.getKeyCode();
        if (code < 0 || code >= keysPressed.length) return;
        if (!keysPressed[code]) {
            keysJustPressed[code] = true;
            if (code == KeyEvent.VK_F3) debug = !debug;
        }
        keysPressed[code] = true;
        refreshHeldKeys();
        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) selectedSlot = code - KeyEvent.VK_1;
    }

    @Override
    public void keyReleased(KeyEvent event) {
        int code = event.getKeyCode();
        if (code < 0 || code >= keysPressed.length) return;
        keysPressed[code] = false;
        refreshHeldKeys();
    }

    private void refreshHeldKeys() {
        up = keysPressed[KeyEvent.VK_W] || keysPressed[KeyEvent.VK_UP];
        down = keysPressed[KeyEvent.VK_S] || keysPressed[KeyEvent.VK_DOWN];
        left = keysPressed[KeyEvent.VK_A] || keysPressed[KeyEvent.VK_LEFT];
        right = keysPressed[KeyEvent.VK_D] || keysPressed[KeyEvent.VK_RIGHT];
        shift = keysPressed[KeyEvent.VK_SHIFT];
        space = keysPressed[KeyEvent.VK_SPACE];
        enter = keysPressed[KeyEvent.VK_ENTER];
        escape = keysPressed[KeyEvent.VK_ESCAPE];
        inventory = keysPressed[KeyEvent.VK_E] || keysPressed[KeyEvent.VK_I];
        interact = keysPressed[KeyEvent.VK_F];
    }
}
