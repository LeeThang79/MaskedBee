package game.maskedbee.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.maps.objects.RectangleMapObject;


import game.maskedbee.entities.Boss;
import game.maskedbee.entities.Guard;
import game.maskedbee.entities.Player;
import game.maskedbee.main.CORE;
import game.maskedbee.map.PuzzleLibrary;
import game.maskedbee.map.PuzzleManager;
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

    // escape: có mặt nạ và rời khỏi hive
    private String pendingExitEndingType = "escape";
    // Chống lỗi vừa spawn xong đã bị portal kéo sang map khác hoặc guard bắt lại ngay.
    private float portalCooldown = 0f;
    private float guardCatchCooldown = 0f;

    private ShapeRenderer shapeRender;
    private BitmapFont font;
    private Rectangle continueBtn;
    private Rectangle quitBtn;

    private PuzzleLibrary puzzleLibrary;
    private PuzzleManager puzzleManager;

    public PlayScreen(CORE game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(515, 290, camera);

        this.myPlayer = new Player(0, 0);

        this.shapeRender = new ShapeRenderer();
        this.font = new BitmapFont();

        continueBtn = new Rectangle(0, 0, 100, 30);
        quitBtn = new Rectangle(0, 0, 100, 30);

        puzzleManager = new PuzzleManager();
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
        // Chết do gai/reset level: gai và cần gạt về trạng thái ban đầu của map.
        game.map.clearSpikeLeverStateForCurrentMap();

        game.map.loadMap("map/" + game.map.getCurrentMapName());
        recreatePuzzleLibrary();
        spawnPlayer(null);
        game.map.updateFloorHide(myPlayer);
        updateCamera();
    }

    @Override
    public void show() {
        game.map.loadMap("map/" + START_MAP);
        recreatePuzzleLibrary();
        spawnPlayer(null);
        game.map.updateFloorHide(myPlayer);
        camera.position.set(myPlayer.x, myPlayer.y, 0);
        updateCamera();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (state == GameState.RUNNING) {
                state = GameState.PAUSE;
            } else if (state == GameState.PAUSE) {
                state = GameState.RUNNING;
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
    private void handleCaughtByGuard() {
        System.out.println("Caught by guard!");

        state = GameState.RUNNING;
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
            return;
        }

        System.out.println("No solved puzzle checkpoint. Restart from first map.");
        resetPlayerProgressToStart();

        game.map.loadMap("map/" + START_MAP);
        recreatePuzzleLibrary();
        spawnPlayer(null);
        game.map.updateFloorHide(myPlayer);
        updateCamera();
    }
    private void resetPlayerProgressToStart() {
        refusedQueenEnding = false;

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

    private boolean isFinalExitPortal(String currentMap, String nextMap) {
        if (currentMap == null || nextMap == null) return false;

        String lowerNextMap = nextMap.toLowerCase();

        if ("Exit_Chamber.tmx".equalsIgnoreCase(currentMap)
            && lowerNextMap.endsWith("exit.tmx")) {
            return true;
        }

        return "Queen_Chamber.tmx".equalsIgnoreCase(currentMap)
            && lowerNextMap.endsWith("exit.tmx")
            && !lowerNextMap.contains("exit_chamber");
    }

    private String getExitEndingType() {
        // Chưa có mặt nạ mà đi tới Exit cuối
        if (!myPlayer.hasMask) {
            return "no_mask";
        }
        // Có mặt nạ nhưng chưa kích hoạt ở Old Chapel
        if (!myPlayer.hasActivatedMask) {
            return "inactive_mask";
        }

        // Có mặt nạ và đã từ chối Queen
        if (refusedQueenEnding) {
            return "escape";
        }

        // Có mặt nạ nhưng bỏ đi mà không nhận lời Queen
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
                System.out.println("Queen choice opened");
                return;
            }
        }
    }
    private void handleExitChoiceLogic() {
        // 1: tiếp tục chơi
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) {

            continueFromExitChoice();
            return;
        }
        // 2: ra ending tương ứng
        // - chưa có mặt nạ -> no_mask
        // - có mặt nạ -> escape
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) {

            goToEnding(pendingExitEndingType);
        }
    }
    private void continueFromExitChoice() {
        state = GameState.RUNNING;
        portalCooldown = 0.7f;
        System.out.println("Continue playing");
    }

    private void goToEnding(String endingType) {
        System.out.println("Ending type: " + endingType);
        game.setScreen(new EndingScreen(game, endingType));
    }
    private void handleQueenChoiceLogic() {
        // 1: chấp nhận thành Queen
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) {

            goToEnding("queen");
            return;
        }
        // 2: từ chối, quay lại game
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) {

            refuseQueenChoice();
        }
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
            game.map.renderBackground(camera);
            game.map.renderForeground(camera);
            drawEntities();
        } else {
            game.map.renderBackground(camera);
            drawEntities();
            game.map.renderForeground(camera);
        }
    }

    private void drawEntities() {
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        for (PushableBlock block : game.map.getPushables()) {
            if (block.getBounds().y > myPlayer.y) {
                block.render(game.batch);
            }
        }

        myPlayer.draw(game.batch);

        for (PushableBlock block : game.map.getPushables()) {
            // nếu block ở phía dưới player
            if (block.getBounds().y <= myPlayer.y) {
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
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRender.setProjectionMatrix(camera.combined);
        shapeRender.begin(ShapeRenderer.ShapeType.Filled);

        shapeRender.setColor(0f, 0f, 0f, 0.78f);
        shapeRender.rect(
            camera.position.x - 185,
            camera.position.y - 70,
            370,
            140
        );

        shapeRender.setColor(Color.DARK_GRAY);
        shapeRender.rect(camera.position.x - 150, camera.position.y - 15, 300, 28);
        shapeRender.rect(camera.position.x - 150, camera.position.y - 52, 300, 28);

        shapeRender.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        font.draw(
            game.batch,
            "Leave the hive?",
            camera.position.x - 70,
            camera.position.y + 42
        );

        font.draw(
            game.batch,
            "1. Continue",
            camera.position.x - 115,
            camera.position.y + 4
        );

        font.draw(
            game.batch,
            "2. Exit",
            camera.position.x - 115,
            camera.position.y - 33
        );
        game.batch.end();
    }
    private void drawQueenChoiceMenu() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRender.setProjectionMatrix(camera.combined);
        shapeRender.begin(ShapeRenderer.ShapeType.Filled);

        // Nền bảng
        shapeRender.setColor(0f, 0f, 0f, 0.78f);
        shapeRender.rect(
            camera.position.x - 190,
            camera.position.y - 75,
            380,
            150
        );

        // Ô lựa chọn
        shapeRender.setColor(Color.DARK_GRAY);
        shapeRender.rect(camera.position.x - 155, camera.position.y - 20, 310, 28);
        shapeRender.rect(camera.position.x - 155, camera.position.y - 58, 310, 28);

        shapeRender.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        font.draw(
            game.batch,
            "Queen: Become the new Queen?",
            camera.position.x - 145,
            camera.position.y + 45
        );

        font.draw(
            game.batch,
            "1. Accept",
            camera.position.x - 120,
            camera.position.y
        );

        font.draw(
            game.batch,
            "2. Refuse",
            camera.position.x - 120,
            camera.position.y - 38
        );

        game.batch.end();
    }

    private void handlePauseMenuLogic() {
        float centerX = camera.position.x;
        float centerY = camera.position.y;

        continueBtn.setPosition(centerX - continueBtn.width / 2, centerY + 10);
        quitBtn.setPosition(centerX - quitBtn.width / 2, centerY - 30);

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Vector3 touchPoint = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPoint);

            if (continueBtn.contains(touchPoint.x, touchPoint.y)) {
                state = GameState.RUNNING;
            } else if (quitBtn.contains(touchPoint.x, touchPoint.y)) {
                game.setScreen(new FirstScreen(game));
            }
        }
    }

    private void drawPauseMenu() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRender.setProjectionMatrix(camera.combined);
        shapeRender.begin(ShapeRenderer.ShapeType.Filled);

        shapeRender.setColor(0, 0, 0, 0.6f);
        shapeRender.rect(
            camera.position.x - viewport.getWorldWidth() / 2,
            camera.position.y - viewport.getWorldHeight() / 2,
            viewport.getWorldWidth(),
            viewport.getWorldHeight()
        );

        shapeRender.setColor(Color.DARK_GRAY);
        shapeRender.rect(continueBtn.x, continueBtn.y, continueBtn.width, continueBtn.height);
        shapeRender.rect(quitBtn.x, quitBtn.y, quitBtn.width, quitBtn.height);

        shapeRender.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        font.draw(game.batch, "Continue", continueBtn.x + 20, continueBtn.y + 20);
        font.draw(game.batch, "Quit", quitBtn.x + 35, quitBtn.y + 20);
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
    }
}
