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
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;

import game.maskedbee.entities.Boss;
import game.maskedbee.entities.Guard;
import game.maskedbee.entities.Player;
import game.maskedbee.objects.Door;
import game.maskedbee.objects.Key;
import game.maskedbee.objects.Lever;
import game.maskedbee.objects.PushableBlock;
import game.maskedbee.objects.Spike;
import game.maskedbee.main.AudioManager;

public class MapManager {

    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    private final Array<Rectangle> wallCollision = new Array<>();
    private final Array<RectangleMapObject> doorObjects = new Array<>();
    private final Array<MapObject> portalObjects = new Array<>();
    private final Array<RectangleMapObject> interactPoints = new Array<>();

    private final Array<PushableBlock> pushables = new Array<>();
    private final Array<Key> keys = new Array<>();
    private final Array<Door> doors = new Array<>();

    private Rectangle hideTrigger;
    private TiledMapTileLayer doorCloseLayer;
    private TiledMapTileLayer doorOpenLayer;

    public final Array<Spike> spikes = new Array<>();
    public final Array<Lever> levers = new Array<>();
    public final Array<Guard> guards = new Array<>();
    public final Array<Boss> bosses = new Array<>();

    private final Array<Rectangle> stoneCollision = new Array<>();
    private final Array<Rectangle> skullCollision = new Array<>();

    private String currentMapName = "";
    private String lastMapName = "";

    // Lưu tiến trình trong phiên chơi hiện tại.
    // Khi rời map rồi quay lại, MapManager sẽ load lại .tmx rồi áp lại trạng thái này.
    private final ObjectSet<String> openedDoors = new ObjectSet<>();
    private final ObjectSet<String> openedHiddenRooms = new ObjectSet<>();
    private final ObjectSet<String> collectedKeys = new ObjectSet<>();

    // Lưu trạng thái động trong phiên chơi hiện tại
    private final ObjectMap<String, Vector2> savedPushablePositions = new ObjectMap<>();
    private final ObjectMap<String, Boolean> savedSpikeStates = new ObjectMap<>();
    private final ObjectMap<String, Boolean> savedLeverStates = new ObjectMap<>();

    private String stateKey(String objectName) {
        return currentMapName + ":" + objectName;
    }

    private String pushableStateKey(PushableBlock block) {
        return currentMapName + ":pushable:" + block.getId();
    }

    private String tileObjectStateKey(String category, TiledMapTileMapObject obj) {
        String objectName = obj.getName();

        if (objectName != null && !objectName.isEmpty()) {
            return currentMapName + ":" + category + ":" + objectName;
        }

        return currentMapName + ":" + category + ":"
            + Math.round(obj.getX()) + ":"
            + Math.round(obj.getY());
    }

    public boolean isHiddenRoomOpened() {
        return openedHiddenRooms.contains(stateKey("hidden_room"));
    }

    private boolean isFloorHideOpened() {
        if (map == null) return false;

        MapLayer hideLayer = map.getLayers().get("Floor_Hide");
        if (hideLayer == null) return false;

        return !hideLayer.isVisible();
    }

    public void markKeyCollected(String keyName) {
        if (keyName == null || keyName.isEmpty()) return;
        collectedKeys.add(stateKey(keyName));
        for (Key key : keys) {
            if (keyName.equals(key.getName())) {
                key.collect();
                break;
            }
        }
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
            interactPoints.clear();
            pushables.clear();
            keys.clear();
            doors.clear();
            spikes.clear();
            levers.clear();
            guards.clear();
            bosses.clear();
            skullCollision.clear();
            stoneCollision.clear();

            hideTrigger = null;

            doorCloseLayer = getTileLayerSafe("Door_Close");
            doorOpenLayer = getTileLayerSafe("Door_Open");

            for (MapLayer layer : map.getLayers()) {
                if (layer == null) continue;

                String layerName = layer.getName();

                // Skull_Collision là vùng núp boss.
                // Không được cho vào wallCollision, vì player phải đi vào được.
                if (layerName.equals("Skull_Collision")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            skullCollision.add(((RectangleMapObject) obj).getRectangle());
                        }
                    }
                }

