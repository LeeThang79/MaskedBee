package game.maskedbee.map;

import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import game.maskedbee.objects.PushableBlock;

/**
 * Stateless utility class — chỉ chứa logic tính toán collision.
 * Không giữ state, không giữ data. Tất cả data nhận qua tham số.
 * Các hàm đều static nên không cần khởi tạo instance.
 *
 * Data (wallCollision, doorObjects...) vẫn nằm trong MapManager.
 */
public final class MapCollisionHelper {

    // Không cho khởi tạo — đây là utility class thuần túy
    private MapCollisionHelper() {}

    // =========================================================
    // Kiểm tra va chạm
    // =========================================================

    /**
     * Kiểm tra entityRect có đụng tường / cửa / pushable không.
     */
    public static boolean isColliding(
        Rectangle entityRect,
        Array<Rectangle> wallCollision,
        Array<RectangleMapObject> doorObjects,
        Array<PushableBlock> pushables
    ) {
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

    /**
     * Gộp tất cả hitbox thành một Array duy nhất.
     * Dùng cho AI / debug / Player.update().
     */
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

    /**
     * Tìm PushableBlock đang overlap với rect.
     * Trả về null nếu không có.
     */
    public static PushableBlock getCollidingPushable(
        Rectangle rect,
        Array<PushableBlock> pushables
    ) {
        for (PushableBlock block : pushables) {
            if (rect.overlaps(block.getBounds())) return block;
        }
        return null;
    }

    // =========================================================
    // Xóa collision theo điều kiện
    // =========================================================

    /**
     * Xóa tất cả door object mà tên chứa target (hoặc ngược lại).
     * Trực tiếp modify array được truyền vào.
     * Trả về số lượng đã xóa.
     */
    public static int removeDoorByName(
        Array<RectangleMapObject> doorObjects,
        String target
    ) {
        if (target == null) return 0;
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

    /**
     * Xóa các Rectangle trong wallCollision trùng / overlap với rectToRemove.
     * Trực tiếp modify array được truyền vào.
     */
    public static void removeWallByRect(
        Array<Rectangle> wallCollision,
        Rectangle rectToRemove
    ) {
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
    // Pure geometry utilities
    // =========================================================

    public static boolean sameRect(Rectangle a, Rectangle b) {
        return Math.abs(a.x - b.x) < 0.01f
            && Math.abs(a.y - b.y) < 0.01f
            && Math.abs(a.width - b.width) < 0.01f
            && Math.abs(a.height - b.height) < 0.01f;
    }
}
