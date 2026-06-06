package game.maskedbee.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import game.maskedbee.entities.Boss;
import game.maskedbee.entities.Guard;
import game.maskedbee.entities.Player;
import game.maskedbee.main.CORE;
import game.maskedbee.main.AudioManager;
import game.maskedbee.main.NotificationManager;
import game.maskedbee.map.DialogueManager;
import game.maskedbee.map.PuzzleLibrary;
import game.maskedbee.map.PuzzleManager;
import game.maskedbee.map.StoryManager;
import game.maskedbee.objects.PushableBlock;


public class PlayScreen implements Screen {
    public final CORE game;
    public Player myPlayer;

    private static final String START_MAP = "Holding_Chamber.tmx";

    private OrthographicCamera camera;
    private Viewport viewport;

    public enum GameState {
        RUNNING, PAUSE, QUEEN_CHOICE, EXIT_CHOICE, TUTORIAL
    }

    private GameState state = GameState.RUNNING;

    private static boolean hasSeenTutorial = false;

    private boolean refusedQueenEnding = false;
    private boolean rescuedPrisoner = false;

    private boolean waxCountdownStarted = false;
    private float waxCountdownTimer = 0f;
    private static final float WAX_ESCAPE_TIME = 120f;
    private String pendingExitEndingType = "escape";

    private float portalCooldown = 0f;
    private float guardCatchCooldown = 0f;

    private ShapeRenderer shapeRender;
    private BitmapFont titleFont;       // Dùng cho chữ TẠM DỪNG
    private BitmapFont menuFont;        // Dùng cho các tùy chọn
    private BitmapFont choiceMenuFont;  // Dung cho Queen/Exit
    private BitmapFont hintFont;        // Dùng cho dòng "Bấm Space để chọn"
    private BitmapFont font;
    private Rectangle continueBtn;
    private Rectangle quitBtn;

    private String currentPrompt = "";

    private PuzzleLibrary puzzleLibrary;
    private PuzzleManager puzzleManager;
    private StoryManager storyManager;
    private DialogueManager dialogueManager;

    private int pauseSelectedIndex = 0; // bien de lua chon bang ban phim
    private int queenSelectedIndex = 0; // bien de lua chon bang ban phim
    private int exitSelectedIndex = 0;  // bien de lua chon bang ban phim
    private Texture arrowTexture; // Mui ten khi chon cac lua chon

    public PlayScreen(CORE game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(515, 290, camera);

        this.myPlayer = new Player(0, 0);

        this.shapeRender = new ShapeRenderer();

        // 1. Tạo font cho Tiêu đề (Size to nhất)
        this.titleFont = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        this.titleFont.getData().setScale(1.2f);

        // 2. Tạo font cho Menu (Size vừa)
        this.menuFont = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        this.menuFont.getData().setScale(0.8f);

        // 3. Chữ đếm ngược (Size nhỏ hơn xíu)
        this.font = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        this.font.getData().setScale(0.6f);

        // 4. Menu Lựa chọn trong hộp thoại (Queen/Exit)
        this.choiceMenuFont = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        this.choiceMenuFont.getData().setScale(0.5f);

        // 5. Chữ hướng dẫn "Bấm space để chọn"
        this.hintFont = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        this.hintFont.getData().setScale(0.45f);

        continueBtn = new Rectangle(0, 0, 100, 30);
        quitBtn = new Rectangle(0, 0, 100, 30);

        puzzleManager = new PuzzleManager();
        dialogueManager = new DialogueManager();
        storyManager = new StoryManager();
    }

    private void spawnPlayer(String fromMap) {
        Rectangle spawn = null;

        if (fromMap != null && !fromMap.isEmpty()) {
            spawn = game.map.getSpawnPoint(fromMap);
        }

        if (spawn == null) {
            spawn = game.map.getPlayerSpawn();
        }

        if (spawn != null) {
            myPlayer.x = spawn.x;
            myPlayer.y = spawn.y;
            myPlayer.hitbox.setPosition(spawn.x, spawn.y);
        } else {
            float safeX = Math.max(16f, game.map.getMapWidth() / 2f);
            float safeY = Math.max(16f, game.map.getMapHeight() / 2f);

            myPlayer.x = safeX;
            myPlayer.y = safeY;
            myPlayer.hitbox.setPosition(safeX, safeY);

            System.out.println("WARNING: Khong tim thay player_spawn trong map "
                + game.map.getCurrentMapName()
                + ", tam spawn o giua map.");
        }

        myPlayer.isHidingAtStone = false;
        myPlayer.noiseRadius = 0f;

        portalCooldown = 0.45f;
        guardCatchCooldown = 0f;
    }

    private void updateCamera() {
        float mapWidth = game.map.getMapWidth();
        float mapHeight = game.map.getMapHeight();

        float halfWidth = viewport.getWorldWidth() / 2f;
        float halfHeight = viewport.getWorldHeight() / 2f;

        float camX = myPlayer.x;
        float camY = myPlayer.y;

        if (mapWidth <= viewport.getWorldWidth()) {
            camX = mapWidth / 2f;
        } else {
            camX = com.badlogic.gdx.math.MathUtils.clamp(camX, halfWidth, mapWidth - halfWidth);
        }

        if (mapHeight <= viewport.getWorldHeight()) {
            camY = mapHeight / 2f;
        } else {
            camY = com.badlogic.gdx.math.MathUtils.clamp(camY, halfHeight, mapHeight - halfHeight);
        }

        camera.position.set(camX, camY, 0);
        camera.update();
    }

    private void recreatePuzzleLibrary() {
        puzzleLibrary = new PuzzleLibrary(game.map, dialogueManager);
    }

    private void reloadCurrentMapAndRespawn() {
        game.map.clearSpikeLeverStateForCurrentMap();

        game.map.loadMap("map/" + game.map.getCurrentMapName());
        recreatePuzzleLibrary();
        spawnPlayer(null);
        game.map.updateFloorHide(myPlayer);
        updateCamera();
    }

