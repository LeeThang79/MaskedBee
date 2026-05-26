package game.maskedbee.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PointMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import game.maskedbee.entities.Guard;
import game.maskedbee.entities.Player;
import game.maskedbee.objects.Door;
import game.maskedbee.objects.Key;
import game.maskedbee.objects.Lever;
import game.maskedbee.objects.PushableBlock;
import game.maskedbee.objects.Spike;

public class MapManager {

    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    private final Array<Rectangle> wallCollision = new Array<>();
    private final Array<RectangleMapObject> doorObjects = new Array<>();
    private final Array<MapObject> portalObjects = new Array<>();
    private final Array<RectangleMapObject> interactPoints = new Array<>();

    // Puzzle mới
    private final Array<PushableBlock> pushables = new Array<>();
    private final Array<Key> keys = new Array<>();
    private final Array<Door> doors = new Array<>();
    private Rectangle hideTrigger;
    private TiledMapTileLayer doorCloseLayer;
    private TiledMapTileLayer doorOpenLayer;

    // Puzzle cũ
    public final Array<Spike> spikes = new Array<>();
    public final Array<Lever> levers = new Array<>();

    // Guards
    public final Array<Guard> guards = new Array<>();

    private String currentMapName = "";
    private String lastMapName = "";

    // =========================
    // LOAD MAP
    // =========================
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
            interactPoints.clear();
            pushables.clear();
            keys.clear();
            doors.clear();
            spikes.clear();
            levers.clear();
            guards.clear();
            hideTrigger = null;

            doorCloseLayer = getTileLayerSafe("Door_Close");
            doorOpenLayer = getTileLayerSafe("Door_Open");

