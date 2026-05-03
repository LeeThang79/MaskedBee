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
import game.maskedbee.map.MapManager;
import game.maskedbee.objects.Spike;
import game.maskedbee.objects.Lever;

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

        this.myPlayer = new Player(0,0);

        this.shapeRender = new ShapeRenderer();
        this.font = new BitmapFont();

        continueBtn = new Rectangle(0,0,100,30);
        quitBtn = new Rectangle(0,0,100,30);
    }
    // Hàm tiện ích: Đưa người chơi về điểm Spawn trên Map
    private void spawnPlayer() {
        Rectangle spawn = game.map.getPlayerSpawn();
        if(spawn != null) {
            myPlayer.x = spawn.x;
            myPlayer.y = spawn.y;
            myPlayer.hitbox.setPosition(spawn.x, spawn.y);
        }
    }

    @Override
    public void show() {
        game.map.loadMap("map/holding_chamber.tmx");
        spawnPlayer();
        camera.position.set(myPlayer.x, myPlayer.y, 0);
        camera.update();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            state = (state == GameState.RUNNING) ? GameState.PAUSE : GameState.RUNNING;
        }

        if (state == GameState.RUNNING) {
            // Cập nhật người chơi (Truyền danh sách tường vào để không đi xuyên tường)
            myPlayer.update(delta, game.map.getWallCollision());
            // LOGIC PUZZLE: GẠT CẦN (Nhấn phím E)
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                for (Lever lever : game.map.levers) {
                    if (myPlayer.hitbox.overlaps(lever.hitbox)) {

                        lever.toggle(game.map.getMap()); // Lật hình cái cần gạt

                        if ("lever".equals(lever.type)) {
                            // Nếu là cần gạt đinh: Đảo trạng thái gai cùng màu và gai đen
                            for (Spike spike : game.map.spikes) {
                                if (lever.targetColor != null && lever.targetColor.equals(spike.type) || "black".equals(spike.type)) {
                                    spike.toggle(game.map.getMap());
                                }
                            }
                        }
                        else if ("door_lever".equals(lever.type)) {
                            // Nếu là cần mở cửa: Gọi hàm mở cửa
                            game.map.openDoor(lever.targetName);
                        }
                        break;
                    }
                }
            }
            // LOGIC PUZZLE: CHẾT KHI ĐẠP TRÚNG GAI
            // ------------------------------------------
            for (Spike spike : game.map.spikes) {
                if (spike.isUp && myPlayer.hitbox.overlaps(spike.hitbox)) {
                    System.out.println("💀 Dap trung gai! Reset level!");
                    game.map.loadMap("map/" + game.map.getCurrentMapName());
                    spawnPlayer();
                    break; // Thoát vòng lặp để tránh lỗi khi reset map
                }
            }
            // CẬP NHẬT CAMERA & CHUYỂN MAP
            // ------------------------------------------
            camera.position.set(myPlayer.x, myPlayer.y, 0);
            camera.update();

            String nextMap = game.map.checkPortal(myPlayer.hitbox);
            if (nextMap != null) {
                game.map.loadMap(nextMap);
                spawnPlayer();
                camera.position.set(myPlayer.x, myPlayer.y, 0);
                return; // Thoát render vòng này để vẽ map mới
            }


        }// TRẠNG THÁI: GAME ĐANG TẠM DỪNG
        else if (state == GameState.PAUSE) {
            // Cập nhật tọa độ nút bấm theo vị trí hiện tại của camera
            float centerX = camera.position.x;
            float centerY = camera.position.y;

            continueBtn.setPosition(centerX - continueBtn.width / 2, centerY + 10);
            quitBtn.setPosition(centerX - quitBtn.width / 2, centerY - 30);

            // Xử lý click chuột vào nút
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                // Lấy tọa độ chuột trên màn hình máy tính (Pixel)
                Vector3 touchPoint = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                // Dịch tọa độ đó sang không gian 2D của game
                camera.unproject(touchPoint);

                // Kiểm tra xem tọa độ chuột có nằm gọn trong nút không
                if (continueBtn.contains(touchPoint.x, touchPoint.y)) {
                    state = GameState.RUNNING; // Tiếp tục game
                } else if (quitBtn.contains(touchPoint.x, touchPoint.y)) {
                    game.setScreen(new FirstScreen(game)); // Chuyển về màn hình đầu
                }
            }
        }

        // RENDER (VẼ LÊN MÀN HÌNH)
        ScreenUtils.clear(0, 0, 0, 1);
        game.map.render(camera); //Vẽ map
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        myPlayer.draw(game.batch);
        game.batch.end();
        // Vẽ Menu Pause đè lên trên
        if (state == GameState.PAUSE) {
            // Bật blend để vẽ nền đen trong suốt
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRender.setProjectionMatrix(camera.combined);
            shapeRender.begin(ShapeRenderer.ShapeType.Filled);

            // Vẽ lớp phủ màn hình màu đen, độ mờ 60%
            shapeRender.setColor(0, 0, 0, 0.6f);
            shapeRender.rect(
                camera.position.x - viewport.getWorldWidth() / 2,
                camera.position.y - viewport.getWorldHeight() / 2,
                viewport.getWorldWidth(),
                viewport.getWorldHeight()
            );

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
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }
    @Override
    public void pause() {}
    @Override
    public void resume() {}
    @Override
    public void hide() {}
    @Override
    public void dispose() {
        shapeRender.dispose();
        font.dispose();
    }
}
