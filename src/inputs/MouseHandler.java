package inputs;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * MouseHandler handles mouse movement, clicks, dragging, and wheel scrolling.
 */
public class MouseHandler implements MouseListener, MouseMotionListener, MouseWheelListener {

    public int mouseX, mouseY;
    public boolean leftPressed;
    public boolean rightPressed;
    public boolean leftClicked;
    public boolean rightClicked;
    public boolean isDragging;
    public int wheelRotation;

    public void update() {
        leftClicked = false;
        rightClicked = false;
        wheelRotation = 0;
    }

    public void reset() {
        update();
        leftPressed = false;
        rightPressed = false;
        isDragging = false;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        isDragging = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        isDragging = true;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftPressed = true;
            leftClicked = true;
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            rightPressed = true;
            rightClicked = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftPressed = false;
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            rightPressed = false;
        }
        isDragging = false;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        wheelRotation += e.getWheelRotation();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Handled in mousePressed for instant, responsive game input
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Can optionally flag mouse outside
    }
}
