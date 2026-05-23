package game.maskedbee.objects;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class PushableBlock {

    private Rectangle bounds;
    private Rectangle startBounds;

    // load ảnh (đặt đúng path)
    private static Texture texture = new Texture("map/block.png");

    public PushableBlock(Rectangle rect) {
        this.bounds = new Rectangle(rect);
        // lưu vị trí ban đầu
        this.startBounds = new Rectangle(rect);
    }
    public void resetPosition() {

        bounds.x = startBounds.x;
        bounds.y = startBounds.y;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    // 👉 render bằng ảnh
    public void render(SpriteBatch batch) {
        batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public void move(float dx, float dy,
                     Array<Rectangle> walls,
                     Array<PushableBlock> others) {

        // Move X
        bounds.x += dx;
        if (isCollidingWalls(walls) || isCollidingBlocks(others)) {
            bounds.x -= dx;
        }

        // Move Y
        bounds.y += dy;
        if (isCollidingWalls(walls) || isCollidingBlocks(others)) {
            bounds.y -= dy;
        }
    }

    private boolean isCollidingWalls(Array<Rectangle> walls) {
        for (Rectangle wall : walls) {
            if (bounds.overlaps(wall)) return true;
        }
        return false;
    }

    private boolean isCollidingBlocks(Array<PushableBlock> blocks) {
        for (PushableBlock b : blocks) {
            if (b != this && bounds.overlaps(b.getBounds())) return true;
        }
        return false;
    }
}
