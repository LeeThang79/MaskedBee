package game.maskedbee.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.objects.PointMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class MapManager {

    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    private final Array<Rectangle> wallCollision = new Array<>();
    private final Array<RectangleMapObject> doorObjects = new Array<>();
    private final Array<MapObject> portalObjects = new Array<>();

    private String currentMapName = "";
    private String lastMapName = "";

    // =========================
    // LOAD MAP
    // =========================
    public Array<Rectangle> getWallCollision() {
        return wallCollision;
    }// code test nen can xoa

    public void loadMap(String fileName) {
        try {
            lastMapName = currentMapName;
            currentMapName = fileName.replace("map/", "");

            if (map != null) map.dispose();
            if (renderer != null) renderer.dispose();

            map = new TmxMapLoader().load(fileName);
            renderer = new OrthogonalTiledMapRenderer(map);

            wallCollision.clear();
            doorObjects.clear();
            portalObjects.clear();

            for (MapLayer layer : map.getLayers()) {
                String layerName = layer.getName();

                if (layerName.contains("Collision")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            wallCollision.add(((RectangleMapObject) obj).getRectangle());
                        }
                    }
                } else if (layerName.equals("Doors")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            doorObjects.add((RectangleMapObject) obj);
                        }
                    }
                } else if (layerName.equals("Exit") || layerName.contains("_Chamber") || layerName.equals("Corridor")) {
                    for (MapObject obj : layer.getObjects()) {
                        portalObjects.add(obj);
                    }
                }
            }

            System.out.println("✅ Loaded map: " + fileName);

        } catch (Exception e) {
            Gdx.app.error("MapManager", "❌ Error loading map: " + fileName, e);
        }
    }

    // điểm xuất hiện của player
    public Rectangle getSpawnPoint(String fromMap) {
        MapLayer spawnLayer = map.getLayers().get("SpawnPoints");

        if (spawnLayer == null || fromMap == null) return null;

        for (MapObject obj : spawnLayer.getObjects()) {
            if (fromMap.equals(obj.getName())) {

                if (obj instanceof RectangleMapObject) {
                    return ((RectangleMapObject) obj).getRectangle();
                }

                if (obj instanceof PointMapObject) {
                    float x = ((PointMapObject) obj).getPoint().x;
                    float y = ((PointMapObject) obj).getPoint().y;
                    return new Rectangle(x, y, 32, 32);
                }
            }
        }

        return null;
    }
    public Rectangle getPlayerSpawn() {
        if (map == null) return null;

        MapLayer layer = map.getLayers().get("Player_spawn");
        if (layer == null) return null;

        for (MapObject obj : layer.getObjects()) {
            if ("player_spawn".equals(obj.getName())) {

                if (obj instanceof RectangleMapObject) {
                    return ((RectangleMapObject) obj).getRectangle();
                }

                if (obj instanceof PointMapObject) {
                    float x = ((PointMapObject) obj).getPoint().x;
                    float y = ((PointMapObject) obj).getPoint().y;
                    return new Rectangle(x, y, 32, 32);
                }
            }
        }

        return null;
    }
    // =========================
    // RENDER MAP
    // =========================
    public void render(OrthographicCamera camera) {
        if (renderer == null) return;
        renderer.setView(camera);
        renderer.render();
        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) {
            // Chỉ quét các lớp Object (như cái màu tím trong ảnh của bạn)
            if (layer != null && !(layer instanceof com.badlogic.gdx.maps.tiled.TiledMapTileLayer)) {
                for (MapObject obj : layer.getObjects()) {
                    // Kiểm tra xem Object đó có chứa hình ảnh từ Tileset không (TiledMapTileMapObject)
                    if (obj instanceof com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject) {
                        com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject tileObj = (com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject) obj;

                        // Vẽ hình ảnh tại đúng tọa độ x, y, giữ nguyên kích thước scale từ Tiled
                        renderer.getBatch().draw(
                            tileObj.getTile().getTextureRegion(),
                            tileObj.getX(),
                            tileObj.getY(),
                            tileObj.getOriginX(),
                            tileObj.getOriginY(),
                            tileObj.getTextureRegion().getRegionWidth(),
                            tileObj.getTextureRegion().getRegionHeight(),
                            tileObj.getScaleX(),
                            tileObj.getScaleY(),
                            tileObj.getRotation()
                        );
                    }
                }
            }
        }
        renderer.getBatch().end();
    }

    // =========================
    // COLLISION
    // =========================
    public boolean isColliding(Rectangle entityRect) {
        for (Rectangle wall : wallCollision) {
            if (entityRect.overlaps(wall)) return true;
        }

        for (RectangleMapObject door : doorObjects) {
            if (entityRect.overlaps(door.getRectangle())) return true;
        }

        return false;
    }

    // =========================
    // PORTAL (CHUYỂN MAP)
    // =========================
    public String checkPortal(Rectangle entityRect) {
        for (MapObject portal : portalObjects) {
            if (portal instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) portal).getRectangle();

                if (entityRect.overlaps(rect)) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                        || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {

                        String destination = portal.getName();
                        if (destination != null && destination.endsWith(".tmx")) {
                            // Thêm "map/" vì bạn để file trong assets/map/
                            return "map/" + destination;
                        }
                    }
                }
            }
        }
        return null;
    }

    // =========================
    // DOOR
    // =========================
    public void openDoor(String doorName) {
        for (int i = 0; i < doorObjects.size; i++) {
            if (doorObjects.get(i).getName().equals(doorName)) {
                doorObjects.removeIndex(i);
                System.out.println("🚪 Door opened: " + doorName);
                return;
            }
        }
    }

    // =========================
    // GETTER
    // =========================
    public String getCurrentMapName() {
        return currentMapName;
    }

    public String getLastMapName() {
        return lastMapName;
    }

    public TiledMap getMap() {
        return map;
    }

    // =========================
    // DISPOSE
    // =========================
    public void dispose() {
        if (map != null) map.dispose();
        if (renderer != null) renderer.dispose();
    }
}
