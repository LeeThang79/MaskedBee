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
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import game.maskedbee.objects.Spike;
import game.maskedbee.objects.Lever;

public class MapManager {

    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    private final Array<Rectangle> wallCollision = new Array<>();
    private final Array<RectangleMapObject> doorObjects = new Array<>();
    private final Array<MapObject> portalObjects = new Array<>();

    // THÊM: Danh sách Gai và Cần gạt
    public final Array<Spike> spikes = new Array<>();
    public final Array<Lever> levers = new Array<>();

    private String currentMapName = "";
    private String lastMapName = "";

    // LOAD MAP
    public Array<Rectangle> getWallCollision() {
        return wallCollision;
    }
    public Array<Rectangle> getFullCollision() {
        Array<Rectangle> allHitboxes = new Array<>();

        // 1. Thêm tường cố định
        allHitboxes.addAll(wallCollision);

        // 2. Thêm các cánh cửa đang đóng
        for (RectangleMapObject door : doorObjects) {
            allHitboxes.add(door.getRectangle());
        }

        return allHitboxes;
    }

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
            spikes.clear(); // tai them
            levers.clear();

            for (MapLayer layer : map.getLayers()) {
                String layerName = layer.getName();

                //   XỬ LÝ VA CHẠM TƯỜNG
                if (layerName.contains("Collision")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            // Tách cửa ra khỏi tường
                            if (obj.getName() != null && obj.getName().contains("jail_door")) {
                                doorObjects.add((RectangleMapObject) obj);
                            } else {
                                wallCollision.add(((RectangleMapObject) obj).getRectangle());
                            }
                        }
                    }
                }
                //   XỬ LÝ VA CHẠM CỬA
                else if (layerName.equals("Doors")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            doorObjects.add((RectangleMapObject) obj);
                        }
                    }
                }
                //    XỬ LÝ PORTAL CHUYỂN MAP CỦA XUÂN
                else if (layerName.equals("Exit") || layerName.contains("_Chamber") || layerName.equals("Corridor")) {
                    for (MapObject obj : layer.getObjects()) {
                        portalObjects.add(obj);
                    }
                }
                // Quét tìm gai và cần gạt
                else if (layerName.equals("Spikes")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof TiledMapTileMapObject) {
                            spikes.add(new Spike((TiledMapTileMapObject) obj));
                        }
                    }
                }
                else if (layerName.equals("Switch")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof TiledMapTileMapObject) {
                            levers.add(new Lever((TiledMapTileMapObject) obj));
                        }
                    }
                }
            }

            System.out.println("✅ Loaded map: " + fileName);

        } catch (Exception e) {
            Gdx.app.error("MapManager", "❌ Error loading map: " + fileName, e);
        }
    }

    // GET SPAWN POINTS
    public Rectangle getSpawnPoint(String fromMap) {
        MapLayer spawnLayer = map.getLayers().get("SpawnPoints");
        if (spawnLayer == null || fromMap == null) return null;
        for (MapObject obj : spawnLayer.getObjects()) {
            if (fromMap.equals(obj.getName())) {
                if (obj instanceof RectangleMapObject) return ((RectangleMapObject) obj).getRectangle();
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
    // 1. Hàm vẽ lớp nền (Sàn, tường dưới, gai, cần gạt)
    public void renderBackground(OrthographicCamera camera) {
        if (renderer == null || map == null) return;
        renderer.setView(camera);

        // Vẽ các Tile Layer trước (Sàn, tường...)
        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) {
            // Bỏ qua lớp che đầu và các lớp Object
            if (layer.getName().equals("Overhead") || !(layer instanceof com.badlogic.gdx.maps.tiled.TiledMapTileLayer)) continue;
            renderer.renderTileLayer((com.badlogic.gdx.maps.tiled.TiledMapTileLayer) layer);
        }
        renderer.getBatch().end();

        // Vẽ các Object (Gai, Cần gạt)
        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) {
            if (layer.getName().equals("Spikes") || layer.getName().equals("Switch") || layer.getName().equals("Door")) {
                renderObjectLayer(layer);
            }
        }
        renderer.getBatch().end();
    }

    // 2. Hàm vẽ lớp che đầu (Thanh sắt lồng)
    public void renderForeground(OrthographicCamera camera) {
        if (renderer == null || map == null) return;
        MapLayer overhead = map.getLayers().get("Overhead");
        if (overhead != null && overhead instanceof com.badlogic.gdx.maps.tiled.TiledMapTileLayer) {
            renderer.getBatch().begin();
            renderer.renderTileLayer((com.badlogic.gdx.maps.tiled.TiledMapTileLayer) overhead);
            renderer.getBatch().end();
        }
    }
    // Hàm phụ để vẽ Object (Copy từ code cũ của Xuân sang)
    private void renderObjectLayer(MapLayer layer) {
        for (MapObject obj : layer.getObjects()) {
            if (obj.isVisible() && obj instanceof TiledMapTileMapObject) {
                TiledMapTileMapObject tileObj = (TiledMapTileMapObject) obj;
                renderer.getBatch().draw(
                    tileObj.getTile().getTextureRegion(),
                    tileObj.getX(), tileObj.getY() - 32,
                    tileObj.getOriginX(), tileObj.getOriginY(),
                    tileObj.getTextureRegion().getRegionWidth(), tileObj.getTextureRegion().getRegionHeight(),
                    tileObj.getScaleX(), tileObj.getScaleY(), tileObj.getRotation()
                );
            }
        }
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
        // Xóa vùng va chạm
        for (int i = doorObjects.size - 1; i >= 0; i--) {
            String objName = doorObjects.get(i).getName();
            if (objName != null && objName.contains(doorName)) {
                doorObjects.removeIndex(i);
            }
        }
        // Xóa hình ảnh cái cửa
        MapLayer visualDoorLayer = map.getLayers().get("Door");
        if (visualDoorLayer != null) {
            for (MapObject obj : visualDoorLayer.getObjects()) {
                if (doorName.equals(obj.getName())) {
                    obj.setVisible(false); // Ra lệnh "Tàng hình" cực kỳ an toàn
                    System.out.println("✅ Đã ẩn hình ảnh cửa: " + doorName);
                }
            }
        }
        System.out.println("🚪 TỔNG KẾT: Door opened: " + doorName);
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
    public float getMapWidth() {
        int width = map.getProperties().get("width", Integer.class);
        int tileWidth = map.getProperties().get("tilewidth", Integer.class);
        return width * tileWidth;
    }

    public float getMapHeight() {
        int height = map.getProperties().get("height", Integer.class);
        int tileHeight = map.getProperties().get("tileheight", Integer.class);
        return height * tileHeight;
    }
    // =========================
    // DISPOSE
    // =========================
    public void dispose() {
        if (map != null) map.dispose();
        if (renderer != null) renderer.dispose();
    }
}
