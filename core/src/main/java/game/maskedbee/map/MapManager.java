package game.maskedbee.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.objects.PointMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.math.Vector2;
import game.maskedbee.entities.Guard;

import game.maskedbee.entities.Player;
import game.maskedbee.objects.Spike;
import game.maskedbee.objects.Lever;
import game.maskedbee.objects.Door;
import game.maskedbee.objects.Key;
import game.maskedbee.objects.PushableBlock;

public class MapManager {

    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    private final Array<Rectangle> wallCollision = new Array<>();
    private final Array<RectangleMapObject> doorObjects = new Array<>();
    private final Array<MapObject> portalObjects = new Array<>();
    private final Array<RectangleMapObject> interactPoints = new Array<>();

    // PushableBlock
    private final Array<PushableBlock> pushables = new Array<>();
    public Array<PushableBlock> getPushables() {
        return pushables;
    }
    private Rectangle hideTrigger;
    public Rectangle getHideTrigger() {
        return hideTrigger;
    }
    public void resetPushables() {
        for (PushableBlock block : pushables) {
            block.resetPosition();
        }
    }
    // Keys
    private final Array<Key> keys = new Array<>();
    public Array<Key> getKeys() {
        return keys;
    }
    //door
    private final Array<Door> doors = new Array<>();
    private TiledMapTileLayer doorCloseLayer;
    private TiledMapTileLayer doorOpenLayer;
    // THÊM: Danh sách Gai và Cần gạt
    public final Array<Spike> spikes = new Array<>();
    public final Array<Lever> levers = new Array<>();

    //Thêm Guards
    public final Array<Guard> guards = new Array<>();

    private String currentMapName = "";
    private String lastMapName = "";

