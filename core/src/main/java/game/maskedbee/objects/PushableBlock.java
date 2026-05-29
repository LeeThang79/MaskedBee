package game.maskedbee.objects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class PushableBlock {

    private final String id;
    private final Rectangle bounds;
    private final Rectangle startBounds;
    // Đổi path này nếu sprite cocoon của bạn nằm ở file khác.
    private static final Texture texture = new Texture("map/block.png");

    public PushableBlock(Rectangle rect) {
        this(rect, "pushable_" + Math.round(rect.x) + "_" + Math.round(rect.y));
    }

    public PushableBlock(Rectangle rect, String id) {
        this.id = id;
        this.bounds = new Rectangle(rect);
        this.startBounds = new Rectangle(rect);
    }
    public String getId() {
        return id;
    }
    public void resetPosition() {
        setPosition(startBounds.x, startBounds.y);
    }

    public void setPosition(float x, float y) {
        bounds.x = x;
        bounds.y = y;
    }
    public Rectangle getBounds() {
        return bounds;
    }

    public void render(SpriteBatch batch) {
        batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public void move(float dx, float dy,
                     Array<Rectangle> walls,
                     Array<PushableBlock> others) {
        bounds.x += dx;
        if (isCollidingWalls(walls) || isCollidingBlocks(others)) {
            bounds.x -= dx;
        }

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
