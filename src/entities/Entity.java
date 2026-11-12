package entities;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Entity represents any interactive, mobile game object in the world.
 */
public abstract class Entity {

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    protected int worldX;
    protected int worldY;
    protected int speed = 3;

    protected Direction direction = Direction.DOWN;
    protected boolean facingLeft = false;

    // Collision box
    protected Rectangle solidArea = new Rectangle(0, 0, 32, 32);

    // Animation tracking
    protected int animTick = 0;
    protected int frameIndex = 0;

    public Entity(int worldX, int worldY) {
        this.worldX = worldX;
        this.worldY = worldY;
    }

    public abstract void update();
    public abstract void render(Graphics2D g2, int cameraX, int cameraY);

    public int getWorldX() { return worldX; }
    public int getWorldY() { return worldY; }
    public Direction getDirection() { return direction; }
    public Rectangle getSolidArea() { return solidArea; }
}
