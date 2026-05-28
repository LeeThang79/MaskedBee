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

        // Nếu phòng bí mật của map này đã mở rồi thì không bắt người chơi giải lại.
        if (mapManager.isHiddenRoomOpened()) {
            started = true;
            finished = true;
            phase1Solved = true;
            phase2Solved = true;
            openChest();
            turnOnAllCandles();
            mapManager.openHiddenRoom();
        }
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

        if (finished) return;
        updateSequence(delta);

        // =================================================
        // NHẤN E VÀO SÁCH TRÊN BÀN
        // =================================================

        if (startPoint != null
            && playerHitbox.overlaps(startPoint)
            && Gdx.input.isKeyJustPressed(Input.Keys.E)) {

            currentInput.clear();

            //Lần đầu luôn là phase 1
            if(!started){
                started = true;
                displayedPhase = 0;
            }
            else {
                displayedPhase++;
                if (displayedPhase > 1 ) {
                    displayedPhase = 0;
                }
            }

            showCurrentCandles();
            System.out.println("Show candle sequence");
        }
        if(!started) return;

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

    // =====================================================
    // CHECK INPUT
    // =====================================================

    private void checkInput() {

        int[] answer = (displayedPhase == 0) ? phase1Answer : phase2Answer;

        int index = currentInput.size - 1;

        // Sai
        if (currentInput.get(index) != answer[index]) {

            System.out.println("Wrong!");

            currentInput.clear();

            return;
        }

        // Đúng hết
        if (currentInput.size == 4) {

            // =================================================
            // PHASE 1
            // =================================================

            if (displayedPhase == 0) {
                displayedPhase = 1;
                phase1Solved = true;
                currentInput.clear();
                openChest();

                //Nếu phase 2 chưa giải thì tiếp tu puzzle
                if (!phase2Solved){
                    displayedPhase = 1;
                    showCurrentCandles();
                }
                else {
                    finished = true;
                    turnOnAllCandles();
                }

                System.out.println("Chest opened!");
            }

            // =================================================
            // PHASE 2
            // =================================================

            else {
                phase2Solved = true;
                openHiddenRoom();
                currentInput.clear();

                //Nếu phase 1 chưa giải quay lại phase 1
                if (!phase1Solved){
                    displayedPhase = 0;
                    showCurrentCandles();
                }
                else {
                    finished = true;
                    turnOnAllCandles();
                }
                System.out.println("Chest opened!");
            }
        }
    }

    // =====================================================
    // CANDLE
    // =====================================================

    private void showCurrentCandles() {
        hideAllCandles();

        //sequenceIndex = 0;

        //sequenceTimer = 0f;

        //playingSequence = true;

        currentSequencePhase = displayedPhase;

        if(currentSequencePhase == 0){
            currentSequence = phase1Answer;
        }
        else {
            currentSequence = phase2Answer;
        }

        sequenceIndex = 0;

        sequenceTimer = 0f;

        waitingNextLoop = false;

        playingSequence = true;
    }

    private void updateSequence(float delta) {

        if (!playingSequence) return;

        sequenceTimer += delta;
        if (waitingNextLoop) {

            if (sequenceTimer >= LOOP_DELAY) {

                waitingNextLoop = false;

                sequenceTimer = 0f;

                sequenceIndex = 0;

                //hideAllCandles();

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

        String layerName =
            ((currentSequencePhase == 0))
                ? "Candle_On_1"
                : "Candle_On_2";

        MapLayer layer =
            map.getLayers().get(layerName);

        if (layer == null) return;

        hideAllCandles();

        int candleNumber =
            currentSequence[sequenceIndex];

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

        MapLayer layer =
            map.getLayers().get(layerName);

        if (layer == null) return;

        for (MapObject obj : layer.getObjects()) {
            obj.setVisible(false);
        }
    }

    private void turnOnAllCandles() {

        setLayerVisible("Candle_Off", true);

        MapLayer layer1 =
            map.getLayers().get("Candle_On_1");

        MapLayer layer2 =
            map.getLayers().get("Candle_On_2");

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

        // 2. XÓA VÙNG VA CHẠM
        MapLayer collisionLayer = map.getLayers().get("Hidden_Room_Collision");
        if (collisionLayer != null) {
            for (MapObject obj : collisionLayer.getObjects()) {
                if (obj instanceof RectangleMapObject) {
                    Rectangle rectToRemove = ((RectangleMapObject) obj).getRectangle();
                    // Xóa từ wallCollision
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

        if (layer != null) layer.setVisible(visible);
    }
}