    @Override
    public void show() {
        // Khởi tạo texture mui ten ở đây để an toàn cho đồ họa
        if (arrowTexture == null) {
            arrowTexture = new Texture(Gdx.files.internal("menu/pointer.png"));
        }

        game.map.loadMap("map/" + START_MAP);
        recreatePuzzleLibrary();
        spawnPlayer(null);
        game.map.updateFloorHide(myPlayer);
        camera.position.set(myPlayer.x, myPlayer.y, 0);
        updateCamera();
        storyManager.checkNewGameIntro(dialogueManager); // KÍCH HOẠT THOẠI MỞ ĐẦU GAME

        if (storyManager != null && dialogueManager != null) {
            storyManager.checkMapEnterEvent("map/" + START_MAP, dialogueManager);
        }

        if (!hasSeenTutorial) {
            state = GameState.TUTORIAL;
        }
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (state == GameState.RUNNING) {
                state = GameState.PAUSE;
                // TẠM DỪNG NHẠC NỀN KHI PAUSE
                AudioManager.getInstance().pauseBackgroundMusic();
            } else if (state == GameState.PAUSE) {
                state = GameState.RUNNING;
                // TIẾP TỤC PHÁT NHẠC NỀN KHI CHƠI TIẾP
                AudioManager.getInstance().resumeBackgroundMusic();
            }
        }

        if (state == GameState.TUTORIAL) {
            handleTutorialLogic();
        }else if (state == GameState.RUNNING) {
            updateRunning(delta);
        } else if (state == GameState.PAUSE) {
            handlePauseMenuLogic();
        } else if (state == GameState.QUEEN_CHOICE) {
            handleQueenChoiceLogic();
        } else if (state == GameState.EXIT_CHOICE) {
            handleExitChoiceLogic();
        }

        drawGame();

        //VẼ THÔNG BÁO NỔI (NOTIFICATION)
        NotificationManager.getInstance().update(delta);
        String notifMsg = NotificationManager.getInstance().currentMessage;
        float notifTimer = NotificationManager.getInstance().timer;

        // Chỉ vẽ khi có thông báo và game đang chạy
        if (notifMsg != null && !notifMsg.isEmpty() && state == GameState.RUNNING) {
            // Tính toán hiệu ứng mờ dần (Fade out) ở giây cuối cùng
            float alpha = Math.min(0.8f, notifTimer);

            // 1. Đo kích thước CHUẨN XÁC của dòng chữ (Tự động nhận diện xuống dòng \n)
            GlyphLayout notifLayout = new GlyphLayout(hintFont, notifMsg);

            // Tính toán kích thước khung đen (Thêm 15px lề trái phải, 8px lề trên dưới cho thoáng)
            float paddingX = 15f;
            float paddingY = 8f;
            float boxWidth = notifLayout.width + (paddingX * 2);
            float boxHeight = notifLayout.height + (paddingY * 2);

            // Vị trí khung nền (Căn giữa nửa dưới màn hình)
            float boxX = camera.position.x - boxWidth / 2f;
            float boxY = camera.position.y - 110f; // Hạ thấp xuống gần cạnh dưới

            // =========================================================
            // 2. VẼ KHUNG NỀN ĐEN TRƯỚC (Bằng ShapeRenderer)
            // =========================================================
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRender.setProjectionMatrix(camera.combined);
            shapeRender.begin(ShapeRenderer.ShapeType.Filled);
            // Cài đặt màu nền đen. Nhân thêm 0.7f vào alpha để nền trong suốt hơn chữ một tí (Hiệu ứng kính mờ)
            shapeRender.setColor(0f, 0f, 0f, alpha * 0.7f);
            shapeRender.rect(boxX, boxY, boxWidth, boxHeight);
            shapeRender.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);

            // =========================================================
            // 3. VẼ CHỮ LÊN TRÊN KHUNG (Bằng SpriteBatch)
            // =========================================================
            game.batch.setProjectionMatrix(camera.combined);
            game.batch.begin();

            // Chữ màu trắng kèm hiệu ứng mờ dần
            hintFont.setColor(1f, 1f, 1f, alpha);

            // Vẽ chữ. Tọa độ Y của LibGDX khi draw text tính từ ĐỈNH của dòng chữ cao nhất
            float textX = boxX + paddingX;
            float textY = boxY + boxHeight - paddingY;

            // Align.center giúp các dòng chữ tự động căn giữa với nhau nếu có 1 dòng dài 1 dòng ngắn
            hintFont.draw(game.batch, notifMsg, textX, textY, notifLayout.width, Align.center, false);

