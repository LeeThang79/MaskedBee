package game.maskedbee.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import game.maskedbee.screens.PlayScreen;
import game.maskedbee.objects.PushableBlock;

public class InteractionPrompt {
    private final PlayScreen screen;
    private String currentPrompt = "";

    public InteractionPrompt(PlayScreen screen) {
        this.screen = screen;
    }

    public void update() {
        currentPrompt = "";

        // Tạo vùng quét tương tác mượn từ hitbox của nhân vật
        Rectangle interactRange = new Rectangle(
            screen.myPlayer.hitbox.x - 5f,
            screen.myPlayer.hitbox.y - 5f,
            screen.myPlayer.hitbox.width + 10f,
            screen.myPlayer.hitbox.height + 10f
        );

        boolean showG = false;
        boolean showF = false;
        boolean showE = false;
        boolean showSpace = false;

        // 1. Quét phím G
        if (screen.game.map.getPushables() != null) {
            for (PushableBlock block : screen.game.map.getPushables()) {
                if (interactRange.overlaps(block.getBounds())) {
                    showG = true;
                    break;
                }
            }
        }
        if (!showG && checkLayerOverlap("ExaminePoints", interactRange)) {
            showG = true;
        }

        // 2. Quét phím F
        if (checkLayerOverlap("Doors", interactRange)) {
            showF = true;
        }
        if (screen.myPlayer.isHidingAtStone) {
            showF = true;
        } else if (checkLayerOverlap("Doors", interactRange) || checkLayerOverlap("Stone_Collision", interactRange)) {
            showF = true;
        }

        // 3. Quét phím E
        if (screen.game.map.getInteractPoints() != null) {
            for (RectangleMapObject obj : screen.game.map.getInteractPoints()) {
                if (interactRange.overlaps(obj.getRectangle())) {
                    showE = true;
                    break;
                }
            }
        }
        if (!showE) {
            if (checkLayerOverlap("Switch", interactRange)
                || checkLayerOverlap("Chest_Collision", interactRange)
                || checkLayerOverlap("Keys", interactRange)) {
                showE = true;
            }
        }

        // 4. Quét SPACE
        if (checkPortalPromptOverlap(interactRange)) {
            showSpace = true;
        }

        // 5. Ghép các phím lại thành chuỗi
        ArrayList<String> prompts = new ArrayList<>();
        if (showE) prompts.add("[E]");
        if (showF) prompts.add("[F]");
        if (showG) prompts.add("[G]");
        if (showSpace) prompts.add("[SPACE]");

        if (prompts.isEmpty()) {
            currentPrompt = "";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < prompts.size(); i++) {
                sb.append(prompts.get(i));
                if (i < prompts.size() - 1) {
                    sb.append(" / ");
                }
            }
            currentPrompt = sb.toString();
        }
    }

    // Các hàm kiểm tra layer cũ được dời sang đây
    private boolean checkLayerOverlap(String layerName, Rectangle interactRange) {
        if (screen.game.map.getMap() == null) return false;
        MapLayer layer = screen.game.map.getMap().getLayers().get(layerName);
        if (layer == null) return false;

        for (MapObject obj : layer.getObjects()) {
            if (obj instanceof RectangleMapObject) {
                if (interactRange.overlaps(((RectangleMapObject) obj).getRectangle())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkPortalPromptOverlap(Rectangle interactRange) {
        if (screen.game.map.getMap() == null) return false;
        for (MapLayer layer : screen.game.map.getMap().getLayers()) {
            if (layer == null) continue;
            String layerName = layer.getName();

            boolean portalLayer = layerName.equals("Exit")
                || layerName.contains("_Chamber")
                || layerName.contains("Corridor")
                || layerName.equals("SpawnPoints");

            if (!portalLayer) continue;

            for (MapObject obj : layer.getObjects()) {
                if (obj instanceof RectangleMapObject) {
                    if (interactRange.overlaps(((RectangleMapObject) obj).getRectangle())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Hàm vẽ giao diện
    public void draw(BitmapFont hintFont) {
        if (!currentPrompt.isEmpty()) {
            screen.game.batch.setProjectionMatrix(screen.getCamera().combined);
            screen.game.batch.begin();
            // Lấy trực tiếp tọa độ của player để chữ bay theo người
            hintFont.draw(screen.game.batch, currentPrompt, screen.myPlayer.x - 10, screen.myPlayer.y + 40);
            screen.game.batch.end();
        }
    }

    // Cung cấp hàm clear để PlayScreen có thể ẩn chữ đi (ví dụ khi có hội thoại, hoặc bị quái bắt)
    public void clear() {
        currentPrompt = "";
    }
}
