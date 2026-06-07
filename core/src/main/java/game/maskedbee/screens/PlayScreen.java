package game.maskedbee.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.maskedbee.entities.Boss;
import game.maskedbee.entities.Guard;
import game.maskedbee.entities.Player;
import game.maskedbee.main.CORE;
import game.maskedbee.main.AudioManager;
import game.maskedbee.main.GameFlowManager;
import game.maskedbee.main.NotificationManager;
import game.maskedbee.map.DialogueManager;
import game.maskedbee.map.PuzzleLibrary;
import game.maskedbee.map.PuzzleManager;
import game.maskedbee.map.StoryManager;
import game.maskedbee.objects.PushableBlock;
import game.maskedbee.ui.ChoiceMenu;
import game.maskedbee.ui.HudRenderer;
import game.maskedbee.ui.InteractionPrompt;
import game.maskedbee.ui.PauseMenu;

public class PlayScreen implements Screen {
    public final CORE game;
    public Player myPlayer;
    private PauseMenu pauseMenu;
    private ChoiceMenu queenChoiceMenu;
    private ChoiceMenu exitChoiceMenu;
    private InteractionPrompt interactionPromptUI;
    private HudRenderer hudRenderer;
    public GameFlowManager flowManager;
    private PuzzleLibrary puzzleLibrary;
    private PuzzleManager puzzleManager;
    private StoryManager storyManager;
    private DialogueManager dialogueManager;

    private static final String START_MAP = "Cocoon_Chamber.tmx";

    private OrthographicCamera camera;
    private Viewport viewport;
    public OrthographicCamera getCamera() { return camera; }
    public Viewport getViewport() { return viewport; }

    public void resumeGame() {
        state = GameState.RUNNING;
        AudioManager.getInstance().resumeBackgroundMusic();
    }
    public enum GameState {
        RUNNING, PAUSE, QUEEN_CHOICE, EXIT_CHOICE, TUTORIAL
    }
    private GameState state = GameState.RUNNING;

    private static boolean hasSeenTutorial = false;

    private float portalCooldown = 0f;
    private float guardCatchCooldown = 0f;

    private ShapeRenderer shapeRender;
    private BitmapFont titleFont;       // Dùng cho chữ TẠM DỪNG
    private BitmapFont menuFont;        // Dùng cho các tùy chọn
    private BitmapFont choiceMenuFont;  // Dung cho Queen/Exit
    private BitmapFont hintFont;        // Dùng cho dòng "Bấm Space để chọn"
    private BitmapFont font;

    public PlayScreen(CORE game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(515, 290, camera);

        this.myPlayer = new Player(0, 0);

        this.shapeRender = new ShapeRenderer();

        // 1. Tạo font cho Tiêu đề (Size to nhất)
        this.titleFont = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        this.titleFont.getData().setScale(1.0f);

        // 2. Tạo font cho Menu (Size vừa)
        this.menuFont = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        this.menuFont.getData().setScale(0.7f);

        // 3. Chữ đếm ngược (Size nhỏ hơn xíu)
        this.font = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        this.font.getData().setScale(0.6f);

        // 4. Menu Lựa chọn trong hộp thoại (Queen/Exit)
        this.choiceMenuFont = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        this.choiceMenuFont.getData().setScale(0.5f);

        // 5. Chữ hướng dẫn "Bấm space để chọn"
        this.hintFont = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        this.hintFont.getData().setScale(0.45f);

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
        game.map.state.clearSpikeLeverStateForCurrentMap();

        game.map.loadMap("map/" + game.map.getCurrentMapName());
        recreatePuzzleLibrary();
        spawnPlayer(null);
        game.map.updateFloorHide(myPlayer);
        updateCamera();
    }

