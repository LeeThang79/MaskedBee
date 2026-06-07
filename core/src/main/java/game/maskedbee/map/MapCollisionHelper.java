package game.maskedbee.map;

import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import game.maskedbee.objects.PushableBlock;

public final class MapCollisionHelper {

    // Không cho khởi tạo — đây là utility class thuần túy.
    private MapCollisionHelper() {}

    // =========================================================
    // KIỂM TRA VA CHẠM
    // =========================================================

    public static boolean isColliding(
        Rectangle entityRect,
        Array<Rectangle> wallCollision,
        Array<RectangleMapObject> doorObjects,
        Array<PushableBlock> pushables
    ) {
        if (entityRect == null) return false;

        for (Rectangle wall : wallCollision) {
            if (entityRect.overlaps(wall)) return true;
        }

        for (RectangleMapObject door : doorObjects) {
            if (entityRect.overlaps(door.getRectangle())) return true;
        }

        for (PushableBlock block : pushables) {
            if (entityRect.overlaps(block.getBounds())) return true;
        }

        return false;
    }

    public static Array<Rectangle> getFullCollision(
        Array<Rectangle> wallCollision,
        Array<RectangleMapObject> doorObjects,
        Array<PushableBlock> pushables
    ) {
        Array<Rectangle> all = new Array<>();
        all.addAll(wallCollision);

        for (RectangleMapObject door : doorObjects) {
            all.add(door.getRectangle());
        }

        for (PushableBlock block : pushables) {
            all.add(block.getBounds());
        }

        return all;
    }

    public static PushableBlock getCollidingPushable(
        Rectangle rect,
        Array<PushableBlock> pushables
    ) {
        if (rect == null) return null;

        for (PushableBlock block : pushables) {
            if (rect.overlaps(block.getBounds())) {
                return block;
            }
        }

        return null;
    }

    // =========================================================
    // XÓA COLLISION
    // =========================================================

    public static int removeDoorByName(
        Array<RectangleMapObject> doorObjects,
        String target
    ) {
        if (target == null || target.isEmpty()) return 0;

        String lower = target.toLowerCase();
        int removed = 0;

        for (int i = doorObjects.size - 1; i >= 0; i--) {
            String objName = doorObjects.get(i).getName();
            if (objName == null) continue;

            String current = objName.toLowerCase();
            if (current.contains(lower) || lower.contains(current)) {
                doorObjects.removeIndex(i);
                removed++;
            }
        }

        return removed;
    }

    public static void removeWallByRect(
        Array<Rectangle> wallCollision,
        Rectangle rectToRemove
    ) {
        if (rectToRemove == null) return;

        for (int i = wallCollision.size - 1; i >= 0; i--) {
            Rectangle wall = wallCollision.get(i);

            if (wall.overlaps(rectToRemove)
                || rectToRemove.overlaps(wall)
                || sameRect(wall, rectToRemove)) {
                wallCollision.removeIndex(i);
            }
        }
    }

    // =========================================================
    // PURE GEOMETRY UTILITIES
    // =========================================================

    public static boolean sameRect(Rectangle a, Rectangle b) {
        if (a == null || b == null) return false;

        return Math.abs(a.x - b.x) < 0.01f
            && Math.abs(a.y - b.y) < 0.01f
            && Math.abs(a.width - b.width) < 0.01f
            && Math.abs(a.height - b.height) < 0.01f;
    }
}
