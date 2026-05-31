package game.maskedbee.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;

import game.maskedbee.entities.Player;
import game.maskedbee.objects.Door;
import game.maskedbee.objects.Key;
import game.maskedbee.objects.Lever;
import game.maskedbee.objects.PushableBlock;
import game.maskedbee.objects.Spike;

public class PuzzleManager {

    public void update(Player player, MapManager mapManager) {
        handlePushables(player, mapManager);
        checkKeyPickup(player, mapManager);
        handleInteractions(player, mapManager);
        handleMaskPickup(player, mapManager);
    }

    private void handleMaskPickup(Player player, MapManager mapManager) {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) return;

        // Chỉ xử lý ở Hidden_Room
        if (!"Hidden_Room.tmx".equalsIgnoreCase(mapManager.getCurrentMapName())) {
            return;
        }

        // Đã có mặt nạ rồi thì không lấy lại nữa
        if (player.hasMask) {
            return;
        }

        for (RectangleMapObject obj : mapManager.getInteractPoints()) {
            if (!"mask_coffin".equals(obj.getName())) {
                continue;
            }

            if (player.hitbox.overlaps(obj.getRectangle())) {
                player.hasMask = true;
                System.out.println("Picked bee mask!");
                return;
            }
        }
    }
    private void handleInteractions(Player player, MapManager mapManager) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            for (Lever lever : mapManager.levers) {
                if (player.hitbox.overlaps(lever.hitbox)) {
                    lever.toggle(mapManager.getMap());
                    mapManager.rememberLeverState(lever);

                    if ("lever".equals(lever.type)) {
                        for (Spike spike : mapManager.spikes) {
                            if ((lever.targetColor != null && lever.targetColor.equals(spike.type))
                                || "black".equals(spike.type)) {
                                spike.toggle(mapManager.getMap());
                                mapManager.rememberSpikeState(spike);
                            }
                        }
                    } else if ("door_lever".equals(lever.type)) {
                        mapManager.openDoor(lever.targetName);
                    }

                    break;
                }
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            for (Door door : mapManager.getDoors()) {
                Rectangle pRect = player.hitbox;
                Rectangle doorRect = door.getBounds();

                float distanceX = Math.abs(pRect.x - doorRect.x);
                float distanceY = Math.abs(pRect.y - doorRect.y);

                if (distanceX <= 40f && distanceY <= 40f) {
                    if (door.canOpen(player.currentKey)) {
                        mapManager.openDoor(door.getName());
                    } else {
                        System.out.println("Need key: " + door.getRequiredKeyName());
                    }
                    break;
                }
            }
        }
    }

    private void checkKeyPickup(Player player, MapManager mapManager) {
        for (Key key : mapManager.getKeys()) {
            if (!key.isCollected() && player.hitbox.overlaps(key.getBounds())) {
                key.collect();

                String keyName = key.getName();
                player.currentKey = keyName;

                if (keyName != null && !keyName.isEmpty()) {
                    mapManager.markKeyCollected(keyName);
                }

                System.out.println("Picked key: " + player.currentKey);
                return;
            }
        }
    }

    private void handlePushables(Player player, MapManager mapManager) {
        float pushDistance = 32f;
        float dx = 0f;
        float dy = 0f;

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) dx = -pushDistance;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) dx = pushDistance;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) dy = pushDistance;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) dy = -pushDistance;

        if (dx == 0f && dy == 0f) return;

        for (PushableBlock block : mapManager.getPushables()) {
            Rectangle b = block.getBounds();
            Rectangle p = player.hitbox;

            boolean touching = false;
            float tolerance = 2f;

            if (dx > 0f) touching = p.x < b.x && (p.x + p.width) >= b.x - tolerance && Math.abs(p.y - b.y) < 12f;
            else if (dx < 0f) touching = p.x > b.x && p.x <= (b.x + b.width) + tolerance && Math.abs(p.y - b.y) < 12f;
            else if (dy > 0f) touching = p.y < b.y && (p.y + p.height) >= b.y - tolerance && Math.abs(p.x - b.x) < 12f;
            else if (dy < 0f) touching = p.y > b.y && p.y <= (b.y + b.height) + tolerance && Math.abs(p.x - b.x) < 12f;

            if (!touching) continue;

            Rectangle future = new Rectangle(b.x + dx, b.y + dy, b.width, b.height);
            boolean blocked = false;

            for (Rectangle wall : mapManager.getWallCollision()) {
                if (future.overlaps(wall)) {
                    blocked = true;
                    break;
                }
            }

            for (Door door : mapManager.getDoors()) {
                if (!door.isOpen() && future.overlaps(door.getBounds())) {
                    blocked = true;
                    break;
                }
            }

            for (PushableBlock other : mapManager.getPushables()) {
                if (other != block && future.overlaps(other.getBounds())) {
                    blocked = true;
                    break;
                }
            }

            if (!blocked) {
                b.x += dx;
                b.y += dy;
                mapManager.rememberPushableState(block);
            }

            break;
        }
    }

    public boolean checkSpikeDeath(Player player, MapManager mapManager) {
        for (Spike spike : mapManager.spikes) {
            if (spike.isUp && player.hitbox.overlaps(spike.hitbox)) {
                System.out.println("💀 Dap trung gai! Reset level!");
                return true;
            }
        }
        return false;
    }

    public void resetPushables(MapManager mapManager) {
        mapManager.resetPushables();
    }
}
