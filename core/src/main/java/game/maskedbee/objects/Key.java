package game.maskedbee.objects;

import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;

public class Key {

    private Rectangle bounds;
    private String name;
    private boolean collected = false;

    public Key(RectangleMapObject obj) {
        this.bounds = obj.getRectangle();
        this.name = obj.getName();
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public String getName() {
        return name;
    }

    public boolean isCollected() {
        return collected;
    }

    public void collect() {
        collected = true;
    }
}
