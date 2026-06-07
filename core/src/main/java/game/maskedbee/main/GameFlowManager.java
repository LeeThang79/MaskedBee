package game.maskedbee.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import game.maskedbee.screens.PlayScreen;
import game.maskedbee.screens.EndingScreen;
import game.maskedbee.main.AudioManager;
import game.maskedbee.main.NotificationManager;

public class GameFlowManager {
    private final PlayScreen screen;

    // Tiến trình cốt truyện quyết định Ending
    private boolean refusedQueenEnding = false;
    private boolean rescuedPrisoner = false;
    private String pendingExitEndingType = "escape";

    // Tiến trình đếm ngược phá hủy phòng thí nghiệm
    private boolean waxCountdownStarted = false;
    private float waxCountdownTimer = 0f;
    private static final float WAX_ESCAPE_TIME = 120f;

    public GameFlowManager(PlayScreen screen) {
        this.screen = screen;
    }

    // ==========================================
    // 1. UPDATE VÀ KIỂM TRA ĐẾM NGƯỢC
    // ==========================================
    public void update(float delta) {
        if (waxCountdownStarted) {
            waxCountdownTimer -= delta;
            if (waxCountdownTimer <= 0f) {
                waxCountdownStarted = false;
                waxCountdownTimer = 0f;
                System.out.println("Wax lab exploded!");
                goToEnding("lab_explosion");
            }
        }
    }

    // ==========================================
    // 2. XỬ LÝ CÁC TƯƠNG TÁC QUAN TRỌNG VỚI CỐT TRUYỆN (Phím E)
    // ==========================================
    public void handleStoryInteractions() {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) return;

        String currentMap = screen.game.map.getCurrentMapName();

        // A. Cứu tù nhân ở Holding Chamber
        if ("Holding_Chamber.tmx".equalsIgnoreCase(currentMap)) {
            if (isNearInteractObject("prisoner", "prisoner_interact")) {
                rescuedPrisoner = true;
                NotificationManager.getInstance().show("Bạn đã nhận được bật lửa từ tù nhân.");
                return;
            }
        }

        // B. Kích hoạt tự hủy Wax Lab
        if ("Wax_Pumb.tmx".equalsIgnoreCase(currentMap) || "Wax_Pump.tmx".equalsIgnoreCase(currentMap)) {
            if (!isNearInteractObject("wax_star", "wax_control", "wax_vent", "wax_pump")) return;

            if (!rescuedPrisoner) {
                NotificationManager.getInstance().show("Bạn cần giải cứu tù nhân trước.");
                return;
            }
            if (waxCountdownStarted) {
                NotificationManager.getInstance().show("Bắt đầu đếm ngược!");
                return;
            }

            waxCountdownStarted = true;
            waxCountdownTimer = WAX_ESCAPE_TIME;
            NotificationManager.getInstance().show("Phòng thí nghiệm sẽ bị tự hủy!\nHãy trốn thoát trong hai phút");
            return;
        }

        // C. Tương tác với Nữ Hoàng (Chuyển sang UI chọn)
        if ("Queen_Chamber.tmx".equalsIgnoreCase(currentMap)) {
            if (!screen.myPlayer.hasMask) {
                NotificationManager.getInstance().show("Bạn cần có mặt nạ trước khi nói chuyện với nữ hoàng");
                return;
            }
            if (!screen.myPlayer.hasActivatedMask) {
                NotificationManager.getInstance().show("Bạn cần kích hoạt mặt nạ ở Old Chapel trước");
                return;
            }
            if (isNearInteractObject("queen_flower")) {
                screen.openQueenChoice(); // Ra lệnh cho PlayScreen mở bảng chọn
                return;
            }
        }
    }

    // Hàm tiện ích tự đi quét xem Player có đứng gần object không
    private boolean isNearInteractObject(String... names) {
        if (screen.game.map.getInteractPoints() == null) return false;

        for (RectangleMapObject obj : screen.game.map.getInteractPoints()) {
            if (obj == null || obj.getName() == null) continue;

            for (String name : names) {
                if (name.equalsIgnoreCase(obj.getName()) && screen.myPlayer.hitbox.overlaps(obj.getRectangle())) {
                    return true;
                }
            }
        }
        return false;
    }

    // ==========================================
    // 3. LOGIC CHUYỂN CẢNH & ENDING
    // ==========================================
    public boolean isFinalExitPortal(String currentMap, String nextMap) {
        if (currentMap == null || nextMap == null) return false;
        String lowerNextMap = nextMap.toLowerCase();
        if ("Exit_Chamber.tmx".equalsIgnoreCase(currentMap) && lowerNextMap.endsWith("exit.tmx")) return true;
        return "Queen_Chamber.tmx".equalsIgnoreCase(currentMap) && lowerNextMap.endsWith("exit.tmx") && !lowerNextMap.contains("exit_chamber");
    }

    public String getExitEndingType() {
        if (!screen.myPlayer.hasMask) return "no_mask";
        if (!screen.myPlayer.hasActivatedMask) return "inactive_mask";
        if (refusedQueenEnding) return "escape";
        return "escape";
    }

    public void goToEnding(String endingType) {
        AudioManager.getInstance().stopBackgroundMusic();
        screen.game.setScreen(new EndingScreen(screen.game, endingType));
    }

    public void refuseQueenChoice() {
        refusedQueenEnding = true;
        screen.resumeGame();
        screen.setPortalCooldown(0.45f);
    }

    public void resetProgress() {
        refusedQueenEnding = false;
        rescuedPrisoner = false;
        pendingExitEndingType = "escape";
        waxCountdownStarted = false;
        waxCountdownTimer = 0f;
    }

    // ==========================================
    // GETTER & SETTER CHO HUD RENDERER & PLAY SCREEN
    // ==========================================
    public boolean isWaxCountdownStarted() { return waxCountdownStarted; }
    public float getWaxCountdownTimer() { return waxCountdownTimer; }

    public String getPendingExitEndingType() { return pendingExitEndingType; }
    public void setPendingExitEndingType(String type) { this.pendingExitEndingType = type; }

    public void stopWaxCountdown() {
        this.waxCountdownStarted = false;
        this.waxCountdownTimer = 0f;
    }
}
