package game.maskedbee.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.maskedbee.entities.Guard;
import game.maskedbee.entities.Player;
import game.maskedbee.main.CORE;
import game.maskedbee.objects.Door;
import game.maskedbee.objects.Key;
import game.maskedbee.objects.PushableBlock;
import game.maskedbee.objects.Spike;
import game.maskedbee.objects.Lever;
import game.maskedbee.map.PuzzleLibrary;

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

    public PlayScreen(CORE game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(515, 290, camera);

        this.myPlayer = new Player(0,0);

        this.shapeRender = new ShapeRenderer();
        this.font = new BitmapFont();

        continueBtn = new Rectangle(0,0,100,30);
        quitBtn = new Rectangle(0,0,100,30);
    }
    // Hàm tiện ích: Đưa người chơi về điểm Spawn trên Map
    private void spawnPlayer(String fromMap) {
        Rectangle spawn = game.map.getSpawnPoint(fromMap);

        if(spawn==null) {
            spawn=game.map.getPlayerSpawn();
        }
        if(spawn != null) {
            myPlayer.x = spawn.x;
            myPlayer.y = spawn.y;
            myPlayer.hitbox.setPosition(spawn.x, spawn.y);
        }
    }

    private void updateCamera() {
        float mapWidth = game.map.getMapWidth();
        float mapHeight = game.map.getMapHeight();

        float halfViewportWidth = viewport.getWorldWidth() / 2f;
        float halfViewportHeight = viewport.getWorldHeight() / 2f;

        // Camera sẽ đi theo Player
        float camX = myPlayer.x;
        float camY = myPlayer.y;

        // KIỂM TRA CHIỀU NGANG: Nếu map nhỏ hơn khung nhìn -> Cố định ở giữa map
        if (mapWidth <= viewport.getWorldWidth()) {
            camX = mapWidth / 2f;
        } else {
            // Nếu map to -> Giới hạn không cho camera lộ ra vùng đen bên ngoài map
            camX = com.badlogic.gdx.math.MathUtils.clamp(camX, halfViewportWidth, mapWidth - halfViewportWidth);
        }

        // KIỂM TRA CHIỀU DỌC: Tương tự như chiều ngang
        if (mapHeight <= viewport.getWorldHeight()) {
            camY = mapHeight / 2f;
        } else {
            camY = com.badlogic.gdx.math.MathUtils.clamp(camY, halfViewportHeight, mapHeight - halfViewportHeight);
        }

        camera.position.set(camX, camY, 0);
        camera.update();
    }

    private void handelInteractions() {
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
                spawnPlayer(null);
                break; // Thoát vòng lặp để tránh lỗi khi reset map
            }
        }

    }
    // ======================================
    // OPEN DOOR WITH KEY
    // ======================================
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
        for (Door door : game.map.getDoors()) {
            Rectangle player = myPlayer.hitbox;
            Rectangle doorRect = door.getBounds();
            float distanceX = Math.abs(player.x - doorRect.x);
            float distanceY = Math.abs(player.y - doorRect.y);
            // Đứng gần cửa
            if (distanceX <= 40 && distanceY <= 40) {
                System.out.println("Player key = " + myPlayer.currentKey);
                System.out.println("Door needs = " + door.getRequiredKeyName());
                if (door.canOpen(myPlayer.currentKey)) {
                    game.map.openDoor(door.getName());
                    System.out.println("🚪 Opened: " + door.getName());
                } else {
                    System.out.println("❌ Need key: " + door.getRequiredKeyName());
                }
                break;
            }
        }
    }
}