    // LOAD MAP
    public Array<Rectangle> getWallCollision() {
        return wallCollision;
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
            pushables.clear();
            keys.clear();
            doors.clear();
            doorObjects.clear();
            portalObjects.clear();
            spikes.clear(); // tai them
            levers.clear();
            guards.clear();

            interactPoints.clear();
            doorCloseLayer = (TiledMapTileLayer) map.getLayers().get("Door_Close");
            doorOpenLayer = (TiledMapTileLayer) map.getLayers().get("Door_Open");

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
                            RectangleMapObject rectObj = (RectangleMapObject) obj;
                            // collision
                            doorObjects.add(rectObj);
                            // logic door
                            doors.add(new Door(rectObj));
                        }
                    }
                }
                // PushableBlock
                else if (layerName.equals("Pushable")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                            // FIX tọa độ (QUAN TRỌNG)
                            rect = new Rectangle(
                                (float) Math.floor(rect.x / 32) * 32,
                                (float) Math.floor(rect.y / 32) * 32,
                                32,
                                32
                            );
                            pushables.add(new PushableBlock(rect));
                        }
                    }
                }
                else if (layerName.equals("Hide_Trigger")) {

                    for (MapObject obj : layer.getObjects()) {

                        if (obj instanceof RectangleMapObject) {

                            hideTrigger =
                                ((RectangleMapObject) obj).getRectangle();
                        }
                    }
                }
                // KEY
                else if (layerName.equals("Keys")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            keys.add(new Key((RectangleMapObject) obj));
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

                //Quái
                else if (layerName.equals("Guards")) {
                    for (MapObject obj : layer.getObjects()) {
                        Array<Vector2> path = new Array<>();

                        // TRƯỜNG HỢP 1: Nếu bạn vẽ bằng công cụ Polyline (Đường zíc zắc/Đường thẳng hở)
                        if (obj instanceof PolylineMapObject) {
                            Polyline polyline = ((PolylineMapObject) obj).getPolyline();
                            float[] vertices = polyline.getTransformedVertices();
                            for (int i = 0; i < vertices.length; i += 2) {
                                path.add(new Vector2(vertices[i], vertices[i + 1]));
                            }
                        }
                        // TRƯỜNG HỢP 2: Nếu bạn vẽ bằng công cụ Polygon (Hình đa giác, vuông khép kín)
                        else if (obj instanceof PolygonMapObject) {
                            com.badlogic.gdx.math.Polygon polygon = ((PolygonMapObject) obj).getPolygon();
                            float[] vertices = polygon.getTransformedVertices();
                            for (int i = 0; i < vertices.length; i += 2) {
                                path.add(new Vector2(vertices[i], vertices[i + 1]));
                            }
                        }

                        // Sinh ra Guard nếu đọc được tọa độ
                        if (path.size > 0) {
                            float startX = path.get(0).x - 16;
                            float startY = path.get(0).y - 20;
                            guards.add(new Guard(startX, startY, path));
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
        MapLayer layer = map.getLayers().get("Player_Spawn");
        if (layer == null) return null;
        for (MapObject obj : layer.getObjects()) {
            if ("Player_Spawn".equals(obj.getName())) {
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
            // BỎ QUA layer đang invisible
            if (layer == null || !layer.isVisible()) {
                continue;
            }
            // Chỉ quét các lớp Object
            if (layer != null && !(layer instanceof com.badlogic.gdx.maps.tiled.TiledMapTileLayer)) {
                for (MapObject obj : layer.getObjects()) {
                    // Kiểm tra xem Object đó có chứa hình ảnh từ Tileset không (TiledMapTileMapObject)
                    if (obj.isVisible() && obj instanceof com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject) {
                        com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject tileObj = (com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject) obj;

                        // Vẽ hình ảnh tại đúng tọa độ x, y, giữ nguyên kích thước scale từ Tiled
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
    //pushable
    public PushableBlock getCollidingPushable(Rectangle rect) {
        for (PushableBlock block : pushables) {
            if (rect.overlaps(block.getBounds())) {
                return block;
            }
        }
        return null;
    }

    // =========================
    // PORTAL (CHUYỂN MAP)
    // =========================
    public String checkPortal(Rectangle entityRect) {
        for (MapObject portal : portalObjects) {
            if (portal instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) portal).getRectangle();

                if (entityRect.overlaps(rect)) {
                    boolean blockedByDoor = false;
                    for (RectangleMapObject door : doorObjects) {
                        if (rect.overlaps(door.getRectangle())) {
                            blockedByDoor = true;
                            break;
                        }
                    }
                    // Nếu portal đang bị cửa đóng chặn
                    if (blockedByDoor) {
                        return null;
                    }

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
        MapLayer visualDoorLayer = map.getLayers().get("Doors");
        if (visualDoorLayer != null) {
            for (MapObject obj : visualDoorLayer.getObjects()) {
                if (doorName.equals(obj.getName())) {
                    obj.setVisible(false); // Ra lệnh "Tàng hình" cực kỳ an toàn
                    System.out.println("✅ Đã ẩn hình ảnh cửa: " + doorName);
                }
            }
            //  XỬ LÝ TILE DOOR (Door_Close / Door_Open)
            for (Door door : doors) {

                if (door.getName().equals(doorName)) {

                    door.open();

                    Rectangle b = door.getBounds();

                    int tileX = Math.round(b.x / 32f);
                    int tileY = Math.round(b.y / 32f);
                    // TẮT TILE CỬA ĐÓNG
                    if (doorCloseLayer != null) {
                        for (int i = 0; i < 2; i++) {
                            doorCloseLayer.setCell(tileX, tileY + i, null);
                        }
                    }
                    // BẬT TILE CỬA MỞ
                    if (doorOpenLayer != null) {
                        doorOpenLayer.setVisible(true);
                        for (int i = 0; i < 2; i++) {
                            TiledMapTileLayer.Cell cell =
                                doorOpenLayer.getCell(tileX, tileY + i);
                            if (cell != null) {
                                doorOpenLayer.setCell(tileX, tileY + i, cell);
                            }
                        }
                    }
                    System.out.println("🚪 Tile door opened: " + doorName);
                    break;
                }
            }
        }
        System.out.println("🚪 TỔNG KẾT: Door opened: " + doorName);
    }
    // GET DOORS
    public Array<Door> getDoors() {
        return doors;
    }
    public void updateFloorHide(Player player) {
        MapLayer hideLayer = map.getLayers().get("Floor_Hide");
        if (hideLayer == null || hideTrigger == null) return;
        boolean triggered = false;
        // Player đứng lên trigger
        float playerCenterX = player.hitbox.x + player.hitbox.width / 2f;

        float playerCenterY = player.hitbox.y + player.hitbox.height / 2f;

        if (hideTrigger.contains(playerCenterX, playerCenterY)) {
            triggered = true;
        }
        // Block đứng lên trigger
        for (PushableBlock block : pushables) {
            Rectangle b = block.getBounds();
            float blockCenterX = b.x + b.width / 2f;
            float blockCenterY = b.y + b.height / 2f;
            if (hideTrigger.contains(blockCenterX, blockCenterY)) {
                triggered = true;
                break;
            }
        }

        hideLayer.setVisible(!triggered);
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