            for (MapLayer layer : map.getLayers()) {
                if (layer == null) continue;
                String layerName = layer.getName();

                // Collision: Wall_Collision, Stone_Collision, Hidden_Room_Collision, v.v.
                if (layerName.contains("Collision")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            if (obj.getName() != null && obj.getName().contains("jail_door")) {
                                doorObjects.add((RectangleMapObject) obj);
                            } else {
                                wallCollision.add(((RectangleMapObject) obj).getRectangle());
                            }
                        }
                    }
                }
                // Doors object/collision
                else if (layerName.equals("Doors")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            RectangleMapObject rectObj = (RectangleMapObject) obj;
                            doorObjects.add(rectObj);
                            doors.add(new Door(rectObj));
                        }
                    }
                }
                // Pushable blocks
                else if (layerName.equals("Pushable")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                            rect = new Rectangle(
                                (float) Math.floor(rect.x / 32f) * 32f,
                                (float) Math.floor(rect.y / 32f) * 32f,
                                32f,
                                32f
                            );
                            pushables.add(new PushableBlock(rect));
                        }
                    }
                }
                // Floor hide trigger
                else if (layerName.equals("Hide_Trigger")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            hideTrigger = ((RectangleMapObject) obj).getRectangle();
                        }
                    }
                }
                // Keys
                else if (layerName.equals("Keys")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            keys.add(new Key((RectangleMapObject) obj));
                        }
                    }
                }
                // Portals
                else if (layerName.equals("Exit") || layerName.contains("_Chamber") || layerName.equals("Corridor")) {
                    for (MapObject obj : layer.getObjects()) {
                        portalObjects.add(obj);
                    }
                }
                // Spikes
                else if (layerName.equals("Spikes")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof TiledMapTileMapObject) {
                            spikes.add(new Spike((TiledMapTileMapObject) obj));
                        }
                    }
                }
                // Levers / switches
                else if (layerName.equals("Switch")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof TiledMapTileMapObject) {
                            levers.add(new Lever((TiledMapTileMapObject) obj));
                        }
                    }
                }
                // Interact points của PuzzleLibrary cũ
                else if (layerName.contains("Interact")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            interactPoints.add((RectangleMapObject) obj);
                        }
                    }
                }
                // Guards patrol path: hỗ trợ cả Polyline và Polygon
                else if (layerName.equals("Guards")) {
                    for (MapObject obj : layer.getObjects()) {
                        Array<Vector2> path = readGuardPath(obj);
                        if (path.size > 0) {
                            float startX = path.get(0).x - 16f;
                            float startY = path.get(0).y - 20f;
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

    private TiledMapTileLayer getTileLayerSafe(String name) {
        if (map == null) return null;
        MapLayer layer = map.getLayers().get(name);
        if (layer instanceof TiledMapTileLayer) {
            return (TiledMapTileLayer) layer;
        }
        return null;
    }

    private Array<Vector2> readGuardPath(MapObject obj) {
        Array<Vector2> path = new Array<>();

        if (obj instanceof PolylineMapObject) {
            Polyline polyline = ((PolylineMapObject) obj).getPolyline();
            float[] vertices = polyline.getTransformedVertices();
            for (int i = 0; i < vertices.length; i += 2) {
                path.add(new Vector2(vertices[i], vertices[i + 1]));
            }
        } else if (obj instanceof PolygonMapObject) {
            com.badlogic.gdx.math.Polygon polygon = ((PolygonMapObject) obj).getPolygon();
            float[] vertices = polygon.getTransformedVertices();
            for (int i = 0; i < vertices.length; i += 2) {
                path.add(new Vector2(vertices[i], vertices[i + 1]));
            }
        }

        return path;
    }

    // =========================
    // SPAWN
    // =========================
    public Rectangle getSpawnPoint(String fromMap) {
        if (map == null || fromMap == null) return null;

        MapLayer spawnLayer = map.getLayers().get("SpawnPoints");
        if (spawnLayer == null) return null;

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

        // Hỗ trợ cả tên cũ và tên mới để không vỡ map cũ
        Rectangle spawn = getPlayerSpawnFromLayer("Player_spawn", "player_spawn");
        if (spawn != null) return spawn;

        return getPlayerSpawnFromLayer("Player_Spawn", "Player_Spawn");
    }

    private Rectangle getPlayerSpawnFromLayer(String layerName, String objectName) {
        MapLayer layer = map.getLayers().get(layerName);
        if (layer == null) return null;

        for (MapObject obj : layer.getObjects()) {
            if (objectName.equals(obj.getName())) {
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
    // RENDER: GIỮ LOGIC CŨ
    // Background -> Player/Entities -> Overhead
    // =========================
    public void renderBackground(OrthographicCamera camera) {
        if (renderer == null || map == null) return;

        renderer.setView(camera);

        // 1. Vẽ tile layer, bỏ Overhead
        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) {
            if (layer.isVisible() && layer instanceof TiledMapTileLayer) {
                if (!layer.getName().equals("Overhead")) {
                    renderer.renderTileLayer((TiledMapTileLayer) layer);
                }
            }
        }
        renderer.getBatch().end();

        // 2. Vẽ object layer có hình ảnh, bỏ collision/spawn/overhead/logic layer
        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) {
            if (layer.isVisible() && !(layer instanceof TiledMapTileLayer)) {
                String n = layer.getName();
                if (!n.contains("Collision")
                    && !n.contains("spawn")
                    && !n.contains("Spawn")
                    && !n.equals("SpawnPoints")
                    && !n.equals("Overhead")
                    && !n.equals("Guards")
                    && !n.equals("Pushable")
                    && !n.equals("Keys")
                    && !n.equals("Hide_Trigger")) {
                    renderObjectLayer(layer);
                }
            }
        }
        renderer.getBatch().end();
    }

    public void renderForeground(OrthographicCamera camera) {
        if (renderer == null || map == null) return;

        renderer.setView(camera);
        MapLayer overhead = map.getLayers().get("Overhead");
        if (overhead != null && overhead.isVisible()) {
            renderer.getBatch().begin();
            if (overhead instanceof TiledMapTileLayer) {
                renderer.renderTileLayer((TiledMapTileLayer) overhead);
            } else {
                renderObjectLayer(overhead);
            }
            renderer.getBatch().end();
        }
    }

    // Giữ lại hàm render(camera) để code cũ/nhánh bạn của bạn không bị lỗi nếu còn gọi.
    public void render(OrthographicCamera camera) {
        renderBackground(camera);
        renderForeground(camera);
    }

    private void renderObjectLayer(MapLayer layer) {
        for (MapObject obj : layer.getObjects()) {
            if (obj.isVisible() && obj instanceof TiledMapTileMapObject) {
                TiledMapTileMapObject tObj = (TiledMapTileMapObject) obj;
                renderer.getBatch().draw(
                    tObj.getTile().getTextureRegion(),
                    tObj.getX(), tObj.getY() - 32,
                    tObj.getOriginX(), tObj.getOriginY(),
                    tObj.getTextureRegion().getRegionWidth(), tObj.getTextureRegion().getRegionHeight(),
                    tObj.getScaleX(), tObj.getScaleY(), tObj.getRotation()
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
        for (PushableBlock block : pushables) {
            if (entityRect.overlaps(block.getBounds())) return true;
        }
        return false;
    }

    public Array<Rectangle> getWallCollision() {
        return wallCollision;
    }

    public Array<Rectangle> getFullCollision() {
        Array<Rectangle> allHitboxes = new Array<>();
        allHitboxes.addAll(wallCollision);

        for (RectangleMapObject door : doorObjects) {
            allHitboxes.add(door.getRectangle());
        }

        for (PushableBlock block : pushables) {
            allHitboxes.add(block.getBounds());
        }

        return allHitboxes;
    }

    public PushableBlock getCollidingPushable(Rectangle rect) {
        for (PushableBlock block : pushables) {
            if (rect.overlaps(block.getBounds())) {
                return block;
            }
        }
        return null;
    }

    // =========================
    // PORTAL
    // =========================
    public String checkPortal(Rectangle entityRect) {
        for (MapObject portal : portalObjects) {
            if (portal instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) portal).getRectangle();

                if (entityRect.overlaps(rect)) {
                    // Nếu portal bị cửa đóng chắn thì chưa được qua
                    for (RectangleMapObject door : doorObjects) {
                        if (rect.overlaps(door.getRectangle())) {
                            return null;
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                        || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                        String dest = portal.getName();
                        if (dest != null && dest.endsWith(".tmx")) {
                            return "map/" + dest;
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
        if (doorName == null) return;

        String target = doorName.toLowerCase();

        // Cho phép gọi bằng nhiều tên khác nhau
        if (target.contains("hidden")) {
            target = "hidden_room";
        }

        // Xóa vùng va chạm cửa
        for (int i = doorObjects.size - 1; i >= 0; i--) {
            String objName = doorObjects.get(i).getName();

            if (objName == null) continue;

            String current = objName.toLowerCase();

            if (current.contains(target) || target.contains(current)) {
                doorObjects.removeIndex(i);
            }
        }

        // Ẩn layer collision của cửa bí mật nếu có
        if (target.equals("hidden_room")) {
            MapLayer hiddenCollision = map.getLayers().get("Hidden_Room_Collision");
            if (hiddenCollision != null) {
                hiddenCollision.setVisible(false);
            }

            System.out.println("✅ Hidden room collision removed!");
            return;
        }

        // Logic cửa thường cũ của bạn
        MapLayer visualDoorLayer = map.getLayers().get("Door");
        if (visualDoorLayer == null) {
            visualDoorLayer = map.getLayers().get("Doors");
        }

        if (visualDoorLayer != null) {
            for (MapObject obj : visualDoorLayer.getObjects()) {
                if (obj.getName() != null && obj.getName().equals(doorName)) {
                    obj.setVisible(false);
                }
            }
        }
    }
    public void openHiddenRoom() {
        // 1. Ẩn lớp hình ảnh che phòng bí mật
        MapLayer hideFloor = map.getLayers().get("Hide_Floor");
        if (hideFloor != null) {
            hideFloor.setVisible(false);
        }

        // 2. Ẩn layer collision cho dễ debug trong Tiled/render
        MapLayer hiddenCollisionLayer = map.getLayers().get("Hidden_Room_Collision");
        if (hiddenCollisionLayer != null) {
            hiddenCollisionLayer.setVisible(false);

            // 3. Xóa khỏi wallCollision nếu nó đang nằm trong wallCollision
            for (MapObject obj : hiddenCollisionLayer.getObjects()) {
                if (obj instanceof RectangleMapObject) {
                    Rectangle rectToRemove = ((RectangleMapObject) obj).getRectangle();

                    for (int i = wallCollision.size - 1; i >= 0; i--) {
                        Rectangle wall = wallCollision.get(i);

                        if (wall.overlaps(rectToRemove)
                            || rectToRemove.overlaps(wall)
                            || sameRect(wall, rectToRemove)) {
                            wallCollision.removeIndex(i);
                        }
                    }

                    // 4. Xóa khỏi doorObjects nếu lỡ đang nằm trong doorObjects
                    for (int i = doorObjects.size - 1; i >= 0; i--) {
                        Rectangle doorRect = doorObjects.get(i).getRectangle();

                        if (doorRect.overlaps(rectToRemove)
                            || rectToRemove.overlaps(doorRect)
                            || sameRect(doorRect, rectToRemove)) {
                            doorObjects.removeIndex(i);
                        }
                    }
                }
            }
        }

        System.out.println("✅ Hidden room opened: Hide_Floor hidden + Hidden_Room_Collision removed");
    }

    private boolean sameRect(Rectangle a, Rectangle b) {
        return Math.abs(a.x - b.x) < 0.01f
            && Math.abs(a.y - b.y) < 0.01f
            && Math.abs(a.width - b.width) < 0.01f
            && Math.abs(a.height - b.height) < 0.01f;
    }

    private void hideDoorObjectInLayer(String layerName, String doorName) {
        if (map == null) return;
        MapLayer visualDoorLayer = map.getLayers().get(layerName);
        if (visualDoorLayer == null) return;

        for (MapObject obj : visualDoorLayer.getObjects()) {
            if (doorName.equals(obj.getName())) {
                obj.setVisible(false);
            }
        }
    }

    // =========================
    // PUZZLE MỚI
    // =========================
    public void updateFloorHide(Player player) {
        if (map == null) return;

        MapLayer hideLayer = map.getLayers().get("Floor_Hide");
        if (hideLayer == null || hideTrigger == null) return;

        boolean triggered = player.hitbox.overlaps(hideTrigger);

        if (!triggered) {
            for (PushableBlock block : pushables) {
                if (block.getBounds().overlaps(hideTrigger)) {
                    triggered = true;
                    break;
                }
            }
        }

        hideLayer.setVisible(!triggered);
    }

    public void resetPushables() {
        for (PushableBlock block : pushables) {
            block.resetPosition();
        }
    }

    // =========================
    // GETTERS
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

    public float getSortY() {
        if (map == null) return 0;
        Float y = map.getProperties().get("sortY", Float.class);
        return (y != null) ? y : 0;
    }

    public Array<RectangleMapObject> getInteractPoints() {
        return interactPoints;
    }

    public Array<PushableBlock> getPushables() {
        return pushables;
    }

    public Array<Key> getKeys() {
        return keys;
    }

    public Array<Door> getDoors() {
        return doors;
    }

    public Rectangle getHideTrigger() {
        return hideTrigger;
    }

    // =========================
    // DISPOSE
    // =========================
    public void dispose() {
        if (map != null) map.dispose();
        if (renderer != null) renderer.dispose();
    }
}
