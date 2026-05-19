package game.maskedbee.objects;

import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;

public class Door {

    private final Rectangle bounds;
    private final String name;

    private boolean isOpen;

    // Optional: cần key mới mở
    private boolean requiresKey;
    private String requiredKeyName;

    public Door(RectangleMapObject obj) {
        this.bounds = obj.getRectangle();
        this.name = obj.getName();

        this.isOpen = false;

        // Đọc custom properties từ Tiled (nếu có)
        if (obj.getProperties().containsKey("requiresKey")) {
            this.requiresKey = Boolean.parseBoolean(obj.getProperties().get("requiresKey").toString());
        }

        if (obj.getProperties().containsKey("keyName")) {
            this.requiredKeyName = obj.getProperties().get("keyName").toString();
        }
    }

    // =========================
    // LOGIC
    // =========================
    public boolean canOpen(String playerKey) {
        if (!requiresKey) return true;
        return requiredKeyName != null && requiredKeyName.equals(playerKey);
    }

    public void open() {
        isOpen = true;
        System.out.println("🚪 Door opened: " + name);
    }

    public void close() {
        isOpen = false;
    }

    // =========================
    // COLLISION
    // =========================
    public boolean isBlocking(Rectangle entity) {
        return !isOpen && entity.overlaps(bounds);
    }

    // =========================
    // GETTER
    // =========================
    public Rectangle getBounds() {
        return bounds;
    }

    public String getName() {
        return name;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean requiresKey() {
        return requiresKey;
    }

    public String getRequiredKeyName() {
        return requiredKeyName;
    }
}
