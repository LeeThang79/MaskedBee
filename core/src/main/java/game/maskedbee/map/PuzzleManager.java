package game.maskedbee.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import game.maskedbee.entities.Player;
import game.maskedbee.objects.Door;
import game.maskedbee.objects.Key;
import game.maskedbee.objects.Lever;
import game.maskedbee.objects.PushableBlock;
import game.maskedbee.objects.Spike;

public class PuzzleManager {

    // Hàm Update chính, sẽ được PlayScreen gọi mỗi frame
    public void update(Player player, MapManager mapManager) {
        handlePushables(player, mapManager);
        checkKeyPickup(player, mapManager);
        handleInteractions(player, mapManager);
    }

    // ==========================================
    // 1. LOGIC TƯƠNG TÁC (CẦN GẠT & CỬA)
    // ==========================================
    private void handleInteractions(Player player, MapManager mapManager) {
        // Nhấn E: Gạt cần
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            for (Lever lever : mapManager.levers) {
                if (player.hitbox.overlaps(lever.hitbox)) {
                    lever.toggle(mapManager.getMap());

                    if ("lever".equals(lever.type)) {
                        for (Spike spike : mapManager.spikes) {
                            if (lever.targetColor != null && lever.targetColor.equals(spike.type) || "black".equals(spike.type)) {
                                spike.toggle(mapManager.getMap());
                            }
                        }
                    } else if ("door_lever".equals(lever.type)) {
                        mapManager.openDoor(lever.targetName);
                    }
                    break;
                }
            }
        }

        // Nhấn F: Mở cửa bằng chìa
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            for (Door door : mapManager.getDoors()) {
                Rectangle pRect = player.hitbox;
                Rectangle doorRect = door.getBounds();
                float distanceX = Math.abs(pRect.x - doorRect.x);
                float distanceY = Math.abs(pRect.y - doorRect.y);

                if (distanceX <= 40 && distanceY <= 40) {
                    if (door.canOpen(player.currentKey)) {
                        mapManager.openDoor(door.getName());
                    } else {
                        System.out.println("❌ Need key: " + door.getRequiredKeyName());
                    }
                    break;
                }
            }
        }
    }

    // ==========================================
    // 2. LOGIC NHẶT CHÌA KHÓA
    // ==========================================
    private void checkKeyPickup(Player player, MapManager mapManager) {
        for (Key key : mapManager.getKeys()) {
            if (!key.isCollected() && player.hitbox.overlaps(key.getBounds())) {
                key.collect();
                player.currentKey = key.getName();
                System.out.println("🔑 Picked key: " + player.currentKey);
            }
        }
    }

    // ==========================================
    // 3. LOGIC ĐẨY ĐÁ
    // ==========================================
    private void handlePushables(Player player, MapManager mapManager) {
        float pushDistance = 32f;
        float dx = 0, dy = 0;

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) dx = -pushDistance;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) dx = pushDistance;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) dy = pushDistance;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) dy = -pushDistance;

        if (dx == 0 && dy == 0) return;

        for (PushableBlock block : mapManager.getPushables()) {
            Rectangle b = block.getBounds();
            Rectangle p = player.hitbox;
            boolean touching = false;
            float tolerance = 2f;

            if (dx > 0) touching = p.x < b.x && (p.x + p.width) >= b.x - tolerance && Math.abs(p.y - b.y) < 12;
            else if (dx < 0) touching = p.x > b.x && p.x <= (b.x + b.width) + tolerance && Math.abs(p.y - b.y) < 12;
            else if (dy > 0) touching = p.y < b.y && (p.y + p.height) >= b.y - tolerance && Math.abs(p.x - b.x) < 12;
            else if (dy < 0) touching = p.y > b.y && p.y <= (b.y + b.height) + tolerance && Math.abs(p.x - b.x) < 12;

            if (!touching) continue;

            Rectangle future = new Rectangle(b.x + dx, b.y + dy, b.width, b.height);
            boolean blocked = false;

            for (Rectangle wall : mapManager.getWallCollision()) {
                if (future.overlaps(wall)) { blocked = true; break; }
            }
            for (PushableBlock other : mapManager.getPushables()) {
                if (other != block && future.overlaps(other.getBounds())) { blocked = true; break; }
            }

            if (!blocked) {
                b.x += dx;
                b.y += dy;
            }
            break;
        }
    }

    // ==========================================
    // 4. CÁC HÀM TIỆN ÍCH TRẢ VỀ KẾT QUẢ CHO PLAYSCREEN
    // ==========================================

    // PlayScreen sẽ gọi hàm này để biết lúc nào cần tải lại map
    public boolean checkSpikeDeath(Player player, MapManager mapManager) {
        for (Spike spike : mapManager.spikes) {
            if (spike.isUp && player.hitbox.overlaps(spike.hitbox)) {
                System.out.println("💀 Dap trung gai! Reset level!");
                return true;
            }
        }
        return false;
    }

    // PlayScreen gọi hàm này khi người chơi nhấn R
    public void resetPushables(MapManager mapManager) {
        for (PushableBlock block : mapManager.getPushables()) {
            block.resetPosition();
        }
    }
}
