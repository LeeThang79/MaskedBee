package game.maskedbee.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public abstract class Entity {
    public float x, y;
    public float speed;
    public Rectangle hitbox;

    protected Rectangle futureHitbox = new Rectangle();
    protected float stateTime = 0f;

    public Entity(float x, float y, float width, float height, float speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.hitbox = new Rectangle(x, y, width, height);
    }

    public float getCenterX() {
        return hitbox.x + hitbox.width / 2f;
    }

    public float getCenterY() {
        return hitbox.y + hitbox.height / 2f;
    }

    protected boolean collides(Rectangle rect, Array<Rectangle> walls) {
        if (walls == null) return false;

        for (Rectangle wall : walls) {
            if (rect.overlaps(wall)) {
                return true;
            }
        }
        return false;
    }

    // Trả về true nếu Entity thật sự di chuyển được
    public boolean moveWithCollision(float stepX, float stepY, Array<Rectangle> walls) {
        float oldX = x;
        float oldY = y;

        // Di chuyển trục X
        if (stepX != 0) {
            futureHitbox.set(hitbox);
            futureHitbox.x += stepX;

            if (!collides(futureHitbox, walls)) {
                x += stepX;
                hitbox.x = x;
            }
        }

        // Di chuyển trục Y
        if (stepY != 0) {
            futureHitbox.set(hitbox);
            futureHitbox.y += stepY;

            if (!collides(futureHitbox, walls)) {
                y += stepY;
                hitbox.y = y;
            }
        }

        hitbox.setPosition(x, y);

        return Math.abs(x - oldX) > 0.001f || Math.abs(y - oldY) > 0.001f;
    }

    public boolean wouldCollide(float stepX, float stepY, Array<Rectangle> walls) {
        futureHitbox.set(hitbox);
        futureHitbox.x += stepX;
        futureHitbox.y += stepY;
        return collides(futureHitbox, walls);
    }

    public abstract void draw(SpriteBatch batch);
}
