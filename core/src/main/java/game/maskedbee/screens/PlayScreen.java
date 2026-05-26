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

import game.maskedbee.entities.Guard;
import game.maskedbee.entities.Player;
import game.maskedbee.main.CORE;
import game.maskedbee.map.PuzzleLibrary;
import game.maskedbee.map.PuzzleManager;
import game.maskedbee.objects.Lever;
import game.maskedbee.objects.PushableBlock;
import game.maskedbee.objects.Spike;

public class PlayScreen implements Screen {
    public final CORE game;
    public Player myPlayer;

    private OrthographicCamera camera;
    private Viewport viewport;

    public enum GameState {
        RUNNING, PAUSE
    }

    private GameState state = GameState.RUNNING;

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

    // Hàm tiện ích: Đưa người chơi về điểm Spawn trên Map
    private void spawnPlayer(String fromMap) {
        Rectangle spawn = game.map.getSpawnPoint(fromMap);

        if (spawn == null) {
            spawn = game.map.getPlayerSpawn();
        }

        if (spawn != null) {
            myPlayer.x = spawn.x;
            myPlayer.y = spawn.y;
            myPlayer.hitbox.setPosition(spawn.x, spawn.y);
        } else {
            myPlayer.x = 100;
            myPlayer.y = 100;
            myPlayer.hitbox.setPosition(100, 100);
            System.out.println("⚠️ Cảnh báo: Không tìm thấy điểm Spawn nào trên Map!");
        }
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
        puzzleLibrary = new PuzzleLibrary(
            game.map.getMap(),
            game.map.getWallCollision(),
            game.map.getInteractPoints()
        );
    }

    private void reloadCurrentMapAndRespawn() {
        game.map.loadMap("map/" + game.map.getCurrentMapName());
        recreatePuzzleLibrary();
        spawnPlayer(null);
        updateCamera();
    }

    private void handleInteractions() {
        // LOGIC PUZZLE CŨ: GẠT CẦN (Nhấn E)
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            for (Lever lever : game.map.levers) {
                if (myPlayer.hitbox.overlaps(lever.hitbox)) {
                    lever.toggle(game.map.getMap());

                    if ("lever".equals(lever.type)) {
                        for (Spike spike : game.map.spikes) {
                            if ((lever.targetColor != null && lever.targetColor.equals(spike.type))
                                || "black".equals(spike.type)) {
                                spike.toggle(game.map.getMap());
                            }
                        }
                    } else if ("door_lever".equals(lever.type)) {
                        game.map.openDoor(lever.targetName);
                    }
                    break;
                }
            }
        }

        // LOGIC PUZZLE CŨ: CHẾT KHI ĐẠP GAI
        for (Spike spike : game.map.spikes) {
            if (spike.isUp && myPlayer.hitbox.overlaps(spike.hitbox)) {
                System.out.println("💀 Đạp trúng gai! Reset level!");
                reloadCurrentMapAndRespawn();
                break;
            }
        }
    }

    @Override
    public void show() {
        game.map.loadMap("map/cocoon_chamber.tmx");
        recreatePuzzleLibrary();
        spawnPlayer(null);
        camera.position.set(myPlayer.x, myPlayer.y, 0);
        updateCamera();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            state = (state == GameState.RUNNING) ? GameState.PAUSE : GameState.RUNNING;
        }

        if (state == GameState.RUNNING) {
            updateRunning(delta);
        } else if (state == GameState.PAUSE) {
            handlePauseMenuLogic();
        }

        drawGame();

        if (state == GameState.PAUSE) {
            drawPauseMenu();
        }
    }

    private void updateRunning(float delta) {
        if (puzzleLibrary != null) {
            puzzleLibrary.update(myPlayer.hitbox, delta);
        }

        // Puzzle mới: keys, doors, pushable blocks, floor hide, v.v.
        if (puzzleManager != null) {
            puzzleManager.update(myPlayer, game.map);
        }

        // Puzzle cũ: lever/spike/door_lever
        handleInteractions();

        // Check spike death từ PuzzleManager mới nếu có logic mới ở đó
        if (puzzleManager != null && puzzleManager.checkSpikeDeath(myPlayer, game.map)) {
            reloadCurrentMapAndRespawn();
            return;
        }

        // Portal chuyển map
        String nextMap = game.map.checkPortal(myPlayer.hitbox);
        if (nextMap != null) {
            String lastMap = game.map.getCurrentMapName();
            game.map.loadMap(nextMap);
            recreatePuzzleLibrary();
            spawnPlayer(lastMap);
            updateCamera();
            return;
        }

        // Reset puzzle bằng R
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            for (PushableBlock block : game.map.getPushables()) {
                block.resetPosition();
            }
            spawnPlayer(game.map.getLastMapName());
            updateCamera();
        }

        // Cập nhật Player.
        // Dùng getFullCollision() để giữ collision tường + cửa + pushable.
        // Nếu Player của bạn đã có hàm update(delta, walls, pushables), bạn có thể đổi dòng này thành:
        // myPlayer.update(delta, game.map.getWallCollision(), game.map.getPushables());
        myPlayer.update(delta, game.map.getWallCollision());

        game.map.updateFloorHide(myPlayer);

        // Cập nhật guard. Không nhận boolean để tương thích với Guard hiện tại của bạn.
        for (Guard guard : game.map.guards) {
            guard.update(delta, myPlayer, game.map.getWallCollision());
        }

        updateCamera();
    }

    private void drawGame() {
        ScreenUtils.clear(0, 0, 0, 1);

        // GIỮ LOGIC RENDER CŨ:
        // background -> entities -> overhead, có sortY để xử lý vật thể che Player.
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

        myPlayer.draw(game.batch);

        for (PushableBlock block : game.map.getPushables()) {
            block.render(game.batch);
        }

        for (Guard guard : game.map.guards) {
            guard.draw(game.batch);
        }

        game.batch.end();

        shapeRender.setProjectionMatrix(camera.combined);
        shapeRender.begin(ShapeRenderer.ShapeType.Line);
        for (Guard guard : game.map.guards) {
            guard.drawDebug(shapeRender, myPlayer);
        }
        shapeRender.end();
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