    @Override
    public void show() {
        pauseMenu = new PauseMenu(this);
        hudRenderer = new HudRenderer(this);
        flowManager = new GameFlowManager(this);

        game.map.loadMap("map/" + START_MAP);
        recreatePuzzleLibrary();
        spawnPlayer(null);
        game.map.updateFloorHide(myPlayer);
        camera.position.set(myPlayer.x, myPlayer.y, 0);
        updateCamera();

        if (storyManager != null && dialogueManager != null) {
            storyManager.checkNewGameIntro(dialogueManager); // KÍCH HOẠT THOẠI MỞ ĐẦU GAME
            storyManager.checkMapEnterEvent("map/" + START_MAP, dialogueManager);
        }

        if (!hasSeenTutorial) {
            state = GameState.TUTORIAL;
        }

        queenChoiceMenu = new ChoiceMenu(this, "Trở thành ong chúa?", "Có", "Không");
        exitChoiceMenu = new ChoiceMenu(this, "Bạn muốn thoát khỏi đây?", "Thoát", "Ở lại");
        interactionPromptUI = new InteractionPrompt(this);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (state == GameState.RUNNING) {
                state = GameState.PAUSE;
                // TẠM DỪNG NHẠC NỀN KHI PAUSE
                AudioManager.getInstance().pauseBackgroundMusic();
            } else if (state == GameState.PAUSE) {
                resumeGame();
            }
        }

        if (state == GameState.TUTORIAL) {
            handleTutorialLogic();
        }else if (state == GameState.RUNNING) {
            updateRunning(delta);
        } else if (state == GameState.PAUSE) {
            pauseMenu.update();
            pauseMenu.draw(shapeRender, titleFont, menuFont, hintFont);
        } else if (state == GameState.QUEEN_CHOICE) {
            int selected = queenChoiceMenu.update();
            if (selected == 0) {
                flowManager.goToEnding("queen");
            } else if (selected == 1) {
                flowManager.refuseQueenChoice();
            }
        } else if (state == GameState.EXIT_CHOICE) {
            int selected = exitChoiceMenu.update();
            if (selected == 0) {
                flowManager.goToEnding(flowManager.getPendingExitEndingType());
            } else if (selected == 1) {
                continueFromExitChoice();
            }
        }

        drawGame();

        //VẼ THÔNG BÁO NỔI (NOTIFICATION)
        if (state == GameState.RUNNING) {
            hudRenderer.drawNotification(delta, shapeRender, hintFont);
            interactionPromptUI.draw(hintFont);

            if (flowManager.isWaxCountdownStarted()) {
                hudRenderer.drawWaxCountdownTimer(flowManager.getWaxCountdownTimer(), font);
            }

            if (dialogueManager != null) {
                dialogueManager.draw(game.batch, camera);
            }
        }


        if (state == GameState.RUNNING) {
            interactionPromptUI.draw(hintFont);
        }

        if (flowManager.isWaxCountdownStarted() && state == GameState.RUNNING) {
            hudRenderer.drawWaxCountdownTimer(flowManager.getWaxCountdownTimer(), font);
        }

        if (dialogueManager != null && state == GameState.RUNNING) {
            dialogueManager.draw(game.batch, camera);
        }

        if (state == GameState.TUTORIAL) {
            hudRenderer.drawTutorial(shapeRender, font, hintFont);
        }

        if (state == GameState.PAUSE) {
            pauseMenu.draw(shapeRender, titleFont, menuFont, hintFont);
        }

        if (state == GameState.QUEEN_CHOICE) {
            queenChoiceMenu.draw(shapeRender, choiceMenuFont);
        }

