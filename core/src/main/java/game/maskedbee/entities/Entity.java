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
    // LOGIC VẬT LÝ VÀ CHỐNG KẸT TƯỜNG (Dùng chung cho cả Player và Guard)
    public void moveWithCollision(float stepX, float stepY, Array<Rectangle> walls) {
        // Kiểm tra trục X
        futureHitbox.set(hitbox.x + stepX, hitbox.y, hitbox.width, hitbox.height);
        boolean canMoveX = true;
        for (Rectangle wall : walls) {
            if (futureHitbox.overlaps(wall)) {
                canMoveX = false;
                break;
            }
        }
        if (canMoveX) {
            x += stepX;
            hitbox.x = x;
        }

        // Kiểm tra trục Y
        futureHitbox.set(hitbox.x, hitbox.y + stepY, hitbox.width, hitbox.height);
        boolean canMoveY = true;
        for (Rectangle wall : walls) {
            if (futureHitbox.overlaps(wall)) {
                canMoveY = false;
                break;
            }
        }
        if (canMoveY) {
            y += stepY;
            hitbox.y = y;
        }
        // Kẹp tọa độ không cho ra khỏi viền màn hình
        x = MathUtils.clamp(x, 0, Gdx.graphics.getWidth() - hitbox.width);
        y = MathUtils.clamp(y, 0, Gdx.graphics.getHeight() - hitbox.height);

        hitbox.setPosition(x, y);
    }
    public abstract void draw(SpriteBatch batch);
}
