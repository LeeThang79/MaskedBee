package game.maskedbee.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
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
import game.maskedbee.map.DialogueManager;
import game.maskedbee.map.PuzzleLibrary;
import game.maskedbee.map.PuzzleManager;
import game.maskedbee.map.StoryManager;
import game.maskedbee.objects.PushableBlock;


public class PlayScreen implements Screen {
    public final CORE game;
    public Player myPlayer;

    private static final String START_MAP = "Cocoon_Chamber.tmx";

    private OrthographicCamera camera;
    private Viewport viewport;

    public enum GameState {
        RUNNING, PAUSE, QUEEN_CHOICE, EXIT_CHOICE
    }

    private GameState state = GameState.RUNNING;

    private boolean refusedQueenEnding = false;
    private String pendingExitEndingType = "escape";

    private float portalCooldown = 0f;
    private float guardCatchCooldown = 0f;

    private ShapeRenderer shapeRender;
    private BitmapFont titleFont; // Dùng cho chữ TẠM DỪNG
    private BitmapFont menuFont; // Dùng cho các tùy chọn
    private BitmapFont hintFont; // Dùng cho dòng "Bấm Space để chọn"
    private BitmapFont font;
    private Rectangle continueBtn;
    private Rectangle quitBtn;

    private String currentPrompt = "";

    private PuzzleLibrary puzzleLibrary;
    private PuzzleManager puzzleManager;
    private StoryManager storyManager;
    private DialogueManager dialogueManager;

    private FreeTypeFontGenerator fontGenerator;

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