                // Boss / Bosses layer.
                else if (layerName.equals("Boss") || layerName.equals("Bosses")) {
                    loadBossLayer(layer);
                }
                else if (layerName.equals("Jail_Door_Collision")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            RectangleMapObject rectObj = (RectangleMapObject) obj;
                            if (rectObj.getName() == null || rectObj.getName().isEmpty()) {
                                rectObj.setName("jail_door_hitbox");
                            }
                            doorObjects.add(rectObj);
                        }
                    }
                }
                else if (layerName.equals("Stone_Collision")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                            // Stone vẫn là vật cản bình thường
                            wallCollision.add(rect);
                            // Nhưng chỉ Stone_Collision mới được dùng để núp bằng F
                            stoneCollision.add(rect);
                        }
                    }
                }
                else if (layerName.equals("Chest_Collision")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            RectangleMapObject rectObj = (RectangleMapObject) obj;

                            if (rectObj.getName() == null || rectObj.getName().isEmpty()) {
                                rectObj.setName("gold_key_chest_block");
                            }
                            // Cho vào doorObjects để sau này openDoor() xóa được
                            doorObjects.add(rectObj);
                        }
                    }
                }
                else if (layerName.contains("Collision")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            String objName = obj.getName();
                            if (objName != null && objName.contains("jail_door")) {
                                doorObjects.add((RectangleMapObject) obj);
                            } else {
                                wallCollision.add(((RectangleMapObject) obj).getRectangle());
                            }
                        }
                    }
                }
                else if (layerName.equals("Doors")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            RectangleMapObject rectObj = (RectangleMapObject) obj;
                            doorObjects.add(rectObj);
                            doors.add(new Door(rectObj));
                        }
                    }
                }
                else if (layerName.equals("Pushable")) {
                    int pushableIndex = 0;

                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                            Rectangle fixedRect = new Rectangle(
                                (float) Math.floor(rect.x / 32f) * 32f,
                                (float) Math.floor(rect.y / 32f) * 32f,
                                32f,
                                32f
                            );

                            String id = obj.getName();
                            if (id == null || id.isEmpty()) {
                                id = "pushable_" + Math.round(fixedRect.x) + "_" + Math.round(fixedRect.y) + "_" + pushableIndex;
                            }

                            pushables.add(new PushableBlock(fixedRect, id));
                            pushableIndex++;
                        }
                    }
                }
                else if (layerName.equals("Hide_Trigger")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            hideTrigger = ((RectangleMapObject) obj).getRectangle();
                        }
                    }
                }
                else if (layerName.equals("Keys")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            keys.add(new Key((RectangleMapObject) obj));
                        }
                    }
                }
                else if (layerName.equals("Exit") || layerName.contains("_Chamber") || layerName.contains("Corridor")) {
                    for (MapObject obj : layer.getObjects()) {
                        portalObjects.add(obj);
                    }
                }
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
                else if (layerName.contains("Interact")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            interactPoints.add((RectangleMapObject) obj);
                        }
                    }
                }
                else if (layerName.equals("Guards")) {
                    for (MapObject obj : layer.getObjects()) {
                        Array<Vector2> path = readGuardPath(obj);
                        if (path.size > 0) {
                            guards.add(new Guard(path.get(0).x - 16f, path.get(0).y - 20f, path));
                        }
                    }
                }
            }

            resetFloorHideToClosed();

            applySavedMapState();
            applySavedPushableState();
            applySavedSpikeLeverState();

            updateFloorHide(null);

            System.out.println("✅ Loaded map: " + fileName);

            // ================================================================
            // THÊM NHẠC NỀN (Tên file, Âm lượng đích)
            // ================================================================
            if (currentMapName.equals("Library.tmx")||currentMapName.equals("Hidden_Room.tmx")||currentMapName.equals("Old_Corridor.tmx")||
                currentMapName.equals("Old_Chapel.tmx")||currentMapName.equals("Exit_Chamber.tmx")) {
                AudioManager.getInstance().playBackgroundMusic("audio/Save_Room_Theme.ogg", 0.4f);
            } else if (currentMapName.equals("Ritual_Chamber.tmx")||currentMapName.equals("Queen_Chamber.tmx")||currentMapName.equals("Corridor.tmx")) {
                AudioManager.getInstance().playBackgroundMusic("audio/The_Gauntlet.ogg", 0.2f);
            } else{
                // Nhạc nền mặc định cho các map khác
                AudioManager.getInstance().playBackgroundMusic("audio/The_Gauntlet.ogg", 0.1f);
            }
        } catch (Exception e) {
            Gdx.app.error("MapManager", "❌ Error loading map: " + fileName, e);
        }
    }

    private void loadBossLayer(MapLayer layer) {
        Array<Vector2> path = null;
        Float bossX = null;
        Float bossY = null;

        for (MapObject obj : layer.getObjects()) {
            Array<Vector2> objectPath = readPath(obj);

            if (objectPath.size > 0) {
                path = objectPath;

                if (bossX == null || bossY == null) {
                    bossX = objectPath.get(0).x;
                    bossY = objectPath.get(0).y;
                }

                continue;
            }

            if (obj instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                bossX = rect.x;
                bossY = rect.y;
            } else if (obj instanceof PointMapObject) {
                bossX = ((PointMapObject) obj).getPoint().x;
                bossY = ((PointMapObject) obj).getPoint().y;
            } else if (obj instanceof TiledMapTileMapObject) {
                TiledMapTileMapObject tileObject = (TiledMapTileMapObject) obj;
                bossX = tileObject.getX();
                bossY = tileObject.getY() - 32f;
            }
        }

        if (path != null && path.size > 0) {
            bosses.add(new Boss(0, 0, path));
            System.out.println("Loaded boss with path");
        } else if (bossX != null && bossY != null) {
            bosses.add(new Boss(bossX, bossY, null));
            System.out.println("Loaded boss at " + bossX + ", " + bossY);
        }
    }

    private Array<Vector2> readPath(MapObject obj) {
        Array<Vector2> path = new Array<>();

        if (obj instanceof PolylineMapObject) {
            Polyline polyline = ((PolylineMapObject) obj).getPolyline();
            float[] vertices = polyline.getTransformedVertices();

            for (int i = 0; i < vertices.length; i += 2) {
                path.add(new Vector2(vertices[i], vertices[i + 1]));
            }
        } else if (obj instanceof PolygonMapObject) {
            Polygon polygon = ((PolygonMapObject) obj).getPolygon();
            float[] vertices = polygon.getTransformedVertices();

            for (int i = 0; i < vertices.length; i += 2) {
                path.add(new Vector2(vertices[i], vertices[i + 1]));
            }
        }

        return path;
    }
    private TiledMapTileLayer getTileLayerSafe(String name) {
        if (map == null) return null;
        MapLayer layer = map.getLayers().get(name);
        return (layer instanceof TiledMapTileLayer) ? (TiledMapTileLayer) layer : null;
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

        Rectangle spawn = getPlayerSpawnFromLayer("Player_spawn", "player_spawn");
        if (spawn != null) return spawn;
        spawn = getPlayerSpawnFromLayer("Player_Spawn", "Player_Spawn");
        if (spawn != null) return spawn;
        spawn = getPlayerSpawnFromLayer("Player_spawn", "Player_spawn");
        if (spawn != null) return spawn;
        return getPlayerSpawnFromLayer("player_spawn", "player_spawn");
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

    public void renderBackground(OrthographicCamera camera, boolean isMasked) {
        if (renderer == null || map == null) return;

        renderer.setView(camera);

        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) {
            if (layer.isVisible() && layer instanceof TiledMapTileLayer) {
                String layerName = layer.getName();

                // ================================================================
                // LOGIC LỌC LAYER THEO MẶT NẠ
                // ================================================================
                if (layerName.equals("Queen_Blood") && !isMasked) {
                    continue; // Nếu KHÔNG đeo mặt nạ -> Không vẽ Queen_Blood
                }
                if (layerName.equals("Blood") && isMasked) {
                    continue; // Nếu đeo mặt nạ -> Không vẽ Blood
                }
                // ================================================================

                if (!layerName.equals("Overhead") && !layerName.equals("Small_Cocon")) {
                    renderer.renderTileLayer((TiledMapTileLayer) layer);
                }
            }
        }
        renderer.getBatch().end();

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
                    && !n.equals("Small_Cocon")
                    && !n.equals("Keys")
                    && !n.equals("Hide_Trigger")) {
                    renderObjectLayer(layer);
                }
            }
        }
        renderer.getBatch().end();
    }

    public void renderForeground(OrthographicCamera camera, boolean isMasked) {
        if (renderer == null || map == null) return;

        renderer.setView(camera);
        MapLayer overhead = map.getLayers().get("Overhead");
        if (overhead != null && overhead.isVisible()) {

            // Kiểm tra nếu layer Overhead hoặc các sub-layer chứa tên máu theo cơ chế mặt nạ
            String layerName = overhead.getName();
            if (layerName.equals("Queen_Blood") && !isMasked) return;
            if (layerName.equals("Blood") && isMasked) return;

            renderer.getBatch().begin();
            if (overhead instanceof TiledMapTileLayer) {
                renderer.renderTileLayer((TiledMapTileLayer) overhead);
            } else {
                renderObjectLayer(overhead);
            }
            renderer.getBatch().end();
        }
    }

    public void render(OrthographicCamera camera, boolean isMasked) {
        renderBackground(camera, isMasked);
        renderForeground(camera, isMasked);
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

    public Array<Rectangle> getSkullCollision() {
        return skullCollision;
    }

    public Array<Rectangle> getStoneCollision() {
        return stoneCollision;
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

    public String checkPortal(Rectangle entityRect) {
        for (MapObject portal : portalObjects) {
            if (!(portal instanceof RectangleMapObject)) {
                continue;
            }
            Rectangle rect = ((RectangleMapObject) portal).getRectangle();
            if (!entityRect.overlaps(rect)) {
                continue;
            }
            String dest = portal.getName();
            // player chưa đứng vào portal
            if (!entityRect.overlaps(rect)) {
                continue;
            }
            // PORTAL RIÊNG
            if ("Old_Corridor.tmx".equals(dest)) {
                if (!isFloorHideOpened()) {
                    return null;
                }
            }
            // PORTAL THƯỜNG
            else {

                for (RectangleMapObject door : doorObjects) {
                    if (rect.overlaps(door.getRectangle())) {
                        return null;
                    }
                }
            }
            // NHẤN SPACE / ENTER
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                if (dest != null && dest.endsWith(".tmx")) {
                    return "map/" + dest;
                }
            }
        }
        return null;
    }

    public void openDoor(String doorName) {
        if (doorName == null) return;

        String target = doorName.toLowerCase();

        if (target.contains("hidden")) {
            openHiddenRoom();
            return;
        }

        // Am thanh mo cua
        AudioManager.getInstance().playSoundEffect("audio/Door_Open.wav", 0.5f);

        openedDoors.add(stateKey(doorName));
        applyDoorOpened(doorName);

        System.out.println("🚪 Door opened: " + doorName);
    }
    private void applyDoorOpened(String doorName) {
        if (doorName == null) return;

        String target = doorName.toLowerCase();

        for (int i = doorObjects.size - 1; i >= 0; i--) {
            String objName = doorObjects.get(i).getName();
            if (objName == null) continue;

            String current = objName.toLowerCase();
            if (current.contains(target) || target.contains(current)) {
                System.out.println("Removed door collision: " + objName);
                doorObjects.removeIndex(i);
            }
        }

        hideDoorObjectInLayer("Door", doorName);
        hideDoorObjectInLayer("Doors", doorName);
        hideDoorObjectInLayer("Jail", doorName);

        for (Door door : doors) {
            if (door.getName() != null && door.getName().equals(doorName)) {
                door.open();
                Rectangle b = door.getBounds();
                int tileX = Math.round(b.x / 32f);
                int tileY = Math.round(b.y / 32f);

                if (doorCloseLayer != null) {
                    for (int i = 0; i < 2; i++) {
                        doorCloseLayer.setCell(tileX, tileY + i, null);
                    }
                }

                if (doorOpenLayer != null) {
                    doorOpenLayer.setVisible(true);
                }
                break;
            }
        }
    }

    public void openHiddenRoom() {
        // Am thanh mo cua
        AudioManager.getInstance().playSoundEffect("audio/Door_Open.wav", 0.5f);

        openedHiddenRooms.add(stateKey("hidden_room"));
        applyHiddenRoomOpened();
        System.out.println("✅ Hidden room opened");
    }
    private void applyHiddenRoomOpened() {
        MapLayer hideFloor = map.getLayers().get("Hide_Floor");
        if (hideFloor != null) {
            hideFloor.setVisible(false);
        }

        MapLayer hiddenCollisionLayer = map.getLayers().get("Hidden_Room_Collision");
        if (hiddenCollisionLayer != null) {
            hiddenCollisionLayer.setVisible(false);

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
                }
            }
        }
    }

    private void applySavedMapState() {
        if (openedHiddenRooms.contains(stateKey("hidden_room"))) {
            applyHiddenRoomOpened();
        }

        String prefix = currentMapName + ":";

        for (String key : openedDoors) {
            if (key.startsWith(prefix)) {
                String doorName = key.substring(prefix.length());
                applyDoorOpened(doorName);
            }
        }

        for (String key : collectedKeys) {
            if (key.startsWith(prefix)) {
                String keyName = key.substring(prefix.length());
                hideCollectedKey(keyName);
            }
        }
    }

    private void hideCollectedKey(String keyName) {
        if (keyName == null) return;

        for (Key key : keys) {
            if (keyName.equals(key.getName())) {
                key.collect();
                break;
            }
        }

        MapLayer keyLayer = map.getLayers().get("Keys");
        if (keyLayer != null) {
            for (MapObject obj : keyLayer.getObjects()) {
                if (keyName.equals(obj.getName())) {
                    obj.setVisible(false);
                }
            }
        }
    }

    private boolean sameRect(Rectangle a, Rectangle b) {
        return Math.abs(a.x - b.x) < 0.01f
            && Math.abs(a.y - b.y) < 0.01f
            && Math.abs(a.width - b.width) < 0.01f
            && Math.abs(a.height - b.height) < 0.01f;
    }

    private void hideDoorObjectInLayer(String layerName, String doorName) {
        if (map == null || doorName == null) return;

        MapLayer visualDoorLayer = map.getLayers().get(layerName);
        if (visualDoorLayer == null) return;

        for (MapObject obj : visualDoorLayer.getObjects()) {
            String objName = obj.getName();
            if (objName != null && objName.equals(doorName)) {
                obj.setVisible(false);
            }
        }
    }


    public void updateFloorHide(Player player) {
        if (map == null) return;

        MapLayer hideLayer = map.getLayers().get("Floor_Hide");
        if (hideLayer == null) return;
        // Không có trigger thì hầm mặc định đóng
        if (hideTrigger == null) {
            hideLayer.setVisible(true);
            return;
        }
        boolean triggered = false;
        // 1. Player đứng lên trigger thì mở hầm
        if (player != null && player.hitbox.overlaps(hideTrigger)) {
            triggered = true;
        }
        // 2. Cocoon / PushableBlock đứng lên trigger thì cũng mở hầm
        if (!triggered) {
            for (PushableBlock block : pushables) {
                Rectangle b = block.getBounds();
                // Dùng tâm block để tránh chỉ chạm mép trigger đã mở
                float centerX = b.x + b.width / 2f;
                float centerY = b.y + b.height / 2f;

                if (hideTrigger.contains(centerX, centerY)) {
                    triggered = true;
                    break;
                }
            }
        }
        // Có người hoặc cocoon đè lên -> ẩn Floor_Hide -> hầm mở
        // Không có gì đè lên -> hiện Floor_Hide -> hầm đóng
        hideLayer.setVisible(!triggered);
    }
    private void resetFloorHideToClosed() {
        if (map == null) return;
        MapLayer hideLayer = map.getLayers().get("Floor_Hide");
        if (hideLayer != null) {
            hideLayer.setVisible(true);
        }
    }
    public void rememberPushableState(PushableBlock block) {
        if (block == null) return;

        Rectangle b = block.getBounds();
        savedPushablePositions.put(pushableStateKey(block), new Vector2(b.x, b.y));
        AudioManager.getInstance().playSoundEffect("audio/Cocoon_Push.wav", 0.8f);
    }
    private void applySavedPushableState() {
        for (PushableBlock block : pushables) {
            Vector2 saved = savedPushablePositions.get(pushableStateKey(block));
            if (saved != null) {
                block.setPosition(saved.x, saved.y);
            }
        }
    }
    public void clearPushableStateForCurrentMap() {
        String prefix = currentMapName + ":pushable:";
        Array<String> keysToRemove = new Array<>();

        for (String key : savedPushablePositions.keys()) {
            if (key.startsWith(prefix)) {
                keysToRemove.add(key);
            }
        }

        for (String key : keysToRemove) {
            savedPushablePositions.remove(key);
        }
    }
    public void resetPushables() {
        clearPushableStateForCurrentMap();

        for (PushableBlock block : pushables) {
            block.resetPosition();
        }

        updateFloorHide(null);
    }
    public void rememberSpikeState(Spike spike) {
        if (spike == null || spike.mapObject == null) return;

        savedSpikeStates.put(
            tileObjectStateKey("spike", spike.mapObject),
            spike.isUp
        );
    }
    public void rememberLeverState(Lever lever) {
        if (lever == null || lever.mapObject == null) return;

        savedLeverStates.put(
            tileObjectStateKey("lever", lever.mapObject),
            lever.isPulled
        );

        AudioManager.getInstance().playSoundEffect("audio/Lever_Sound.wav", 0.5f);
    }
    private void applySavedSpikeLeverState() {
        for (Spike spike : spikes) {
            if (spike == null || spike.mapObject == null) continue;

            Boolean savedIsUp = savedSpikeStates.get(tileObjectStateKey("spike", spike.mapObject));
            if (savedIsUp != null && spike.isUp != savedIsUp) {
                spike.toggle(map);
            }
        }

        for (Lever lever : levers) {
            if (lever == null || lever.mapObject == null) continue;

            Boolean savedPulled = savedLeverStates.get(tileObjectStateKey("lever", lever.mapObject));
            if (savedPulled != null && lever.isPulled != savedPulled) {
                lever.toggle(map);
            }
        }
    }
    public void clearSpikeLeverStateForCurrentMap() {
        String spikePrefix = currentMapName + ":spike:";
        String leverPrefix = currentMapName + ":lever:";

        Array<String> spikeKeysToRemove = new Array<>();
        for (String key : savedSpikeStates.keys()) {
            if (key.startsWith(spikePrefix)) {
                spikeKeysToRemove.add(key);
            }
        }
        for (String key : spikeKeysToRemove) {
            savedSpikeStates.remove(key);
        }

        Array<String> leverKeysToRemove = new Array<>();
        for (String key : savedLeverStates.keys()) {
            if (key.startsWith(leverPrefix)) {
                leverKeysToRemove.add(key);
            }
        }
        for (String key : leverKeysToRemove) {
            savedLeverStates.remove(key);
        }
    }
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

    public void dispose() {
        // NGẮT NHẠC NỀN KHI MAP MANAGER BỊ HỦY
        AudioManager.getInstance().stopBackgroundMusic();
        if (map != null) map.dispose();
        if (renderer != null) renderer.dispose();
    }
}