            // Bắt buộc reset về màu trắng để không làm hỏng màu UI khác
            hintFont.setColor(Color.WHITE);
            game.batch.end();
        }

        if (!currentPrompt.isEmpty() && state == GameState.RUNNING) {
            game.batch.setProjectionMatrix(camera.combined);
            game.batch.begin();
            hintFont.draw(game.batch, currentPrompt, myPlayer.x - 10, myPlayer.y + 40);
            game.batch.end();
        }
        if (waxCountdownStarted && state == GameState.RUNNING) {
            drawWaxCountdownTimer();
        }

        if (dialogueManager != null && state == GameState.RUNNING) {
            dialogueManager.draw(game.batch, camera);
        }

        if (state == GameState.TUTORIAL) {
            drawTutorial();
        }

        if (state == GameState.PAUSE) {
            drawPauseMenu();
        }

        if (state == GameState.QUEEN_CHOICE) {
            drawQueenChoiceMenu();
        }

        if (state == GameState.EXIT_CHOICE) {
            drawExitChoiceMenu();
        }
    }

    private void updateRunning(float delta) {
        // ========================================================
        // 🛠️ DEBUG / CHEAT KEYS (Dành riêng cho Dev test game)
        // Nhớ xóa hoặc comment đoạn này lại trước khi nộp bài/xuất game!
        // ========================================================
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) { goToEnding("queen"); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) { goToEnding("escape"); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) { goToEnding("boss"); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) { goToEnding("no_mask"); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) { goToEnding("lab_escape"); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) { goToEnding("lab_explosion"); return; }
        // ========================================================
        // ========================================================



        if (portalCooldown > 0f) {
            portalCooldown -= delta;
        }

        if (guardCatchCooldown > 0f) {
            guardCatchCooldown -= delta;
        }
        if (updateWaxCountdown(delta)) {
            return;
        }

        if (dialogueManager != null) {
            //Kiểm tra xem thoại có đang bật không
            boolean wasShowing = dialogueManager.isShowing;

            dialogueManager.update(delta);

            //Nếu có thoại thì khỏi di chuyển đi
            if (wasShowing) {
                currentPrompt = ""; // Ẩn luôn gợi ý phím khi thoại đang mở
                return;
            }
        }

        if (storyManager != null && dialogueManager != null) {
            storyManager.handleExamine(
                myPlayer.hitbox,
                game.map.getMap(),
                dialogueManager,
                game.map.getPushables(),
                game.map.getCurrentMapName()
            );
        }

        updateInteractionPrompt();

        if (puzzleLibrary != null) {
            puzzleLibrary.update(myPlayer.hitbox, delta);
        }
        // PuzzleManager xử lý: E gạt cần/gai/cửa, F mở cửa bằng key, nhặt key, đẩy block.
        if (puzzleManager != null) {
            puzzleManager.update(myPlayer, game.map);
        }
        handlePrisonerAndWaxEndingInteraction();

        checkQueenInteraction();

        if (puzzleManager != null && puzzleManager.checkSpikeDeath(myPlayer, game.map)) {
            reloadCurrentMapAndRespawn();
            return;
        }

        if (portalCooldown <= 0f) {
            String nextMap = game.map.checkPortal(myPlayer.hitbox);

            if (nextMap != null) {
                String currentMap = game.map.getCurrentMapName();
                // Sau khi từ chối Queen, đi vào Exit thì chưa ending ngay.
                // Mở bảng lựa chọn Continue / Exit.
                if (isFinalExitPortal(currentMap, nextMap)) {
                    // Nếu đang chạy timer Wax Lab thì thoát kịp -> ending riêng
                    if (waxCountdownStarted) {
                        waxCountdownStarted = false;
                        waxCountdownTimer = 0f;

                        goToEnding("lab_escape");
                        return;
                    }

                    pendingExitEndingType = getExitEndingType();
                    state = GameState.EXIT_CHOICE;
                    portalCooldown = 0.7f;
                    System.out.println("Exit choice opened. Ending type = " + pendingExitEndingType);
                    return;
                }

                String lastMap = currentMap;

                game.map.loadMap(nextMap);
                recreatePuzzleLibrary();
                spawnPlayer(lastMap);
                game.map.updateFloorHide(myPlayer);
                updateCamera();

                if (storyManager != null && dialogueManager != null) {
                    storyManager.checkMapEnterEvent(nextMap, dialogueManager);
                }

                return;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            game.map.clearPushableStateForCurrentMap();
            game.map.resetPushables();

            String resetSpawnFromMap = game.map.getLastMapName();

            if ("Disposal.tmx".equalsIgnoreCase(game.map.getCurrentMapName())) {
                resetSpawnFromMap = "Corridor.tmx";
            }

            spawnPlayer(resetSpawnFromMap);

            game.map.updateFloorHide(myPlayer);
            updateCamera();
        }

        // Dùng full collision để Player bị chặn bởi tường + cửa tù + block.
        // Hidden_Room_Collision vẫn xóa được vì PuzzleLibrary xóa nó khỏi wallCollision.
        myPlayer.update(delta, game.map.getFullCollision(), game.map.getStoneCollision());

        game.map.updateFloorHide(myPlayer);

        if (guardCatchCooldown <= 0f) {
            for (Guard guard : game.map.guards) {
                boolean caught = guard.update(delta, myPlayer, game.map.getWallCollision());

                if (caught) {
                    handleCaughtByGuard();
                    return;
                }
            }
        }

        for (Boss boss : game.map.bosses) {
            boolean caught = boss.update(
                delta,
                myPlayer,
                game.map.getWallCollision(),
                game.map.getSkullCollision()
            );

            if (caught) {
                goToEnding("boss");
                return;
            }
        }

        updateCamera();
    }
    private void drawWaxCountdownTimer() {
        int secondsLeft = Math.max(0, (int) Math.ceil(waxCountdownTimer));
        int minutes = secondsLeft / 60;
        int seconds = secondsLeft % 60;

        String text = String.format("SELF-DESTRUCT %02d:%02d", minutes, seconds);

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        font.draw(
            game.batch,
            text,
            camera.position.x - viewport.getWorldWidth() / 2f + 12f,
            camera.position.y + viewport.getWorldHeight() / 2f - 12f
        );

        game.batch.end();
    }
    private boolean isFinalExitPortal(String currentMap, String nextMap) {
        if (currentMap == null || nextMap == null) return false;

        String lowerNextMap = nextMap.toLowerCase();

        // Case chính của bạn:
        // Đang ở Exit_Chamber.tmx, portal tên Exit.tmx -> nextMap = map/Exit.tmx
        if ("Exit_Chamber.tmx".equalsIgnoreCase(currentMap)
            && lowerNextMap.endsWith("exit.tmx")) {
            return true;
        }

        // Dự phòng nếu sau này đặt portal cuối trực tiếp trong Queen_Chamber là Exit.tmx.
        // Không bắt Exit_Chamber.tmx để tránh chặn nhầm cổng đi sang phòng Exit_Chamber.
        return "Queen_Chamber.tmx".equalsIgnoreCase(currentMap)
            && lowerNextMap.endsWith("exit.tmx")
            && !lowerNextMap.contains("exit_chamber");
    }

    private String getExitEndingType() {
        // Chưa có mặt nạ mà đi tới Exit cuối
        if (!myPlayer.hasMask) {
            return "no_mask";
        }

        if (!myPlayer.hasActivatedMask) {
            return "inactive_mask";
        }

        // Có mặt nạ và đã từ chối Queen
        if (refusedQueenEnding) {
            return "escape";
        }

        return "escape";
    }
    private void handlePrisonerAndWaxEndingInteraction() {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            return;
        }

        String currentMap = game.map.getCurrentMapName();

        // 1. Cứu tù nhân ở Holding_Chamber
        if ("Holding_Chamber.tmx".equalsIgnoreCase(currentMap)) {
            if (isNearInteractObject("prisoner", "prisoner_interact")) {
                rescuedPrisoner = true;

                System.out.println("Prisoner rescued. Wax pump can now be activated.");
                NotificationManager.getInstance().show("Bạn đã nhận được bật lửa từ tù nhân.");
                return;
            }
        }

        // 2. Kích hoạt Wax_Pumb sau khi đã cứu tù nhân
        if ("Wax_Pumb.tmx".equalsIgnoreCase(currentMap)
            || "Wax_Pump.tmx".equalsIgnoreCase(currentMap)) {

            if (!isNearInteractObject("wax_star", "wax_control", "wax_vent", "wax_pump")) {
                return;
            }

            if (!rescuedPrisoner) {
                System.out.println("You need to rescue the prisoner first.");
                NotificationManager.getInstance().show("Bạn cần giải cứu tù nhân trước.");
                return;
            }

            if (waxCountdownStarted) {
                System.out.println("Countdown already started.");
                NotificationManager.getInstance().show("Bắt đầu đếm ngược!");
                return;
            }

            waxCountdownStarted = true;
            waxCountdownTimer = WAX_ESCAPE_TIME;

            System.out.println("Wax lab self-destruct started! Escape in 2 minutes.");
            NotificationManager.getInstance().show("Phòng thí nghiệm sẽ bị tự hủy!\nHãy trốn thoát trong hai phút");
        }
    }

    private boolean isNearInteractObject(String... names) {
        for (RectangleMapObject obj : game.map.getInteractPoints()) {
            if (obj == null || obj.getName() == null) {
                continue;
            }

            for (String name : names) {
                if (name.equalsIgnoreCase(obj.getName())
                    && myPlayer.hitbox.overlaps(obj.getRectangle())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean updateWaxCountdown(float delta) {
        if (!waxCountdownStarted) {
            return false;
        }

        waxCountdownTimer -= delta;

        if (waxCountdownTimer <= 0f) {
            waxCountdownStarted = false;
            waxCountdownTimer = 0f;

            System.out.println("Wax lab exploded!");
            goToEnding("lab_explosion");
            return true;
        }

        return false;
    }

    private void checkQueenInteraction() {
        if (!"Queen_Chamber.tmx".equalsIgnoreCase(game.map.getCurrentMapName())) {
            return;
        }

        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            return;
        }

        if (!myPlayer.hasMask) {
            NotificationManager.getInstance().show("Bạn cần có mặt nạ trước khi nói chuyện với nữ hoàng");
            System.out.println("Ban can co mat na truoc khi noi chuyen voi Queen.");
            return;
        }

        if (!myPlayer.hasActivatedMask) {
            NotificationManager.getInstance().show("Bạn cần kích hoạt mặt nạ ở Old Chapel trước");
            System.out.println("Ban can kich hoat mat na o Old Chapel truoc.");
            return;
        }

        for (RectangleMapObject obj : game.map.getInteractPoints()) {
            if (!"queen_flower".equals(obj.getName())) {
                continue;
            }

            if (myPlayer.hitbox.overlaps(obj.getRectangle())) {
                state = GameState.QUEEN_CHOICE;
                currentPrompt = "";
                System.out.println("Queen choice opened");
                return;
            }
        }
    }
    private void handleExitChoiceLogic() {
        // 1. ĐIỀU KHIỂN DI CHUYỂN LÊN/XUỐNG bằng biến exitSelectedIndex riêng biệt
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            exitSelectedIndex = 0; // Chọn "Thoát"
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            exitSelectedIndex = 1; // Chọn "Quay lại"
        }

        // 2. XÁC NHẬN LỰA CHỌN (Khong dung chuot, chi dung phim)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (exitSelectedIndex == 0) {
                goToEnding(pendingExitEndingType);
            } else if (exitSelectedIndex == 1) {
                continueFromExitChoice();
            }
        }
    }

    private void continueFromExitChoice() {
        state = GameState.RUNNING;
        portalCooldown = 0.7f;
        System.out.println("Continue playing");
    }

    private void goToEnding(String endingType) {
        AudioManager.getInstance().stopBackgroundMusic(); // tắt nhạc nền
        System.out.println("Ending type: " + endingType);
        game.setScreen(new EndingScreen(game, endingType));
    }

    private void handleQueenChoiceLogic() {
        // 1. ĐIỀU KHIỂN DI CHUYỂN LÊN/XUỐNG (Bằng W/S hoặc Mũi tên)
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            queenSelectedIndex = 0; // Chọn "Có" (nằm trên)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            queenSelectedIndex = 1; // Chọn "Không" (nằm dưới)
        }

        // 2. XÁC NHẬN LỰA CHỌN (Khong dung chuot, chi dung phim)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (queenSelectedIndex == 0) {
                // Đã chọn "Có"
                goToEnding("queen");
            } else if (queenSelectedIndex == 1) {
                // Đã chọn "Không"
                refuseQueenChoice();
                queenSelectedIndex = 0; // Reset lại index cho lần sau nếu cần
            }
        }
    }

    private void handleCaughtByGuard() {
        NotificationManager.getInstance().show("Bạn đã bị Quái bắt");
        System.out.println("Caught by guard!");

        state = GameState.RUNNING;
        currentPrompt = "";

        myPlayer.isBeeDisguised = false;
        myPlayer.isHidingAtStone = false;
        myPlayer.noiseRadius = 0f;

        portalCooldown = 0.45f;
        guardCatchCooldown = 0f;

        if (game.map.hasProgressCheckpoint()) {
            String checkpointMap = game.map.getProgressCheckpointMapName();

            System.out.println("Respawn at solved puzzle checkpoint: " + checkpointMap);

            game.map.loadMap("map/" + checkpointMap);
            recreatePuzzleLibrary();
            spawnPlayer(null);
            game.map.updateFloorHide(myPlayer);
            updateCamera();

            if (storyManager != null && dialogueManager != null) {
                storyManager.checkMapEnterEvent("map/" + checkpointMap, dialogueManager);
            }

            return;
        }

        System.out.println("No solved puzzle checkpoint. Restart from first map.");

        resetPlayerProgressToStart();

        game.map.loadMap("map/" + START_MAP);
        recreatePuzzleLibrary();
        spawnPlayer(null);
        game.map.updateFloorHide(myPlayer);
        updateCamera();

        if (storyManager != null && dialogueManager != null) {
            storyManager.checkMapEnterEvent("map/" + START_MAP, dialogueManager);
        }
    }

    private void resetPlayerProgressToStart() {
        refusedQueenEnding = false;
        currentPrompt = "";
        rescuedPrisoner = false;
        waxCountdownStarted = false;
        waxCountdownTimer = 0f;

        myPlayer.hasMask = false;
        myPlayer.hasActivatedMask = false;
        myPlayer.hasMaskItem = false;
        myPlayer.hasKeyItem = false;
        myPlayer.currentKey = "";
        myPlayer.isBeeDisguised = false;
        myPlayer.isHidingAtStone = false;
        myPlayer.noiseRadius = 0f;

        game.map.clearAllProgressState();
    }

    private void refuseQueenChoice() {
        refusedQueenEnding = true;
        state = GameState.RUNNING;
        portalCooldown = 0.45f;

        System.out.println("Refused queen ending. Exit is now escape ending.");
    }

    private void drawGame() {
        ScreenUtils.clear(0, 0, 0, 1);

        float sortY = game.map.getSortY();

        if (myPlayer.y < sortY) {
            game.map.renderBackground(camera, myPlayer.isBeeDisguised);
            game.map.renderForeground(camera, myPlayer.isBeeDisguised);
            drawEntities();
        } else {
            game.map.renderBackground(camera, myPlayer.isBeeDisguised);
            drawEntities();
            game.map.renderForeground(camera, myPlayer.isBeeDisguised);
        }
    }

    private void drawEntities() {
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        float playerFootY = myPlayer.hitbox.y;

        for (PushableBlock block : game.map.getPushables()) {
            float blockFootY = block.getBounds().y;

            if (blockFootY > playerFootY) {
                block.render(game.batch);
            }
        }

        myPlayer.draw(game.batch);

        for (PushableBlock block : game.map.getPushables()) {
            float blockFootY = block.getBounds().y;

            if (blockFootY <= playerFootY) {
                block.render(game.batch);
            }
        }

        for (Guard guard : game.map.guards) {
            guard.draw(game.batch);
        }

        for (Boss boss : game.map.bosses) {
            boss.draw(game.batch);
        }

        game.batch.end();

        shapeRender.setProjectionMatrix(camera.combined);
        shapeRender.begin(ShapeRenderer.ShapeType.Line);

        for (Guard guard : game.map.guards) {
            guard.drawDebug(shapeRender, myPlayer);
        }

        for (Boss boss : game.map.bosses) {
            boss.drawDebug(shapeRender, myPlayer);
        }

        shapeRender.end();
    }

    // Hàm xử lý bấm nút để tắt Tutorial
    private void handleTutorialLogic() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            state = GameState.RUNNING;
            hasSeenTutorial = true; // Đánh dấu là đã xem
        }
    }

    // Hàm vẽ cái bảng hướng dẫn lên màn hình
    private void drawTutorial() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRender.setProjectionMatrix(camera.combined);

        // 1. VẼ CÁC MẢNG NỀN TRONG SUỐT (ShapeType.Filled)
        shapeRender.begin(ShapeRenderer.ShapeType.Filled);

        // Nền đen làm tối toàn màn hình (Giảm xuống 0.6f cho đỡ tối tăm)
        shapeRender.setColor(0f, 0f, 0f, 0.6f);
        shapeRender.rect(
            camera.position.x - viewport.getWorldWidth() / 2,
            camera.position.y - viewport.getWorldHeight() / 2,
            viewport.getWorldWidth(),
            viewport.getWorldHeight()
        );

        // Khung bảng chính: Pha màu đen xám với độ trong suốt 0.75f (Kính mờ)
        shapeRender.setColor(0.1f, 0.1f, 0.1f, 0.75f);
        shapeRender.rect(camera.position.x - 170, camera.position.y - 100, 360, 220);

        shapeRender.end();

        // 2. VẼ ĐƯỜNG VIỀN BAO QUANH CHO SANG CHẢNH (ShapeType.Line)
        shapeRender.begin(ShapeRenderer.ShapeType.Line);

        // Viền màu vàng gold (hoặc đổi thành Color.WHITE tùy gu)
        shapeRender.setColor(Color.GOLD);
        shapeRender.rect(camera.position.x - 170, camera.position.y - 125, 340, 225);

        shapeRender.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ... (Phần vẽ chữ bên dưới giữ nguyên không đổi)
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        font.setColor(Color.PINK);
        font.draw(game.batch, "--- HƯỚNG DẪN CƠ BẢN ---", camera.position.x - 91, camera.position.y + 80);

        font.setColor(Color.WHITE);
        hintFont.draw(game.batch, "W, A, S, D hoặc UP, LEFT, DOWN, RIGHT: Di chuyển", camera.position.x - 160, camera.position.y + 50);
        hintFont.draw(game.batch, "G : Tìm manh mối", camera.position.x - 160, camera.position.y + 25);
        hintFont.draw(game.batch, "E : Tương tác đồ vật", camera.position.x - 160, camera.position.y);
        hintFont.draw(game.batch, "F : Mở khóa cửa/Núp", camera.position.x - 160, camera.position.y - 25);
        hintFont.draw(game.batch, "P : Đeo/Tháo mặt nạ ", camera.position.x - 160, camera.position.y - 50);
        hintFont.draw(game.batch, "SPACE : Đi qua phòng tiếp theo", camera.position.x - 160, camera.position.y - 75);

        // Hiệu ứng chữ nhấp nháy báo hiệu bấm phím
        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            font.setColor(Color.PINK);
        } else {
            font.setColor(Color.RED);
        }
        font.draw(game.batch, "Bấm SPACE để bắt đầu", camera.position.x - 81, camera.position.y - 100);

        font.setColor(Color.WHITE); // Reset màu
        game.batch.end();
    }

    private void drawExitChoiceMenu() {
        // VE LAI KHUNG CHON GIONG DIALOGUE
        // TÍNH TOÁN VỊ TRÍ KHUNG
        float camX = camera.position.x;
        float camY = camera.position.y;
        float viewWidth = camera.viewportWidth;
        float viewHeight = camera.viewportHeight;

        float boxWidth = viewWidth * 0.9f; // Rộng 90% màn hình
        float boxHeight = 80f;
        float boxX = camX - (boxWidth / 2f);
        float boxY = camY - (viewHeight / 2f) + 10f; // Cách đáy 10 pixel

        // 2. VẼ KHUNG NỀN
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRender.setProjectionMatrix(camera.combined);
        shapeRender.begin(ShapeRenderer.ShapeType.Filled);
        // Nền đen mờ
        shapeRender.setColor(0, 0, 0, 0.8f);
        shapeRender.rect(boxX, boxY, boxWidth, boxHeight); // Khung to
        // Viền trắng
        shapeRender.setColor(Color.WHITE);
        // Viền khung to (chỉ vẽ viền dưới và viền trên giống hàm draw)
        shapeRender.rectLine(boxX, boxY, boxX + boxWidth, boxY, 2f);
        shapeRender.rectLine(boxX, boxY + boxHeight, boxX + boxWidth, boxY + boxHeight, 2f);
        shapeRender.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 3. VẼ CHỮ VÀ CÁC LỰA CHỌN (Dùng SpriteBatch)
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Tọa độ gốc để vẽ chữ bên trong hộp thoại
        float textStartX = boxX + 20f;
        float textStartY = boxY + boxHeight - 10f;
        float arrowWidth = 14f;
        float arrowHeight = 14f;

        // --- VẼ CÂU HỎI ---
        choiceMenuFont.setColor(Color.WHITE);
        choiceMenuFont.draw(game.batch, "Bạn muốn thoát khỏi đây?", textStartX, textStartY, boxWidth - 40f, Align.left, true);

        // --- LỰA CHỌN 1: CÓ (Vẽ thấp xuống 22 pixel) ---
        float optionYesY = textStartY - 22f;
        if (exitSelectedIndex == 0) {
            // Vẽ mũi tên ngay trước chữ "Có"
            game.batch.draw(arrowTexture, textStartX, optionYesY - 11f, arrowWidth, arrowHeight);
        } else {
            choiceMenuFont.setColor(Color.WHITE);
        }
        // Dịch chữ sang phải một chút (20px) để nhường chỗ cho mũi tên
        choiceMenuFont.draw(game.batch, "Thoát", textStartX + 20f, optionYesY, boxWidth - 60f, Align.left, true);

        // --- LỰA CHỌN 2: KHÔNG (Vẽ thấp xuống tiếp 20 pixel) ---
        float optionNoY = optionYesY - 20f;
        if (exitSelectedIndex == 1) {
            // Vẽ mũi tên ngay trước chữ "Không"
            game.batch.draw(arrowTexture, textStartX, optionNoY - 11f, arrowWidth, arrowHeight);
        } else {
            choiceMenuFont.setColor(Color.WHITE);
        }
        choiceMenuFont.draw(game.batch, "Ở lại", textStartX + 20f, optionNoY, boxWidth - 60f, Align.left, true);

        game.batch.end();
    }

    private void drawQueenChoiceMenu() {
        // VE LAI KHUNG CHON CHO GIONG DIALOGUE
        // TÍNH TOÁN VỊ TRÍ KHUNG
        float camX = camera.position.x;
        float camY = camera.position.y;
        float viewWidth = camera.viewportWidth;
        float viewHeight = camera.viewportHeight;

        float boxWidth = viewWidth * 0.9f; // Rộng 90% màn hình
        float boxHeight = 80f;
        float boxX = camX - (boxWidth / 2f);
        float boxY = camY - (viewHeight / 2f) + 10f; // Cách đáy 10 pixel

        // 2. VẼ KHUNG NỀN
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRender.setProjectionMatrix(camera.combined);
        shapeRender.begin(ShapeRenderer.ShapeType.Filled);
        // Nền đen mờ
        shapeRender.setColor(0, 0, 0, 0.8f);
        shapeRender.rect(boxX, boxY, boxWidth, boxHeight); // Khung to
        // Viền trắng
        shapeRender.setColor(Color.WHITE);
        // Viền khung to (chỉ vẽ viền dưới và viền trên giống hàm draw)
        shapeRender.rectLine(boxX, boxY, boxX + boxWidth, boxY, 2f);
        shapeRender.rectLine(boxX, boxY + boxHeight, boxX + boxWidth, boxY + boxHeight, 2f);
        shapeRender.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 3. VẼ CHỮ VÀ CÁC LỰA CHỌN (Dùng SpriteBatch)
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Tọa độ gốc để vẽ chữ bên trong hộp thoại
        float textStartX = boxX + 20f;
        float textStartY = boxY + boxHeight - 10f;
        float arrowWidth = 14f;
        float arrowHeight = 14f;

        // --- VẼ CÂU HỎI ---
        choiceMenuFont.setColor(Color.WHITE);
        choiceMenuFont.draw(game.batch, "Trở thành ong chúa?", textStartX, textStartY, boxWidth - 40f, Align.left, true);

        // --- LỰA CHỌN 1: CÓ (Vẽ thấp xuống 22 pixel) ---
        float optionYesY = textStartY - 22f;
        if (queenSelectedIndex == 0) {
            // Vẽ mũi tên ngay trước chữ "Có"
            game.batch.draw(arrowTexture, textStartX, optionYesY - 11f, arrowWidth, arrowHeight);
        } else {
            choiceMenuFont.setColor(Color.WHITE);
        }
        // Dịch chữ sang phải một chút (20px) để nhường chỗ cho mũi tên
        choiceMenuFont.draw(game.batch, "Có", textStartX + 20f, optionYesY, boxWidth - 60f, Align.left, true);

        // --- LỰA CHỌN 2: KHÔNG (Vẽ thấp xuống tiếp 20 pixel) ---
        float optionNoY = optionYesY - 20f;
        if (queenSelectedIndex == 1) {
            // Vẽ mũi tên ngay trước chữ "Không"
            game.batch.draw(arrowTexture, textStartX, optionNoY - 11f, arrowWidth, arrowHeight);
        } else {
            choiceMenuFont.setColor(Color.WHITE);
        }
        choiceMenuFont.draw(game.batch, "Không", textStartX + 20f, optionNoY, boxWidth - 60f, Align.left, true);

        game.batch.end();
    }

    private boolean checkLayerOverlap(String layerName, Rectangle interactRange) {
        if (game.map.getMap() == null) return false;

        MapLayer layer = game.map.getMap().getLayers().get(layerName);

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
        if (game.map.getMap() == null) return false;

        for (MapLayer layer : game.map.getMap().getLayers()) {
            if (layer == null) continue;

            String layerName = layer.getName();

            boolean portalLayer =
                layerName.equals("Exit")
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

    private void updateInteractionPrompt() {
        currentPrompt = "";

        Rectangle interactRange = new Rectangle(
            myPlayer.hitbox.x - 5f,
            myPlayer.hitbox.y - 5f,
            myPlayer.hitbox.width + 10f,
            myPlayer.hitbox.height + 10f
        );

        // Tạo các công tắc để nhớ xem phím nào cần hiển thị
        boolean showG = false;
        boolean showF = false;
        boolean showE = false;
        boolean showSpace = false;

        // QUÉT CÁC LAYER
        // Quét G
        if (game.map.getPushables() != null) {
            for (PushableBlock block : game.map.getPushables()) {
                if (interactRange.overlaps(block.getBounds())) {
                    showG = true;
                    break; // Chỉ dùng break để thoát vòng lặp con này, tiếp tục quét cái khác
                }
            }
        }
        if (!showG && checkLayerOverlap("ExaminePoints", interactRange)) {
            showG = true;
        }

        // Quét F
        // Quét mở khóa cửa
        if (checkLayerOverlap("Doors", interactRange)) {
            showF = true;
        }
        // Quét núp cột
        if (myPlayer.isHidingAtStone) {
            showF = true;
        }
        // Nếu chưa núp, quét xem có đứng gần Cửa hoặc Cột đá (Stone_Collision) không
        else if (checkLayerOverlap("Doors", interactRange) || checkLayerOverlap("Stone_Collision", interactRange)) {
            showF = true;
        }

        // Quét E
        if (checkLayerOverlap("Queen_Interact", interactRange) || checkLayerOverlap("Chapel_Interact", interactRange)) {
            showE = true;
        }
        if (!showE) {
            for (int i = 0; i <= 5; i++) {
                if (checkLayerOverlap("Interact_Point_" + i, interactRange)) {
                    showE = true;
                    break;
                }
            }
        }

        // Quét SPACE
        if (checkPortalPromptOverlap(interactRange)) {
            showSpace = true;
        }

        // GHÉP CÁC PHÍM LẠI THÀNH MỘT CHUỖI
        java.util.ArrayList<String> prompts = new java.util.ArrayList<>();

        if (showE) prompts.add("[E]");
        if (showF) prompts.add("[F]");
        if (showG) prompts.add("[G]");
        if (showSpace) prompts.add("[SPACE]");

        // Tiến hành ghép chuỗi với dấu " / "
        if (prompts.isEmpty()) {
            currentPrompt = "";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < prompts.size(); i++) {
                sb.append(prompts.get(i));
                // Nếu chưa phải là phím cuối cùng thì chèn thêm dấu gạch chéo
                if (i < prompts.size() - 1) {
                    sb.append(" / ");
                }
            }
            currentPrompt = sb.toString();
        }
    }

    private void handlePauseMenuLogic() {
        float centerX = camera.position.x;
        float centerY = camera.position.y;

        continueBtn.set(centerX - 50, centerY - 15, 100, 25);
        quitBtn.set(centerX - 45, centerY - 40, 90, 25);

        // Lấy tọa độ chuột hiện tại và chuyển đổi về tọa độ World của Camera
        Vector3 touchPoint = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(touchPoint);

        if (continueBtn.contains(touchPoint.x, touchPoint.y)) {
            pauseSelectedIndex = 0;
        } else if (quitBtn.contains(touchPoint.x, touchPoint.y)) {
            pauseSelectedIndex = 1;
        }

        // Xử lý điều khiển bằng phím mũi tên (Tùy chọn bổ sung cho mượt)
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            pauseSelectedIndex = 0;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            pauseSelectedIndex = 1;
        }

        // 2. XỬ LÝ PHÍM XÁC NHẬN (ENTER HOẶC SPACE)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (pauseSelectedIndex == 0) {
                // Chọn "Tiếp tục"
                state = GameState.RUNNING;
                AudioManager.getInstance().resumeBackgroundMusic(); // Tiếp tục phát nhạc nền
            } else if (pauseSelectedIndex == 1) {
                // Chọn "Thoát"
                returnToMainMenu();
            }
        }

        // --- XỬ LÝ CLICK CHUỘT THUẦN TÚY ---
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (continueBtn.contains(touchPoint.x, touchPoint.y)) {
                state = GameState.RUNNING;
                AudioManager.getInstance().resumeBackgroundMusic();
            } else if (quitBtn.contains(touchPoint.x, touchPoint.y)) {
                AudioManager.getInstance().stopBackgroundMusic();
                game.setScreen(new FirstScreen(game));
            }
        }
    }

    private void drawPauseMenu() {
        // VE LAI
        // 1. Vẽ lớp phủ tối toàn màn hình
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRender.setProjectionMatrix(camera.combined);
        shapeRender.begin(ShapeRenderer.ShapeType.Filled);
        shapeRender.setColor(0, 0, 0, 0.5f);
        shapeRender.rect(
            camera.position.x - viewport.getWorldWidth() / 2,
            camera.position.y - viewport.getWorldHeight() / 2,
            viewport.getWorldWidth(),
            viewport.getWorldHeight()
        );
        shapeRender.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 2. Vẽ chữ căn giữa bằng font size gốc
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        float viewWidth = viewport.getWorldWidth();
        float viewHeight = viewport.getWorldHeight();
        float startX = camera.position.x - viewWidth / 2;
        float centerX = camera.position.x;
        float centerY = camera.position.y;

        // --- TIÊU ĐỀ: TẠM DỪNG (Dùng titleFont gốc, không scale) ---
        titleFont.setColor(Color.WHITE);
        titleFont.draw(game.batch, "TẠM DỪNG", startX, centerY + 45, viewWidth, Align.center, false);

        // Kích thước mong muốn hiển thị của mũi tên (Ví dụ: pixel art 12x12 hoặc 16x16)
        float arrowWidth = 12f;
        float arrowHeight = 12f;

        // --- LỰA CHỌN: TIẾP TỤC (Dùng menuFont gốc) ---
        if (pauseSelectedIndex == 0) {
            game.batch.draw(arrowTexture, centerX - 55, centerY - 14, arrowWidth, arrowHeight);
        } else {
            menuFont.setColor(Color.WHITE);
        }
        menuFont.draw(game.batch, "Tiếp tục", startX, centerY +0 , viewWidth, Align.center, false);

        // --- LỰA CHỌN: THOÁT ---
        if (pauseSelectedIndex == 1) {
            game.batch.draw(arrowTexture, centerX - 45, centerY - 47, arrowWidth, arrowHeight);
        } else {
            menuFont.setColor(Color.WHITE);
        }
        menuFont.draw(game.batch, "Thoát", startX, centerY - 35, viewWidth, Align.center, false);

        float textX = startX;
        float textWidth = viewWidth - 15f;

        // Tọa độ Y: Góc dưới màn hình là (centerY - viewHeight / 2). Cộng thêm tầm 15px để nhấc chữ lên khỏi sát sàn.
        float textY = (centerY - viewHeight / 2f) + 15f;

        hintFont.draw(game.batch, "Bấm SPACE để lựa chọn", textX, textY, textWidth, Align.right, false);

        game.batch.end();
    }

    // Reset toàn bộ logic, biến ảo, trạng thái bản đồ và hòm đồ
    public void resetAllGameLogic() {
        // 1. Reset các biến tiến trình của PlayScreen
        refusedQueenEnding = false;
        rescuedPrisoner = false;
        waxCountdownStarted = false;
        waxCountdownTimer = 0f;
        pendingExitEndingType = "escape";
        currentPrompt = "";
        portalCooldown = 0.45f;
        guardCatchCooldown = 0f;
        state = GameState.RUNNING;

        // 2. Tẩy não Player (Mất hết đồ, mất mặt nạ, bỏ núp)
        myPlayer.hasMask = false;
        myPlayer.hasActivatedMask = false;
        myPlayer.hasMaskItem = false;
        myPlayer.hasKeyItem = false;
        myPlayer.currentKey = "";
        myPlayer.isBeeDisguised = false;
        myPlayer.isHidingAtStone = false;
        myPlayer.isCreeping = false;
        myPlayer.noiseRadius = 0f;

        // 3. Tắt hội thoại nếu đang nói dở
        if (dialogueManager != null) {
            dialogueManager.isShowing = false;
        }

        // 4. QUAN TRỌNG NHẤT: Bắt MapManager quên hết các cửa đã mở, câu đố đã giải
        if (game.map != null) {
            game.map.clearAllProgressState();
            game.map.clearPushableStateForCurrentMap();
        }
    }

    // Reset tất cả và chơi lại từ đầu ngay lập tức
    public void restartGameFromBeginning() {
        resetAllGameLogic(); // Gọi Hàm 1 dọn dẹp trước

        // Khôi phục nhạc nền về bài gốc
        AudioManager.getInstance().stopBackgroundMusic();
        // Có thể sửa tên file nhạc bên dưới theo tên nhạc khởi đầu của bạn
        AudioManager.getInstance().playBackgroundMusic("audio/Memories.ogg", 0.4f);

        // Load lại map khởi đầu
        game.map.loadMap("map/" + START_MAP);

        recreatePuzzleLibrary();
        spawnPlayer(null); // Sinh ra ở spawn mặc định

        // Cập nhật lại bóng tối và Camera
        game.map.updateFloorHide(myPlayer);
        camera.position.set(myPlayer.x, myPlayer.y, 0);
        updateCamera();

        // Gọi lại hội thoại mở đầu game
        if (storyManager != null && dialogueManager != null) {
            storyManager.checkNewGameIntro(dialogueManager);
            storyManager.checkMapEnterEvent("map/" + START_MAP, dialogueManager);
        }
    }

    // Reset tất cả và cút về FirstScreen
    public void returnToMainMenu() {
        resetAllGameLogic(); // Gọi Hàm 1 dọn dẹp trước khi đi

        // Tắt hết nhạc nhẽo của PlayScreen
        AudioManager.getInstance().stopBackgroundMusic();

        // Quăng người chơi ra ngoài FirstScreen
        game.setScreen(new FirstScreen(game));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        shapeRender.dispose();
        font.dispose();
        titleFont.dispose();
        menuFont.dispose();
        if (choiceMenuFont != null) choiceMenuFont.dispose();
        if (hintFont != null) hintFont.dispose();
        if (arrowTexture != null) arrowTexture.dispose();
    }
}
