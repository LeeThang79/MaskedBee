package game.maskedbee.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PointMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen implements Screen {
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;

    private final Array<Rectangle> wallCollision = new Array<>();
    private final Array<RectangleMapObject> doorObjects = new Array<>();
    private final Array<MapObject> portalObjects = new Array<>();

    private TestPlayer testPlayer;
    private String currentMapName = "";
    private String lastMapName = "";
    private Viewport viewport;

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.zoom = 0.7f; // Số càng nhỏ thì nhìn càng gần (phóng to map)
        viewport = new FitViewport(400, 300, camera);
        camera.setToOrtho(false, 400, 300);
        shapeRenderer = new ShapeRenderer();
        testPlayer = new TestPlayer(110, 80);
        loadMap("holding_chamber.tmx");
    }

    public void loadMap(String fileName) {
        try {
            lastMapName = currentMapName;
            currentMapName = fileName;

            if (map != null) map.dispose();
            if (renderer != null) renderer.dispose();

            map = new TmxMapLoader().load(fileName);
            renderer = new OrthogonalTiledMapRenderer(map);

            wallCollision.clear();
            doorObjects.clear();
            portalObjects.clear();

            for (int i = 0; i < map.getLayers().getCount(); i++) {
                String layerName = map.getLayers().get(i).getName();

                if (layerName.contains("Collision")) {
                    for (MapObject obj : map.getLayers().get(i).getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            wallCollision.add(((RectangleMapObject) obj).getRectangle());
                        }
                    }
                } else if (layerName.equals("Doors")) {
                    for (MapObject obj : map.getLayers().get(i).getObjects()) {
                        if (obj instanceof RectangleMapObject) doorObjects.add((RectangleMapObject) obj);
                    }
                } else if (layerName.equals("Exit") || layerName.contains("_Chamber") || layerName.equals("Corridor")) {
                    for (MapObject obj : map.getLayers().get(i).getObjects()) {
                        portalObjects.add(obj);
                    }
                }
            }

            // --- XỬ LÝ SPAWN POINT  ---
            boolean spawnFound = false;
            MapLayer spawnLayer = map.getLayers().get("SpawnPoints");

            if (spawnLayer != null && !lastMapName.isEmpty()) {
                for (MapObject spawn : spawnLayer.getObjects()) {
                    if (lastMapName.equals(spawn.getName())) {
                        float sx = 0, sy = 0;

                        if (spawn instanceof PointMapObject) {
                            sx = ((PointMapObject) spawn).getPoint().x;
                            sy = ((PointMapObject) spawn).getPoint().y;
                        } else if (spawn instanceof RectangleMapObject) {
                            Rectangle r = ((RectangleMapObject) spawn).getRectangle();
                            sx = r.x;
                            sy = r.y;
                        } else {
                            continue;
                        }

                        testPlayer.x = sx;
                        testPlayer.y = sy;
                        testPlayer.bounds.setPosition(sx, sy);
                        spawnFound = true;
                        System.out.println("🚀 Đã đón nhân vật tại điểm: " + lastMapName);
                        break;
                    }
                }
            }

            if (!spawnFound && testPlayer != null) {
                testPlayer.x = 110;
                testPlayer.y = 80;
                testPlayer.bounds.setPosition(110, 80);
            }

            System.out.println("✅ Loaded: " + fileName + (lastMapName.isEmpty() ? "" : " | Từ: " + lastMapName));
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "❌ Lỗi load map: " + fileName, e);
        }
    }

    public boolean isColliding(Rectangle entityRect) {
        for (Rectangle wall : wallCollision) {
            if (entityRect.overlaps(wall)) return true;
        }
        for (RectangleMapObject door : doorObjects) {
            if (entityRect.overlaps(door.getRectangle())) return true;
        }
        return false;
    }

    public void updateGameplayLogic(Rectangle playerRect) {
        for (MapObject portal : portalObjects) {
            if (portal instanceof RectangleMapObject) {
                Rectangle portalRect = ((RectangleMapObject) portal).getRectangle();

                if (playerRect.overlaps(portalRect)) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                        String destination = portal.getName();
                        if (destination != null && destination.endsWith(".tmx")) {
                            loadMap(destination);
                            return;
                        }
                    }
                }
            }
        }
    }

    public void openDoor(String doorName) {
        for (int i = 0; i < doorObjects.size; i++) {
            if (doorObjects.get(i).getName().equals(doorName)) {
                doorObjects.removeIndex(i);
                System.out.println("🚪 Cửa " + doorName + " đã mở!");
            }
        }
    }

    @Override
    public void render(float delta) {
        // 1. Xóa màn hình
        ScreenUtils.clear(0, 0, 0, 1);

        // 2. Cập nhật logic di chuyển & va chạm
        testPlayer.update(delta, this);

        // 3. Cập nhật Camera bám theo Player
        camera.position.set(testPlayer.x, testPlayer.y, 0);
        camera.update();

        // 4. Vẽ MAP
        renderer.setView(camera);
        renderer.render();

        // 5. VẼ TEST PLAYER (Khối vuông đỏ)
        // QUAN TRỌNG: Phải nạp ma trận của camera vào shapeRenderer
        shapeRenderer.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(com.badlogic.gdx.graphics.Color.RED);
        shapeRenderer.rect(testPlayer.x, testPlayer.y, testPlayer.bounds.width, testPlayer.bounds.height);
        shapeRenderer.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void dispose() {
        if (map != null) map.dispose();
        if (renderer != null) renderer.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
