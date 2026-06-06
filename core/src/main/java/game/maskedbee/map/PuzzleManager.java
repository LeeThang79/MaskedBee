package game.maskedbee.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;

import game.maskedbee.entities.Player;
import game.maskedbee.main.NotificationManager;
import game.maskedbee.objects.Door;
import game.maskedbee.objects.Key;
import game.maskedbee.objects.Lever;
import game.maskedbee.objects.PushableBlock;
import game.maskedbee.objects.Spike;

public class PuzzleManager {

    private boolean waitingToHideLibraryKey = false;
    private float libraryKeyHideTimer = 0f;
    private float pushCooldown = 0f;
    private static final float PUSH_INTERVAL = 0.15f;

    public void update(Player player, MapManager mapManager) {
        pushCooldown -= Gdx.graphics.getDeltaTime();
        updateDelayedLibraryKeyVisual(mapManager);
        handlePushables(player, mapManager);
        checkKeyPickup(player, mapManager);
        handleInteractions(player, mapManager);
        handleMaskPickup(player, mapManager);
        handleChapelMaskActivation(player, mapManager);
    }
    private void updateDelayedLibraryKeyVisual(MapManager mapManager) {
        if (!waitingToHideLibraryKey) return;

        // Nếu người chơi rời Library trước khi hết 3 giây,
        // lần sau quay lại map sẽ tự hiện Chest_Open_No_Key nhờ isKeyCollected().
        if (!"Library.tmx".equalsIgnoreCase(mapManager.getCurrentMapName())) {
            waitingToHideLibraryKey = false;
            return;
        }

        libraryKeyHideTimer -= Gdx.graphics.getDeltaTime();

        if (libraryKeyHideTimer <= 0f) {
            waitingToHideLibraryKey = false;
            mapManager.showLibraryChestNoKey();
            System.out.println("Library chest switched to no-key.");
        }
    }

    private void handleChapelMaskActivation(Player player, MapManager mapManager) {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) return;

        if (!"Old_Chapel.tmx".equalsIgnoreCase(mapManager.getCurrentMapName())) {
            return;
        }

        for (RectangleMapObject obj : mapManager.getInteractPoints()) {
            if (!"chapel_altar".equals(obj.getName())) {
                continue;
            }

            if (!player.hitbox.overlaps(obj.getRectangle())) {
                continue;
            }

            if (!player.hasMask) {
                NotificationManager.getInstance().show("Bạn cần có mặt nạ để tương tác với điểm này");
                System.out.println("Ban can co mat na truoc khi tuong tac voi diem nay.");
                return;
            }

            if (player.hasActivatedMask) {
                NotificationManager.getInstance().show("Mặt nạ đã được kích hoạt rồi");
                System.out.println("Mat na da duoc kich hoat roi.");
                return;
            }

            player.hasActivatedMask = true;
            System.out.println("Mask activated at Old Chapel!");
            return;
        }
    }

    private void handleMaskPickup(Player player, MapManager mapManager) {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) return;

        // Chỉ xử lý ở Hidden_Room
        if (!"Hidden_Room.tmx".equalsIgnoreCase(mapManager.getCurrentMapName())) {
            return;
        }

        // Đã có mặt nạ rồi thì không lấy lại nữa
        if (player.hasMask) {
            NotificationManager.getInstance().show("Bạn đã nhặt mặt nạ rồi, không thể nhặt thêm");
            return;
        }

        for (RectangleMapObject obj : mapManager.getInteractPoints()) {
            if (!"mask_coffin".equals(obj.getName())) {
                continue;
            }

            if (player.hitbox.overlaps(obj.getRectangle())) {
                player.hasMask = true;
                player.hasMaskItem = true;
                mapManager.saveProgressCheckpointHere();
                NotificationManager.getInstance().show("Bạn vừa nhặt được mặt nạ của nữ hoàng\n     Bấm 'P' để đeo");
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
                        NotificationManager.getInstance().show("Cửa tù đã được mở");
                        NotificationManager.getInstance().show("Checkpoint mới được lưu thành Holding Chamber");
                        mapManager.saveProgressCheckpointHere();
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
                        NotificationManager.getInstance().show("Cần chìa khóa để mở cửa này");
                        System.out.println("Need key: " + door.getRequiredKeyName());
                    }
                    break;
                }
            }
        }
    }

    private void checkKeyPickup(Player player, MapManager mapManager) {
        // Bắt buộc bấm E mới nhặt key
        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) return;

        for (Key key : mapManager.getKeys()) {
            if (key.isCollected()) continue;

            if (!player.hitbox.overlaps(key.getBounds())) {
                continue;
            }

            String keyName = key.getName();

            key.collect();

            player.currentKey = keyName;
            player.hasKeyItem = true;

            if (keyName != null && !keyName.isEmpty()) {
                mapManager.markKeyCollected(keyName);
            }

            // Riêng gold_key ở Library:
            // vừa lấy xong thì vẫn để hình Chest_Open_Key trong 3 giây,
            // sau đó mới đổi sang Chest_Open_No_Key.
            if ("Library.tmx".equalsIgnoreCase(mapManager.getCurrentMapName())
                && "gold_key".equalsIgnoreCase(keyName)) {

                mapManager.showLibraryChestWithKey();

                waitingToHideLibraryKey = true;
                libraryKeyHideTimer = 0.5f;
                NotificationManager.getInstance().show("Bạn đã nhặt được chìa khóa");
                System.out.println("Picked gold_key. Chest will change after 3 seconds.");
            } else {
                System.out.println("Picked key: " + player.currentKey);
            }

            return;
        }
    }

    private void handlePushables(Player player, MapManager mapManager) {
        if (pushCooldown > 0f) return;
        float pushDistance = 32f;
        float dx = 0f;
        float dy = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) dx = -pushDistance;
        else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) dx = pushDistance;
        else if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) dy = pushDistance;
        else if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) dy = -pushDistance;

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
                pushCooldown = PUSH_INTERVAL;
            }

            break;
        }
    }

    public boolean checkSpikeDeath(Player player, MapManager mapManager) {
        for (Spike spike : mapManager.spikes) {
            if (spike.isUp && player.hitbox.overlaps(spike.hitbox)) {
                System.out.println("Dap trung gai! Reset level!");
                NotificationManager.getInstance().show("Bạn đã đạp trúng gai!");
                return true;
            }
        }
        return false;
    }

    public void resetPushables(MapManager mapManager) {
        mapManager.resetPushables();
    }
}
