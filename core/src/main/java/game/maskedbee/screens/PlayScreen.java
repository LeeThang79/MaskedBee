package game.maskedbee.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.maskedbee.entities.Player;
import game.maskedbee.main.CORE;

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

    public PlayScreen(CORE game) {
        this.game = game;
        this.camera = new OrthographicCamera();

        this.viewport = new FitViewport(352, 256, camera);
        this.myPlayer = new Player(0f, 0f);

        this.shapeRender = new ShapeRenderer();
        this.font = new BitmapFont();

        continueBtn = new Rectangle(0,0,100,30);
        quitBtn = new Rectangle(0,0,100,30);
    }

    @Override
    public void show() {
        game.map.loadMap("map/cocoon_chamber.tmx");

        Rectangle spawn = game.map.getPlayerSpawn();
        if(spawn != null) {
            myPlayer.x = spawn.x;
            myPlayer.y = spawn.y;
            myPlayer.hitbox.setPosition(spawn.x, spawn.y);
        }
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            state = (state == GameState.RUNNING) ? GameState.PAUSE : GameState.RUNNING;
        }

        if (state == GameState.RUNNING) {
            this.myPlayer.update(delta, game.map.getWallCollision());
            camera.position.set(myPlayer.x, myPlayer.y, 0);
            camera.update();

            // 3. Xử lý chuyển Map (Portal)
            String nextMap = game.map.checkPortal(myPlayer.hitbox);
            if (nextMap != null) {
                game.map.loadMap(nextMap);
                Rectangle spawn = game.map.getPlayerSpawn();
                if(spawn != null) {
                    myPlayer.x = spawn.x;
                    myPlayer.y = spawn.y;
                    myPlayer.hitbox.setPosition(spawn.x, spawn.y);
                }
                camera.position.set(myPlayer.x, myPlayer.y, 0);
                return;
            }

        } else if (state == GameState.PAUSE) {
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

        // Vẽ mọi thứ ra màn hình
        ScreenUtils.clear(0, 0, 0, 1);

        game.map.render(camera); // Vẽ map của Xuân
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        myPlayer.draw(game.batch);
        game.batch.end();

        // Vẽ lớp phủ khi Pause
        if (state == GameState.PAUSE) {
            drawPauseOverlay();
        }
    }

    // Tách riêng hàm vẽ Pause cho code của Thắng gọn gàng hơn
    private void drawPauseOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRender.setProjectionMatrix(camera.combined);
        shapeRender.begin(ShapeRenderer.ShapeType.Filled);
        // Vẽ lớp phủ màn hình màu đen, độ mờ 60%
        shapeRender.setColor(0, 0, 0, 0.6f);
        shapeRender.rect(camera.position.x - viewport.getWorldWidth()/2, camera.position.y - viewport.getWorldHeight()/2, viewport.getWorldWidth(), viewport.getWorldHeight());
        // Vẽ màu nền cho 2 nút bấm (Màu xám đậm)
        shapeRender.setColor(Color.DARK_GRAY);
        shapeRender.rect(continueBtn.x, continueBtn.y, continueBtn.width, continueBtn.height);
        shapeRender.rect(quitBtn.x, quitBtn.y, quitBtn.width, quitBtn.height);
        shapeRender.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        // Vẽ chữ lên trên nút bấm
        game.batch.begin();
        // Căn chỉnh chữ thủ công cho vào giữa nút
        font.draw(game.batch, "Continue", continueBtn.x + 20, continueBtn.y + 20);
        font.draw(game.batch, "Quit", quitBtn.x + 35, quitBtn.y + 20);
        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override
    public void dispose() {
        shapeRender.dispose();
        font.dispose();
    }
}
