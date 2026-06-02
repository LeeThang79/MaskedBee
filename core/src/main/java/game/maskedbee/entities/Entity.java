package game.maskedbee.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
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

        futureHitbox.set(hitbox.x + stepX, hitbox.y, hitbox.width, hitbox.height);
        boolean canMoveX = true;

        if (walls != null) {
            for (Rectangle wall : walls) {
                if (futureHitbox.overlaps(wall)) {
                    canMoveX = false;
                    break;
                }
            }
        }

        if (canMoveX) {
            x += stepX;
            hitbox.x = x;
        }

        futureHitbox.set(hitbox.x, hitbox.y + stepY, hitbox.width, hitbox.height);
        boolean canMoveY = true;

        if (walls != null) {
            for (Rectangle wall : walls) {
                if (futureHitbox.overlaps(wall)) {
                    canMoveY = false;
                    break;
                }
            }
        }

        if (canMoveY) {
            y += stepY;
            hitbox.y = y;
        }
        x = MathUtils.clamp(x, 0, Gdx.graphics.getWidth() - hitbox.width);
        y = MathUtils.clamp(y, 0, Gdx.graphics.getHeight() - hitbox.height);

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
