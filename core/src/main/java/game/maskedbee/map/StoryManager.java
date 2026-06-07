package game.maskedbee.map;

import com.badlogic.gdx.*;
import java.util.HashSet;
import java.util.Set;

public class StoryManager {
    private Set<String> seenIntros = new HashSet<>();

    // TỰ ĐỘNG QUÉT BẢN ĐỒ VÀ KÍCH HOẠT THOẠI
    public void handleExamine(com.badlogic.gdx.math.Rectangle playerHitbox, com.badlogic.gdx.maps.tiled.TiledMap map, DialogueManager dialogueManager, com.badlogic.gdx.utils.Array<game.maskedbee.objects.PushableBlock> pushables, String mapName) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {

            // TẠO VÙNG TƯƠNG TÁC ÁO (Mở rộng thêm 10 pixel xung quanh Player)
            com.badlogic.gdx.math.Rectangle interactRange = new com.badlogic.gdx.math.Rectangle(
                playerHitbox.x - 5, playerHitbox.y - 5,
                playerHitbox.width + 10, playerHitbox.height + 10
            );

            // ƯU TIÊN QUÉT PUSHABLE BLOCKS TRƯỚC
            if (pushables != null) {
                for (game.maskedbee.objects.PushableBlock block : pushables) {
                    // Nếu Player đứng cạnh một khối Pushable
                    if (interactRange.overlaps(block.getBounds())) {

                        // Kiểm tra xem ta đang ở Map nào
                        if (mapName.contains("Disposal")) {
                            // Gọi thẳng sự kiện thoại của Kén Nhỏ
                            checkExamineEvent("disposal_small_cocoon", dialogueManager);
                            return; // Thoát luôn, không quét đồ vật khác nữa
                        }
                    }
                }
            }

            // NẾU KHÔNG CÓ VẬT ĐỘNG NÀO, TIẾP TỤC QUÉT VẬT TĨNH TRÊN TILED
            com.badlogic.gdx.maps.MapLayer examineLayer = map.getLayers().get("ExaminePoints");
            if (examineLayer != null) {
                for (com.badlogic.gdx.maps.MapObject obj : examineLayer.getObjects()) {
                    if (obj instanceof com.badlogic.gdx.maps.objects.RectangleMapObject) {
                        com.badlogic.gdx.math.Rectangle rect = ((com.badlogic.gdx.maps.objects.RectangleMapObject) obj).getRectangle();

                        // Dùng interactRange ảo thay vì playerHitbox để dễ bấm phím hơn
                        if (interactRange.overlaps(rect)) {
                            String objName = obj.getName();
                            if (objName != null && !objName.isEmpty()) {
                                boolean foundExamine = checkExamineEvent(objName, dialogueManager);
                                if (!foundExamine) {
                                    checkExamineEvent(objName, dialogueManager);
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public void checkNewGameIntro(DialogueManager dialogueManager) {
        if (!seenIntros.contains("new_game")) {
            dialogueManager.startDialogue(DialogueLibrary.getIntroText("new_game"));
            seenIntros.add("new_game");
        }
    }

    // Hàm này giờ không còn cái if-else nào nữa! Nó tự động dò trong thư viện.
    public boolean checkExamineEvent(String objectName, DialogueManager dialogueManager) {
        String[] text = DialogueLibrary.getExamineText(objectName);
        if (text != null) {
            dialogueManager.startDialogue(text);
            return true;
        }
        return false; // Không có trong thư viện -> không có thoại
}

    public void checkMapEnterEvent(String mapName, DialogueManager dialogueManager) {
        // Chỉ quét xem mapName có chứa từ khóa (như Holding, Disposal) không
        for (String key : new String[]{"Holding", "Disposal", "Ritual"}) {
            if (mapName.contains(key) && !seenIntros.contains(key)) {
                dialogueManager.startDialogue(DialogueLibrary.getIntroText(key));
                seenIntros.add(key);
                break;
            }
        }
    }
}
