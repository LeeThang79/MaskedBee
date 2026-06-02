package game.maskedbee.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class PuzzleLibrary {
    private final Array<Rectangle> wallCollision;

    private final TiledMap map;
    private final MapManager mapManager;
    private Array<RectangleMapObject> interactPoints;

    // =====================================================
    // PUZZLE STATE
    // =====================================================

    private static final String PHASE_1_KEY = "library_phase_1";
    private static final String PHASE_2_KEY = "library_phase_2";

    private boolean started = false;
    private boolean finished = false;
    private boolean phase1Solved = false;
    private boolean phase2Solved = false;

    private int displayedPhase = 0;
    private final int[] phase1Answer = {2, 4, 3, 1};
    private final int[] phase2Answer = {1, 3, 4, 2};
    private int[] currentSequence;
    private int currentSequencePhase;

    private final Array<Integer> currentInput = new Array<>();

    // =====================================================
    // INTERACT POINTS
    // =====================================================

    // Interact_Point_0 = sách trên bàn
    private Rectangle startPoint;

    // Interact_Point_1 -> 4 = 4 kệ sách
    private final Rectangle[] buttons = new Rectangle[4];

    //SEQUENCE
    private boolean waitingNextLoop = false;

    private final float LOOP_DELAY = 1.5f;

    private boolean playingSequence = false;

    private float sequenceTimer = 0f;

    private int sequenceIndex = 0;

    private final float SHOW_DELAY = 0.7f;

    public PuzzleLibrary(MapManager mapManager) {
        this.mapManager = mapManager;
        this.map = mapManager.getMap();
        this.wallCollision = mapManager.getWallCollision();
        this.interactPoints = mapManager.getInteractPoints();

        init();
        restoreSolvedStateFromMapManager();
    }

    // Giữ constructor cũ để nếu chỗ nào trong code còn gọi kiểu cũ thì không bị lỗi compile.
    public PuzzleLibrary(TiledMap map, Array<Rectangle> wallCollision, Array<RectangleMapObject> interactPoints) {
        this.mapManager = null;
        this.map = map;
        this.wallCollision = wallCollision;
        this.interactPoints = interactPoints;

        init();
    }
    private void init() {
        loadInteractPoints();

        // Ban đầu:
        // Candle off visible
        // Candle on invisible
        setLayerVisible("Candle_Off", true);
        hideAllCandles();

        // Chest open hidden
        setLayerVisible("Chest_Open", false);
        setLayerVisible("Chest_Close", true);
    }
    private void restoreSolvedStateFromMapManager() {
        if (mapManager == null) {
            return;
        }
        boolean hiddenRoomAlreadyOpened = mapManager.isHiddenRoomOpened();

        phase1Solved = mapManager.isPuzzleStepSolved(PHASE_1_KEY) || hiddenRoomAlreadyOpened;
        phase2Solved = mapManager.isPuzzleStepSolved(PHASE_2_KEY) || hiddenRoomAlreadyOpened;

        if (phase1Solved) {
            // Đã giải phase 1 thì rương/đường lấy key phải mở sẵn.
            openChest();
            mapManager.openDoor("gold_key_chest");
        }

        if (phase2Solved) {
            // Đã giải phase 2 thì hầm phải mở sẵn và puzzle kết thúc.
            openHiddenRoom();
            turnOnAllCandles();

            started = true;
            finished = true;
            displayedPhase = 1;
            playingSequence = false;
            currentInput.clear();
            return;
        }

        if (phase1Solved) {
            // Đã giải phase 1 nhưng chưa giải phase 2.
            // Khi quay lại Library, tự động hiện sequence phase 2 luôn.
            started = true;
            finished = false;
            displayedPhase = 1;
            currentInput.clear();
            showCurrentCandles();
        }
    }

    // =====================================================
    // LOAD INTERACT POINTS
    // =====================================================

    private void loadInteractPoints() {

        for (int i = 0; i <= 4; i++) {

            MapLayer layer = map.getLayers().get("Interact_Point_" + i);

            if (layer == null) continue;

            for (MapObject obj : layer.getObjects()) {

                if (!(obj instanceof RectangleMapObject)) continue;

                Rectangle rect =
                    ((RectangleMapObject) obj).getRectangle();

                // Interact_Point_0 = sách dưới cùng
                if (i == 0) {
                    startPoint = rect;
                }
                else {
                    // 1 -> 4
                    buttons[i - 1] = rect;
                }
            }
        }
    }

    // =====================================================
    // UPDATE
    // =====================================================

    public void update(Rectangle playerHitbox, float delta) {
        if (mapManager != null && !"Library.tmx".equalsIgnoreCase(mapManager.getCurrentMapName())) {
            return;
        }

        if (finished) return;

        updateSequence(delta);

        // =================================================
        // NHẤN E VÀO SÁCH TRÊN BÀN
        // =================================================

        if (startPoint != null
            && playerHitbox.overlaps(startPoint)
            && Gdx.input.isKeyJustPressed(Input.Keys.E)) {

            startCurrentUnsolvedPhase();
            return;
        }

        if (!started) return;

        // =================================================
        // NHẤN KỆ SÁCH
        // =================================================

        for (int i = 0; i < 4; i++) {
            Rectangle button = buttons[i];

            if (button == null) continue;

            if (playerHitbox.overlaps(button)
                && Gdx.input.isKeyJustPressed(Input.Keys.E)) {

                int pressed = i + 1;

                currentInput.add(pressed);

                System.out.println("Pressed: " + pressed);

                checkInput();

                break;
            }
        }
    }
    private void startCurrentUnsolvedPhase() {
        currentInput.clear();
        started = true;

        /*
         * Chưa giải phase 1 -> cho làm phase 1.
         * Đã giải phase 1 nhưng chưa giải phase 2 -> chỉ cho làm phase 2.
         */
        if (!phase1Solved) {
            displayedPhase = 0;
        } else if (!phase2Solved) {
            displayedPhase = 1;
        } else {
            finished = true;
            turnOnAllCandles();
            return;
        }

        showCurrentCandles();

        System.out.println("Show candle sequence. Phase = " + (displayedPhase + 1));
    }

    // =====================================================
    // CHECK INPUT
    // =====================================================

    private void checkInput() {
        int[] answer = (displayedPhase == 0) ? phase1Answer : phase2Answer;

        int index = currentInput.size - 1;

        // Chống lỗi nếu nhập quá số bước
        if (index < 0 || index >= answer.length) {
            currentInput.clear();
            return;
        }

        // Sai
        if (currentInput.get(index) != answer[index]) {
            System.out.println("Wrong!");

            currentInput.clear();

            // Có thể cho xem lại sequence đang cần giải
            showCurrentCandles();

            return;
        }

        // Đúng hết
        if (currentInput.size == 4) {
            if (displayedPhase == 0) {
                solvePhase1();
            } else {
                solvePhase2();
            }
        }
    }

    private void solvePhase1() {
        phase1Solved = true;

        openChest();

        if (mapManager != null) {
            mapManager.openDoor("gold_key_chest");
            mapManager.markPuzzleStepSolved(PHASE_1_KEY);
        }

        System.out.println("Library phase 1 solved - opened gold key chest path");

        currentInput.clear();

        /*
         * Sau khi giải phase 1:
         * - Người chơi có thể lấy key.
         * - Nếu vẫn ở Library, tự chuyển sang sequence phase 2.
         * - Nếu rời map rồi quay lại, constructor sẽ đọc phase1Solved và chỉ cần làm phase 2.
         */
        if (!phase2Solved) {
            displayedPhase = 1;
            started = true;
            showCurrentCandles();
        } else {
            finishPuzzle();
        }
    }

    private void solvePhase2() {
        phase2Solved = true;

        openHiddenRoom();

        if (mapManager != null) {
            mapManager.markPuzzleStepSolved(PHASE_2_KEY);
        }

        System.out.println("Library phase 2 solved - opened hidden room");

        currentInput.clear();

        finishPuzzle();
    }

    private void finishPuzzle() {
        started = true;
        finished = true;

        playingSequence = false;
        waitingNextLoop = false;

        currentInput.clear();

        turnOnAllCandles();

        System.out.println("Library puzzle finished!");
    }

    // =====================================================
    // CANDLE
    // =====================================================

    private void showCurrentCandles() {
        hideAllCandles();

        currentSequencePhase = displayedPhase;

        if (currentSequencePhase == 0) {
            currentSequence = phase1Answer;
        } else {
            currentSequence = phase2Answer;
        }

        sequenceIndex = 0;
        sequenceTimer = 0f;

        waitingNextLoop = false;
        playingSequence = true;
    }

    private void updateSequence(float delta) {
        if (!playingSequence) return;
        if (currentSequence == null) return;

        sequenceTimer += delta;

        if (waitingNextLoop) {
            if (sequenceTimer >= LOOP_DELAY) {
                waitingNextLoop = false;

                sequenceTimer = 0f;
                sequenceIndex = 0;

                showSequenceStep();
                sequenceIndex++;
            }

            return;
        }

        if (sequenceTimer >= SHOW_DELAY) {
            sequenceTimer = 0f;

            if (sequenceIndex >= currentSequence.length) {
                hideAllCandles();
                waitingNextLoop = true;
                return;
            }

            showSequenceStep();
            sequenceIndex++;
        }
    }

    private void showSequenceStep() {
        if (currentSequence == null) return;
        if (sequenceIndex < 0 || sequenceIndex >= currentSequence.length) return;

        String layerName = (currentSequencePhase == 0)
            ? "Candle_On_1"
            : "Candle_On_2";

        MapLayer layer = map.getLayers().get(layerName);

        if (layer == null) return;

        hideAllCandles();

        int candleNumber = currentSequence[sequenceIndex];

        for (MapObject obj : layer.getObjects()) {
            if (obj.getName() == null) continue;

            if (obj.getName().equals("candle_" + candleNumber)) {
                obj.setVisible(true);

                System.out.println("Candle " + candleNumber);

                break;
            }
        }
    }

    // =====================================================
    // UTIL
    // =====================================================

    private void hideAllCandles() {
        hideLayerCandles("Candle_On_1");
        hideLayerCandles("Candle_On_2");
    }

    private void hideLayerCandles(String layerName) {
        MapLayer layer = map.getLayers().get(layerName);

        if (layer == null) return;

        for (MapObject obj : layer.getObjects()) {
            obj.setVisible(false);
        }
    }

    private void turnOnAllCandles() {
        setLayerVisible("Candle_Off", true);

        MapLayer layer1 = map.getLayers().get("Candle_On_1");
        MapLayer layer2 = map.getLayers().get("Candle_On_2");

        if (layer1 != null) {
            for (MapObject obj : layer1.getObjects()) {
                obj.setVisible(true);
            }
        }

        if (layer2 != null) {
            for (MapObject obj : layer2.getObjects()) {
                obj.setVisible(true);
            }
        }
    }

    private void openChest() {
        setLayerVisible("Chest_Close", false);
        setLayerVisible("Chest_Open", true);
    }

    // =====================================================
    // MỞ PHÒNG BÍ MẬT
    // =====================================================

    private void openHiddenRoom() {
        if (mapManager != null) {
            mapManager.openHiddenRoom();
            return;
        }

        MapLayer hiddenLayer = map.getLayers().get("Hide_Floor");
        if (hiddenLayer != null) {
            hiddenLayer.setVisible(false);
        }

        // Xóa vùng va chạm
        MapLayer collisionLayer = map.getLayers().get("Hidden_Room_Collision");
        if (collisionLayer != null) {
            for (MapObject obj : collisionLayer.getObjects()) {
                if (obj instanceof RectangleMapObject) {
                    Rectangle rectToRemove = ((RectangleMapObject) obj).getRectangle();

                    for (int i = wallCollision.size - 1; i >= 0; i--) {
                        if (wallCollision.get(i).overlaps(rectToRemove)) {
                            wallCollision.removeIndex(i);
                        }
                    }
                }
            }
        }
    }

    private void setLayerVisible(String layerName, boolean visible) {
        MapLayer layer = map.getLayers().get(layerName);

        if (layer != null) {
            layer.setVisible(visible);
        }
    }
}