        // Cau hinh font moi
        this.fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("MaskedBee.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 13;
        parameter.color = com.badlogic.gdx.graphics.Color.WHITE;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS
            + "áàảãạăắằẳẵặâấầẩẫậéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵđĐ\n" +
            "ÁÀẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬÉÈẺẼẸÊẾỀỂỄỆÍÌỈĨỊÓÒỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢÚÙỦŨỤƯỨỪỬỮỰÝỲỶỸY";
        // 1. Tạo font cho Tiêu đề (Size lớn hơn, ví dụ: 18 hoặc 20 tùy bạn thấy vừa mắt)
        parameter.minFilter = Texture.TextureFilter.Nearest;
        parameter.magFilter = Texture.TextureFilter.Nearest;

        parameter.size = 28;
        this.titleFont = fontGenerator.generateFont(parameter);

        // 2. Tạo font cho Menu (Size nhỏ hơn, ví dụ: 12)
        parameter.size = 18;
        this.menuFont = fontGenerator.generateFont(parameter);

        parameter.size = 12;
        this.hintFont = fontGenerator.generateFont(parameter);

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
        puzzleLibrary = new PuzzleLibrary(game.map);
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

        if (state == GameState.RUNNING) {
            updateRunning(delta);
        } else if (state == GameState.PAUSE) {
            handlePauseMenuLogic();
        } else if (state == GameState.QUEEN_CHOICE) {
            handleQueenChoiceLogic();
        } else if (state == GameState.EXIT_CHOICE) {
            handleExitChoiceLogic();
        }

        drawGame();

        if (!currentPrompt.isEmpty() && state == GameState.RUNNING) {
            game.batch.setProjectionMatrix(camera.combined);
            game.batch.begin();
            menuFont.draw(game.batch, currentPrompt, myPlayer.x - 15, myPlayer.y + 45);
            game.batch.end();
        }

        if (dialogueManager != null && state == GameState.RUNNING) {
            dialogueManager.draw(game.batch, camera);
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
        if (portalCooldown > 0f) {
            portalCooldown -= delta;
        }

        if (guardCatchCooldown > 0f) {
            guardCatchCooldown -= delta;
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

    private void checkQueenInteraction() {
        if (!"Queen_Chamber.tmx".equalsIgnoreCase(game.map.getCurrentMapName())) {
            return;
        }

        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            return;
        }

        if (!myPlayer.hasMask) {
            System.out.println("Ban can co mat na truoc khi noi chuyen voi Queen.");
            return;
        }

        if (!myPlayer.hasActivatedMask) {
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
            boss.drawDebug(shapeRender);
        }

        shapeRender.end();
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

        menuFont.getData().setScale(0.7f);

        // Tọa độ gốc để vẽ chữ bên trong hộp thoại
        float textStartX = boxX + 20f;
        float textStartY = boxY + boxHeight - 10f;
        float arrowWidth = 14f;
        float arrowHeight = 14f;

        // --- VẼ CÂU HỎI ---
        menuFont.setColor(Color.WHITE);
        menuFont.draw(game.batch, "Bạn muốn thoát khỏi đây?", textStartX, textStartY, boxWidth - 40f, Align.left, true);

        // --- LỰA CHỌN 1: CÓ (Vẽ thấp xuống 22 pixel) ---
        float optionYesY = textStartY - 22f;
        if (exitSelectedIndex == 0) {
            // Vẽ mũi tên ngay trước chữ "Có"
            game.batch.draw(arrowTexture, textStartX, optionYesY - 11f, arrowWidth, arrowHeight);
        } else {
            menuFont.setColor(Color.WHITE);
        }
        // Dịch chữ sang phải một chút (20px) để nhường chỗ cho mũi tên
        menuFont.draw(game.batch, "Thoát", textStartX + 20f, optionYesY, boxWidth - 60f, Align.left, true);

        // --- LỰA CHỌN 2: KHÔNG (Vẽ thấp xuống tiếp 20 pixel) ---
        float optionNoY = optionYesY - 20f;
        if (exitSelectedIndex == 1) {
            // Vẽ mũi tên ngay trước chữ "Không"
            game.batch.draw(arrowTexture, textStartX, optionNoY - 11f, arrowWidth, arrowHeight);
        } else {
            menuFont.setColor(Color.WHITE);
        }
        menuFont.draw(game.batch, "Ở lại", textStartX + 20f, optionNoY, boxWidth - 60f, Align.left, true);

        // Trả lại scale mặc định của font tránh ảnh hưởng chỗ khác
        menuFont.getData().setScale(0.5f);
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

        menuFont.getData().setScale(0.7f);

        // Tọa độ gốc để vẽ chữ bên trong hộp thoại
        float textStartX = boxX + 20f;
        float textStartY = boxY + boxHeight - 10f;
        float arrowWidth = 14f;
        float arrowHeight = 14f;

        // --- VẼ CÂU HỎI ---
        menuFont.setColor(Color.WHITE);
        menuFont.draw(game.batch, "Trở thành ong chúa?", textStartX, textStartY, boxWidth - 40f, Align.left, true);

        // --- LỰA CHỌN 1: CÓ (Vẽ thấp xuống 22 pixel) ---
        float optionYesY = textStartY - 22f;
        if (queenSelectedIndex == 0) {
            // Vẽ mũi tên ngay trước chữ "Có"
            game.batch.draw(arrowTexture, textStartX, optionYesY - 11f, arrowWidth, arrowHeight);
        } else {
            menuFont.setColor(Color.WHITE);
        }
        // Dịch chữ sang phải một chút (20px) để nhường chỗ cho mũi tên
        menuFont.draw(game.batch, "Có", textStartX + 20f, optionYesY, boxWidth - 60f, Align.left, true);

        // --- LỰA CHỌN 2: KHÔNG (Vẽ thấp xuống tiếp 20 pixel) ---
        float optionNoY = optionYesY - 20f;
        if (queenSelectedIndex == 1) {
            // Vẽ mũi tên ngay trước chữ "Không"
            game.batch.draw(arrowTexture, textStartX, optionNoY - 11f, arrowWidth, arrowHeight);
        } else {
            menuFont.setColor(Color.WHITE);
        }
        menuFont.draw(game.batch, "Không", textStartX + 20f, optionNoY, boxWidth - 60f, Align.left, true);

        // Trả lại scale mặc định của font tránh ảnh hưởng chỗ khác
        menuFont.getData().setScale(0.5f);
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

        if (game.map.getPushables() != null) {
            for (PushableBlock block : game.map.getPushables()) {
                if (interactRange.overlaps(block.getBounds())) {
                    currentPrompt = "[G]";
                    return;
                }
            }
        }

        if (checkLayerOverlap("ExaminePoints", interactRange)) {
            currentPrompt = "[G]";
            return;
        }

        if (checkLayerOverlap("Doors", interactRange)) {
            currentPrompt = "[F]";
            return;
        }

        if (checkLayerOverlap("Queen_Interact", interactRange)
            || checkLayerOverlap("Chapel_Interact", interactRange)) {
            currentPrompt = "[E]";
            return;
        }

        for (int i = 0; i <= 5; i++) {
            if (checkLayerOverlap("Interact_Point_" + i, interactRange)) {
                currentPrompt = "[E]";
                return;
            }
        }

        if (checkPortalPromptOverlap(interactRange)) {
            currentPrompt = "[SPACE]";
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
                AudioManager.getInstance().stopBackgroundMusic();
                game.setScreen(new FirstScreen(game));
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
        menuFont.draw(game.batch, "Tiếp tục", startX, centerY + 0, viewWidth, Align.center, false);

        // --- LỰA CHỌN: THOÁT ---
        if (pauseSelectedIndex == 1) {
            game.batch.draw(arrowTexture, centerX - 45, centerY - 39, arrowWidth, arrowHeight);
        } else {
            menuFont.setColor(Color.WHITE);
        }
        menuFont.draw(game.batch, "Thoát", startX, centerY - 25, viewWidth, Align.center, false);

        float textX = startX;
        float textWidth = viewWidth - 15f;

        // Tọa độ Y: Góc dưới màn hình là (centerY - viewHeight / 2). Cộng thêm tầm 15px để nhấc chữ lên khỏi sát sàn.
        float textY = (centerY - viewHeight / 2f) + 15f;

        hintFont.draw(game.batch, "Bấm SPACE để lựa chọn", textX, textY, textWidth, Align.right, false);

        game.batch.end();
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
        if (hintFont != null) hintFont.dispose();
        fontGenerator.dispose();
        if (arrowTexture != null) arrowTexture.dispose();
    }
}