        if (state == GameState.EXIT_CHOICE) {
            exitChoiceMenu.draw(shapeRender, choiceMenuFont);
        }
    }

    private void updateRunning(float delta) {
        // ========================================================
        // 🛠️ DEBUG / CHEAT KEYS (Dành riêng cho Dev test game)
        // Nhớ xóa hoặc comment đoạn này lại trước khi nộp bài/xuất game!
        // ========================================================
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) { flowManager.goToEnding("queen"); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) { flowManager.goToEnding("escape"); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) { flowManager.goToEnding("boss"); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) { flowManager.goToEnding("no_mask"); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) { flowManager.goToEnding("lab_escape"); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) { flowManager.goToEnding("lab_explosion"); return; }
        // ========================================================
        // ========================================================



        if (portalCooldown > 0f) {
            portalCooldown -= delta;
        }

        if (guardCatchCooldown > 0f) {
            guardCatchCooldown -= delta;
        }
        flowManager.update(delta);

        if (dialogueManager != null) {
            //Kiểm tra xem thoại có đang bật không
            boolean wasShowing = dialogueManager.isShowing;

            dialogueManager.update(delta);

            //Nếu có thoại thì khỏi di chuyển đi
            if (wasShowing) {
                interactionPromptUI.clear(); // Ẩn luôn gợi ý phím khi thoại đang mở
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

        interactionPromptUI.update();

        if (puzzleLibrary != null) {
            puzzleLibrary.update(myPlayer.hitbox, delta);
        }
        // PuzzleManager xử lý: E gạt cần/gai/cửa, F mở cửa bằng key, nhặt key, đẩy block.
        if (puzzleManager != null) {
            puzzleManager.update(myPlayer, game.map);
        }

        flowManager.handleStoryInteractions();

        if (puzzleManager != null && puzzleManager.checkSpikeDeath(myPlayer, game.map)) {
            reloadCurrentMapAndRespawn();
            return;
        }

        if (portalCooldown <= 0f) {
            String nextMap = game.map.checkPortal(myPlayer.hitbox);

            if (nextMap != null) {
                String currentMap = game.map.getCurrentMapName();
                if (flowManager.isFinalExitPortal(currentMap, nextMap)) {
                    if (flowManager.isWaxCountdownStarted()) {
                        flowManager.stopWaxCountdown();
                        flowManager.goToEnding("lab_escape");
                        return;
                    }

                    flowManager.setPendingExitEndingType(flowManager.getExitEndingType());
                    state = GameState.EXIT_CHOICE;
                    portalCooldown = 0.7f;
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
                flowManager.goToEnding("boss");
                return;
            }
        }

        updateCamera();
    }

    private void continueFromExitChoice() {
        state = GameState.RUNNING;
        portalCooldown = 0.7f;
        System.out.println("Continue playing");
    }

    private void handleCaughtByGuard() {
        NotificationManager.getInstance().show("Bạn đã bị Quái bắt");
        System.out.println("Caught by guard!");

        state = GameState.RUNNING;
        interactionPromptUI.clear();

        myPlayer.isBeeDisguised = false;
        myPlayer.isHidingAtStone = false;
        myPlayer.noiseRadius = 0f;

        portalCooldown = 0.45f;
        guardCatchCooldown = 0f;

        if (game.map.state.hasProgressCheckpoint()) {
            String checkpointMap = game.map.state.getProgressCheckpointMapName();

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
        interactionPromptUI.clear();
        myPlayer.hasMask = false;
        myPlayer.hasActivatedMask = false;
        myPlayer.hasMaskItem = false;
        myPlayer.hasKeyItem = false;
        myPlayer.currentKey = "";
        myPlayer.isBeeDisguised = false;
        myPlayer.isHidingAtStone = false;
        myPlayer.noiseRadius = 0f;
        if (flowManager != null) flowManager.resetProgress();
        game.map.state.clearAllProgressState();
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

    // Reset toàn bộ logic, biến ảo, trạng thái bản đồ và hòm đồ
    public void resetAllGameLogic() {
        // 1. Reset các biến tiến trình của PlayScreen
        interactionPromptUI.clear();
        portalCooldown = 0.45f;
        guardCatchCooldown = 0f;
        state = GameState.RUNNING;
        if (flowManager != null) flowManager.resetProgress();

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

    // Reset tất cả và cút về FirstScreen
    public void returnToMainMenu() {
        resetAllGameLogic(); // Gọi Hàm 1 dọn dẹp trước khi đi

        // Tắt hết nhạc nhẽo của PlayScreen
        AudioManager.getInstance().stopBackgroundMusic();

        // Quăng người chơi ra ngoài FirstScreen
        game.setScreen(new FirstScreen(game));
    }

    public void setPortalCooldown(float cooldown) {
        this.portalCooldown = cooldown;
    }

    public void openQueenChoice() {
        state = GameState.QUEEN_CHOICE;
        interactionPromptUI.clear();
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
        if (queenChoiceMenu != null) queenChoiceMenu.dispose();
        if (exitChoiceMenu != null) exitChoiceMenu.dispose();
    }
}