private void checkKeyPickup() {
    for (Key key : game.map.getKeys()) {
        if (!key.isCollected() && myPlayer.hitbox.overlaps(key.getBounds())) {
            key.collect();
            myPlayer.currentKey = key.getName();
            System.out.println("🔑 Picked key: " + myPlayer.currentKey);
        }
    }
}
private void handlePushables() {

    float pushDistance = 32f;

    float dx = 0;
    float dy = 0;

    // Hướng đẩy
    if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
        || Gdx.input.isKeyJustPressed(Input.Keys.A)) {

        dx = -pushDistance;
    }

    else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
        || Gdx.input.isKeyJustPressed(Input.Keys.D)) {

        dx = pushDistance;
    }

    else if (Gdx.input.isKeyJustPressed(Input.Keys.UP)
        || Gdx.input.isKeyJustPressed(Input.Keys.W)) {

        dy = pushDistance;
    }

    else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)
        || Gdx.input.isKeyJustPressed(Input.Keys.S)) {

        dy = -pushDistance;
    }

    if (dx == 0 && dy == 0) return;

    for (PushableBlock block : game.map.getPushables()) {

        Rectangle b = block.getBounds();
        Rectangle p = myPlayer.hitbox;

        boolean touching = false;

        // =========================
        // CHECK THEO HƯỚNG
        // =========================

        // PUSH RIGHT
        float tolerance = 2f;

        // PUSH RIGHT
        if (dx > 0) {

            touching =
                p.x < b.x &&
                    (p.x + p.width) >= b.x - tolerance &&
                    Math.abs(p.y - b.y) < 12;
        }

        // PUSH LEFT
        else if (dx < 0) {

            touching =
                p.x > b.x &&
                    p.x <= (b.x + b.width) + tolerance &&
                    Math.abs(p.y - b.y) < 12;
        }

        // PUSH UP
        else if (dy > 0) {

            touching =
                p.y < b.y &&
                    (p.y + p.height) >= b.y - tolerance &&
                    Math.abs(p.x - b.x) < 12;
        }

        // PUSH DOWN
        else if (dy < 0) {

            touching =
                p.y > b.y &&
                    p.y <= (b.y + b.height) + tolerance &&
                    Math.abs(p.x - b.x) < 12;
        }

        // Không đứng đúng cạnh block
        if (!touching) continue;

        // =========================
        // BLOCK TƯƠNG LAI
        // =========================

        Rectangle future = new Rectangle(
            b.x + dx,
            b.y + dy,
            b.width,
            b.height
        );

        boolean blocked = false;

        // WALL
        for (Rectangle wall : game.map.getFullCollision()) {

            if (future.overlaps(wall)) {
                blocked = true;
                break;
            }
        }

        // BLOCK KHÁC
        for (PushableBlock other : game.map.getPushables()) {

            if (other != block &&
                future.overlaps(other.getBounds())) {

                blocked = true;
                break;
            }
        }

        // PUSH
        if (!blocked) {

            b.x += dx;
            b.y += dy;
        }

        break;
    }
}
    @Override
    public void show() {
        game.map.loadMap("map/cocoon_chamber.tmx");
        puzzleLibrary = new PuzzleLibrary(game.map.getMap(), game.map.getWallCollision());
        spawnPlayer(null);
        camera.position.set(myPlayer.x, myPlayer.y, 0);
        camera.update();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            state = (state == GameState.RUNNING) ? GameState.PAUSE : GameState.RUNNING;
        }

        if (state == GameState.RUNNING) {
            puzzleLibrary.update(myPlayer.hitbox, delta);
            //PUZZLE
            handelInteractions();
            checkKeyPickup();

            //Kiểm tra Portal
            String nextMap = game.map.checkPortal(myPlayer.hitbox);
            if (nextMap != null) {
                String lastMap = game.map.getCurrentMapName();
                game.map.loadMap(nextMap);
                puzzleLibrary = new PuzzleLibrary(game.map.getMap(), game.map.getWallCollision());
                spawnPlayer(lastMap);
                updateCamera();
                return; // Thoát render vòng này để vẽ map mới
            }
            // RESET PUZZLE
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                // reset block
                for (PushableBlock block : game.map.getPushables()) {
                    block.resetPosition();
                }
                // reset player
                spawnPlayer(game.map.getLastMapName());
                updateCamera();
            }
            // PUSH BLOCK
            handlePushables();

            // Cập nhật người chơi (Truyền danh sách tường vào để không đi xuyên tường)
            myPlayer.update(delta, game.map.getWallCollision());
            game.map.updateFloorHide(myPlayer);

            // Cập nhật tất cả quái vật trên bản đồ hiện tại
            for (Guard guard : game.map.guards) {
                guard.update(delta, myPlayer, game.map.getWallCollision());
            }

            //Camera
            updateCamera();
        }

        // TRẠNG THÁI: GAME ĐANG TẠM DỪNG
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

        // Vẽ block trước player
        for (PushableBlock block : game.map.getPushables()) {
            block.render(game.batch);
        }
        // Vẽ player
        myPlayer.draw(game.batch);
        // Vẽ quái vật
        for (Guard guard : game.map.guards) {
            guard.draw(game.batch);
        }
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
    public void show() {}
    @Override
    public void resize(int width, int height) {}
    @Override
    public void pause() {}
    @Override
    public void resume() {}
    @Override
    public void hide() {}
    @Override
    public void dispose() {
        if (background != null) background.dispose();
    }
}
