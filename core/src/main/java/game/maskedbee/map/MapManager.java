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
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import game.maskedbee.entities.Boss;
import game.maskedbee.entities.Guard;
import game.maskedbee.entities.Player;
import game.maskedbee.main.AudioManager;
import game.maskedbee.main.NotificationManager;
import game.maskedbee.objects.Door;
import game.maskedbee.objects.Key;
import game.maskedbee.objects.Lever;
import game.maskedbee.objects.PushableBlock;
import game.maskedbee.objects.Spike;

public class MapManager {

    // =========================================================
    // STATE / MAP DATA
    // =========================================================

    public final MapStateManager state = new MapStateManager();

    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private String currentMapName = "";
    private String lastMapName = "";

    private final Array<Rectangle> wallCollision = new Array<>();
    private final Array<Rectangle> stoneCollision = new Array<>();
    private final Array<Rectangle> skullCollision = new Array<>();
    private final Array<RectangleMapObject> doorObjects = new Array<>();

    private final Array<MapObject> portalObjects = new Array<>();
    private final Array<RectangleMapObject> interactPoints = new Array<>();
    private final Array<PushableBlock> pushables = new Array<>();
    private final Array<Key> keys = new Array<>();
    private final Array<Door> doors = new Array<>();

    public final Array<Spike> spikes = new Array<>();
    public final Array<Lever> levers = new Array<>();
    public final Array<Guard> guards = new Array<>();
    public final Array<Boss> bosses = new Array<>();

    private Rectangle hideTrigger;
    private TiledMapTileLayer doorCloseLayer;
    private TiledMapTileLayer doorOpenLayer;
    private boolean wasFloorHideTriggered = false;

    private interface RectHandler { void handle(RectangleMapObject obj); }
    private interface TileHandler { void handle(TiledMapTileMapObject obj); }
    // =========================================================
    // LOAD MAP
    // =========================================================

    public void loadMap(String fileName) {
        try {
            lastMapName = currentMapName;
            currentMapName = fileName.replace("map/", "");
            state.setCurrentMapName(currentMapName);

            if (map != null) map.dispose();
            if (renderer != null) renderer.dispose();

            map = new TmxMapLoader().load(fileName);
            renderer = new OrthogonalTiledMapRenderer(map);

            clearCurrentMapData();
            doorCloseLayer = getTileLayerSafe("Door_Close");
            doorOpenLayer = getTileLayerSafe("Door_Open");

            for (MapLayer layer : map.getLayers()) if (layer != null) parseLayer(layer);

            resetFloorHideToClosed();
            applySavedStateToCurrentMap();
            updateFloorHide(null);

            System.out.println("✅ Loaded map: " + fileName);
            playMusicForCurrentMap();
        } catch (Exception e) {
            Gdx.app.error("MapManager", "Error loading map: " + fileName, e);
        }
    }

    private void clearCurrentMapData() {
        wallCollision.clear(); stoneCollision.clear(); skullCollision.clear(); doorObjects.clear();
        portalObjects.clear(); interactPoints.clear(); pushables.clear(); keys.clear(); doors.clear();
        spikes.clear(); levers.clear(); guards.clear(); bosses.clear();
        hideTrigger = null;
        wasFloorHideTriggered = false;
    }

    private void applySavedStateToCurrentMap() {
        if (state.isHiddenRoomOpened()) applyHiddenRoomOpened();
        state.applyOpenedDoors(state.currentMapPrefix(), this::applyDoorOpened);
        state.applyCollectedKeys(state.currentMapPrefix(), this::hideCollectedKey);
        state.applyPushablePositions(pushables);
        state.applySpikeLeverStates(spikes, levers, map);
    }

    // =========================================================
    // PARSE LAYERS
    // =========================================================

    private void parseLayer(MapLayer layer) {
        String n = layer.getName();

        if (n.equals("Skull_Collision")) {
            eachRect(layer, r -> { Rectangle rect = r.getRectangle(); skullCollision.add(rect); wallCollision.add(rect); });
        } else if (n.equals("Stone_Collision")) {
            eachRect(layer, r -> { Rectangle rect = r.getRectangle(); wallCollision.add(rect); stoneCollision.add(rect); });
        } else if (n.equals("Jail_Door_Collision")) {
            parseDoorCollision(layer, "jail_door_hitbox");
        } else if (n.equals("Chest_Collision")) {
            parseDoorCollision(layer, "gold_key_chest_block");
        } else if (n.contains("Collision")) {
            eachRect(layer, r -> { String objName = r.getName(); if (objName != null && objName.contains("jail_door")) doorObjects.add(r); else wallCollision.add(r.getRectangle()); });
        } else if (n.equals("Boss") || n.equals("Bosses")) {
            loadBossLayer(layer);
        } else if (n.equals("Doors")) {
            eachRect(layer, r -> { doorObjects.add(r); doors.add(new Door(r)); });
        } else if (n.equals("Pushable")) {
            parsePushableLayer(layer);
        } else if (n.equals("Hide_Trigger")) {
            eachRect(layer, r -> hideTrigger = r.getRectangle());
        } else if (n.equals("Keys")) {
            eachRect(layer, r -> keys.add(new Key(r)));
        } else if (n.equals("Spikes")) {
            eachTile(layer, t -> spikes.add(new Spike(t)));
        } else if (n.equals("Switch")) {
            eachTile(layer, t -> levers.add(new Lever(t)));
        } else if (n.equals("Guards")) {
            parseGuardsLayer(layer);
        } else if (n.equals("Exit") || n.contains("_Chamber") || n.contains("Corridor")) {
            for (MapObject obj : layer.getObjects()) portalObjects.add(obj);
        } else if (n.contains("Interact")) {
            eachRect(layer, interactPoints::add);
        }
    }

    private void eachRect(MapLayer layer, RectHandler handler) {
        for (MapObject obj : layer.getObjects()) if (obj instanceof RectangleMapObject) handler.handle((RectangleMapObject) obj);
    }

    private void eachTile(MapLayer layer, TileHandler handler) {
        for (MapObject obj : layer.getObjects()) if (obj instanceof TiledMapTileMapObject) handler.handle((TiledMapTileMapObject) obj);
    }

    private void parseDoorCollision(MapLayer layer, String fallbackName) {
        eachRect(layer, r -> {
            if (r.getName() == null || r.getName().isEmpty()) r.setName(fallbackName);
            doorObjects.add(r);
        });
    }

    private void parsePushableLayer(MapLayer layer) {
        int index = 0;
        for (MapObject obj : layer.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;
            Rectangle rect = ((RectangleMapObject) obj).getRectangle();
            Rectangle fixed = new Rectangle((float) Math.floor(rect.x / 32f) * 32f, (float) Math.floor(rect.y / 32f) * 32f, 32f, 32f);
            String id = obj.getName();
            if (id == null || id.isEmpty()) id = "pushable_" + Math.round(fixed.x) + "_" + Math.round(fixed.y) + "_" + index;
            pushables.add(new PushableBlock(fixed, id));
            index++;
        }
    }

    private void parseGuardsLayer(MapLayer layer) {
        for (MapObject obj : layer.getObjects()) {
            Array<Vector2> path = readPath(obj);
            if (path.size > 0) guards.add(new Guard(path.get(0).x - 16f, path.get(0).y - 20f, path));
        }
    }

    // =========================================================
    // DOOR / HIDDEN ROOM / KEY
    // =========================================================

    public void openDoor(String doorName) {
        if (doorName == null) return;
        if (doorName.toLowerCase().contains("hidden")) { openHiddenRoom(); return; }

        if (!state.isDoorOpened(doorName)) {
            AudioManager.getInstance().playSoundEffect("audio/Door_Open.wav", 0.5f);
            state.markDoorOpened(doorName);
            applyDoorOpened(doorName);
            System.out.println("🚪 Door opened: " + doorName);
        }
    }

    private void applyDoorOpened(String doorName) {
        if (doorName == null) return;
        MapCollisionHelper.removeDoorByName(doorObjects, doorName);
        hideDoorObjectInLayer("Door", doorName);
        hideDoorObjectInLayer("Doors", doorName);
        hideDoorObjectInLayer("Jail", doorName);

        for (Door door : doors) {
            if (door.getName() == null || !door.getName().equals(doorName)) continue;
            door.open();
            Rectangle b = door.getBounds();
            int tileX = Math.round(b.x / 32f);
            int tileY = Math.round(b.y / 32f);
            if (doorCloseLayer != null) for (int i = 0; i < 2; i++) doorCloseLayer.setCell(tileX, tileY + i, null);
            if (doorOpenLayer != null) doorOpenLayer.setVisible(true);
            break;
        }
    }

    public void openHiddenRoom() {
        if (!state.isHiddenRoomOpened()) AudioManager.getInstance().playSoundEffect("audio/Door_Open.wav", 0.5f);
        state.markHiddenRoomOpened();
        state.saveProgressCheckpointHere();
        applyHiddenRoomOpened();
        System.out.println("✅ Hidden room opened");
    }

    private void applyHiddenRoomOpened() {
        if (map == null) return;
        setLayerVisibleSafe("Hide_Floor", false);

        MapLayer hiddenCollision = map.getLayers().get("Hidden_Room_Collision");
        if (hiddenCollision == null) return;
        hiddenCollision.setVisible(false);
        eachRect(hiddenCollision, r -> MapCollisionHelper.removeWallByRect(wallCollision, r.getRectangle()));
    }

    public void markKeyCollected(String keyName) {
        if (keyName == null || keyName.isEmpty()) return;
        state.markKeyCollected(keyName);
        hideCollectedKey(keyName);
    }

    private void hideCollectedKey(String keyName) {
        if (keyName == null || map == null) return;
        for (Key key : keys) if (keyName.equals(key.getName())) { key.collect(); break; }

        MapLayer keyLayer = map.getLayers().get("Keys");
        if (keyLayer != null) for (MapObject obj : keyLayer.getObjects()) if (keyName.equals(obj.getName())) obj.setVisible(false);

        if ("Library.tmx".equalsIgnoreCase(currentMapName) && "gold_key".equalsIgnoreCase(keyName)) showLibraryChestNoKey();
    }

    // =========================================================
    // STATE WRAPPERS — giữ để code cũ không lỗi compile
    // =========================================================

    public void markPuzzleStepSolved(String stepName) { state.markPuzzleStepSolved(stepName); }
    public boolean isPuzzleStepSolved(String stepName) { return state.isPuzzleStepSolved(stepName); }
    public boolean isKeyCollected(String keyName) { return state.isKeyCollected(keyName); }
    public boolean isHiddenRoomOpened() { return state.isHiddenRoomOpened(); }
    public void clearPushableStateForCurrentMap() { state.clearPushableStateForCurrentMap(); }
    public void clearSpikeLeverStateForCurrentMap() { state.clearSpikeLeverStateForCurrentMap(); }
    public void clearAllProgressState() { state.clearAllProgressState(); }
    public void saveProgressCheckpointHere() { state.saveProgressCheckpointHere(); }
    public boolean hasProgressCheckpoint() { return state.hasProgressCheckpoint(); }
    public String getProgressCheckpointMapName() { return state.getProgressCheckpointMapName(); }
    public void clearProgressCheckpoint() { state.clearProgressCheckpoint(); }

    public void rememberPushableState(PushableBlock block) {
        state.rememberPushableState(block);
        AudioManager.getInstance().playSoundEffect("audio/Cocoon_Push.wav", 0.8f);
    }

    public void rememberSpikeState(Spike spike) { state.rememberSpikeState(spike); }

    public void rememberLeverState(Lever lever) {
        state.rememberLeverState(lever);
        AudioManager.getInstance().playSoundEffect("audio/Lever_Sound.wav", 0.5f);
    }

    // =========================================================
    // FLOOR HIDE / PUSHABLE RESET
    // =========================================================

    public void updateFloorHide(Player player) {
        if (map == null) return;
        MapLayer hideLayer = map.getLayers().get("Floor_Hide");
        if (hideLayer == null) return;
        if (hideTrigger == null) { hideLayer.setVisible(true); return; }

        boolean triggeredByPlayer = player != null && player.hitbox.overlaps(hideTrigger);
        boolean triggeredByPushable = false;

        for (PushableBlock block : pushables) {
            Rectangle b = block.getBounds();
            if (hideTrigger.contains(b.x + b.width / 2f, b.y + b.height / 2f)) { triggeredByPushable = true; break; }
        }

        boolean triggered = triggeredByPlayer || triggeredByPushable;
        hideLayer.setVisible(!triggered);

        if (triggered && !wasFloorHideTriggered) AudioManager.getInstance().playSoundEffect("audio/Door_Open.wav", 0.6f);
        wasFloorHideTriggered = triggered;

        if (triggeredByPushable) {
            if (!hasProgressCheckpoint || !currentMapName.equals(progressCheckpointMapName)) {
                NotificationManager.getInstance().show("Giải đố thành công\nĐã mở ra lối đi bí mật");
                NotificationManager.getInstance().show("Checkpoint mới được lưu thành Disposal");
                saveProgressCheckpointHere();
            }
        }
    }

    private void resetFloorHideToClosed() {
        if (map == null) return;
        MapLayer hideLayer = map.getLayers().get("Floor_Hide");
        if (hideLayer != null) hideLayer.setVisible(true);
    }

    public void resetPushables() {
        state.clearPushableStateForCurrentMap();
        for (PushableBlock block : pushables) block.resetPosition();
        updateFloorHide(null);
    }

    // =========================================================
    // PORTAL / SPAWN POINTS
    // =========================================================

    public String checkPortal(Rectangle entityRect) {
        for (MapObject portal : portalObjects) {
            if (!(portal instanceof RectangleMapObject)) continue;
            Rectangle rect = ((RectangleMapObject) portal).getRectangle();
            if (!entityRect.overlaps(rect)) continue;

            String dest = portal.getName();
            if ("Old_Corridor.tmx".equals(dest) && hasFloorHideLayer()) {
                if (!isFloorHideOpened()) return null;
            } else {
                for (RectangleMapObject door : doorObjects) if (rect.overlaps(door.getRectangle())) return null;
            }

            if ((Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
                && dest != null && dest.endsWith(".tmx")) return "map/" + dest;
        }
        return null;
    }

    private boolean isFloorHideOpened() {
        MapLayer hideLayer = (map == null) ? null : map.getLayers().get("Floor_Hide");
        return hideLayer != null && !hideLayer.isVisible();
    }

    private boolean hasFloorHideLayer() { return map != null && map.getLayers().get("Floor_Hide") != null; }

    public Rectangle getSpawnPoint(String fromMap) {
        if (map == null || fromMap == null) return null;
        MapLayer spawnLayer = map.getLayers().get("SpawnPoints");
        if (spawnLayer == null) return null;

        String normalizedFromMap = normalizeMapName(fromMap);
        for (MapObject obj : spawnLayer.getObjects()) {
            if (obj.getName() != null && normalizedFromMap.equals(normalizeMapName(obj.getName()))) return objectToRectangle(obj);
        }

        System.out.println("WARNING: Khong tim thay spawn tu map " + fromMap + " trong SpawnPoints cua map " + currentMapName);
        return null;
    }

    public Rectangle getPlayerSpawn() {
        if (map == null) return null;
        String[] layerNames = {"Player_spawn", "Player_Spawn", "player_spawn", "SpawnPoints"};
        String[] objectNames = {"player_spawn", "Player_Spawn", "Player_spawn"};

        for (String layerName : layerNames) {
            MapLayer layer = map.getLayers().get(layerName);
            if (layer == null) continue;
            for (String objectName : objectNames) {
                Rectangle spawn = getNamedObjectAsRectangle(layer, objectName);
                if (spawn != null) return spawn;
            }
        }
        return null;
    }

    private Rectangle getNamedObjectAsRectangle(MapLayer layer, String objectName) {
        for (MapObject obj : layer.getObjects()) if (objectName.equals(obj.getName())) return objectToRectangle(obj);
        return null;
    }

    private Rectangle objectToRectangle(MapObject obj) {
        if (obj instanceof RectangleMapObject) return ((RectangleMapObject) obj).getRectangle();
        if (obj instanceof PointMapObject) {
            float x = ((PointMapObject) obj).getPoint().x;
            float y = ((PointMapObject) obj).getPoint().y;
            return new Rectangle(x, y, 32, 32);
        }
        return null;
    }

    private String normalizeMapName(String name) {
        if (name == null) return "";
        return name.replace("\\", "/").replace("map/", "").trim().toLowerCase();
    }

    // =========================================================
    // RENDER
    // =========================================================

    public void render(OrthographicCamera camera, boolean isMasked) { renderBackground(camera, isMasked); renderForeground(camera, isMasked); }

    public void renderBackground(OrthographicCamera camera, boolean isMasked) {
        if (renderer == null || map == null) return;
        renderer.setView(camera);

        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) {
            if (!layer.isVisible() || !(layer instanceof TiledMapTileLayer)) continue;
            String n = layer.getName();
            if (n.equals("Queen_Blood") && !isMasked) continue;
            if (n.equals("Blood") && isMasked) continue;
            if (!n.equals("Overhead") && !n.equals("Small_Cocon")) renderer.renderTileLayer((TiledMapTileLayer) layer);
        }
        renderer.getBatch().end();

        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) {
            if (!layer.isVisible() || layer instanceof TiledMapTileLayer) continue;
            String n = layer.getName();
            if (!n.contains("Collision") && !n.contains("spawn") && !n.contains("Spawn")
                && !n.equals("SpawnPoints") && !n.equals("Overhead") && !n.equals("Guards")
                && !n.equals("Pushable") && !n.equals("Small_Cocoon") && !n.equals("Keys")
                && !n.equals("Hide_Trigger") && !n.contains("Interact_Point_0")) renderObjectLayer(layer);
        }
        renderer.getBatch().end();
    }

    public void renderForeground(OrthographicCamera camera, boolean isMasked) {
        if (renderer == null || map == null) return;
        renderer.setView(camera);

        MapLayer overhead = map.getLayers().get("Overhead");
        if (overhead != null && overhead.isVisible()) {
            String n = overhead.getName();
            if (n.equals("Queen_Blood") && !isMasked) return;
            if (n.equals("Blood") && isMasked) return;
            renderer.getBatch().begin();
            if (overhead instanceof TiledMapTileLayer) renderer.renderTileLayer((TiledMapTileLayer) overhead); else renderObjectLayer(overhead);
            renderer.getBatch().end();
        }

        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) if (layer.getName().contains("Interact_Point_0")) renderObjectLayer(layer);
        renderer.getBatch().end();
    }

    public void renderInteractPoints(OrthographicCamera camera) {
        if (renderer == null || map == null) return;
        renderer.setView(camera);
        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) if (layer.getName().contains("Interact")) renderObjectLayer(layer);
        renderer.getBatch().end();
    }

    private void renderObjectLayer(MapLayer layer) {
        AnimatedTiledMapTile.updateAnimationBaseTime();
        for (MapObject obj : layer.getObjects()) {
            if (!(obj.isVisible() && obj instanceof TiledMapTileMapObject)) continue;
            TiledMapTileMapObject tObj = (TiledMapTileMapObject) obj;
            renderer.getBatch().draw(
                tObj.getTile().getTextureRegion(), tObj.getX(), tObj.getY() - 32,
                tObj.getOriginX(), tObj.getOriginY(),
                tObj.getTextureRegion().getRegionWidth(), tObj.getTextureRegion().getRegionHeight(),
                tObj.getScaleX(), tObj.getScaleY(), tObj.getRotation()
            );
        }
    }

    // =========================================================
    // CHEST / LAYER VISIBILITY
    // =========================================================

    public void showLibraryChestWithKey() {
        if (!"Library.tmx".equalsIgnoreCase(currentMapName)) return;
        setLayerVisibleSafe("Chest_Close", false); setLayerVisibleSafe("Chest_Open", false);
        setLayerVisibleSafe("Chest_Open_Key", true); setLayerVisibleSafe("Chest_Open_No_Key", false);
    }

    public void showLibraryChestNoKey() {
        if (!"Library.tmx".equalsIgnoreCase(currentMapName)) return;
        setLayerVisibleSafe("Chest_Close", false); setLayerVisibleSafe("Chest_Open", false);
        setLayerVisibleSafe("Chest_Open_Key", false); setLayerVisibleSafe("Chest_Open_No_Key", true);
    }

    private void hideDoorObjectInLayer(String layerName, String doorName) {
        if (map == null || doorName == null) return;
        MapLayer layer = map.getLayers().get(layerName);
        if (layer == null) return;
        for (MapObject obj : layer.getObjects()) if (doorName.equals(obj.getName())) obj.setVisible(false);
    }

    private void setLayerVisibleSafe(String layerName, boolean visible) {
        if (map == null) return;
        MapLayer layer = map.getLayers().get(layerName);
        if (layer != null) layer.setVisible(visible);
    }

    private TiledMapTileLayer getTileLayerSafe(String name) {
        if (map == null) return null;
        MapLayer layer = map.getLayers().get(name);
        return (layer instanceof TiledMapTileLayer) ? (TiledMapTileLayer) layer : null;
    }

    // =========================================================
    // BOSS / GUARD PATH
    // =========================================================

    private void loadBossLayer(MapLayer layer) {
        Array<Vector2> path = null;
        Float bossX = null, bossY = null;

        for (MapObject obj : layer.getObjects()) {
            Array<Vector2> objectPath = readPath(obj);
            if (objectPath.size > 0) { path = objectPath; if (bossX == null) { bossX = objectPath.get(0).x; bossY = objectPath.get(0).y; } continue; }
            if (obj instanceof RectangleMapObject) { Rectangle r = ((RectangleMapObject) obj).getRectangle(); bossX = r.x; bossY = r.y; }
            else if (obj instanceof PointMapObject) { bossX = ((PointMapObject) obj).getPoint().x; bossY = ((PointMapObject) obj).getPoint().y; }
            else if (obj instanceof TiledMapTileMapObject) { TiledMapTileMapObject t = (TiledMapTileMapObject) obj; bossX = t.getX(); bossY = t.getY() - 32f; }
        }

        if (path != null && path.size > 0) bosses.add(new Boss(0, 0, path));
        else if (bossX != null && bossY != null) bosses.add(new Boss(bossX, bossY, null));
    }

    private Array<Vector2> readPath(MapObject obj) {
        Array<Vector2> path = new Array<>();
        if (obj instanceof PolylineMapObject) {
            Polyline polyline = ((PolylineMapObject) obj).getPolyline();
            float[] v = polyline.getTransformedVertices();
            for (int i = 0; i < v.length; i += 2) path.add(new Vector2(v[i], v[i + 1]));
        } else if (obj instanceof PolygonMapObject) {
            Polygon polygon = ((PolygonMapObject) obj).getPolygon();
            float[] v = polygon.getTransformedVertices();
            for (int i = 0; i < v.length; i += 2) path.add(new Vector2(v[i], v[i + 1]));
        }
        return path;
    }

    // =========================================================
    // MUSIC
    // =========================================================

    private void playMusicForCurrentMap() {
        if (currentMapName.equals("Library.tmx") || currentMapName.equals("Hidden_Room.tmx")
            || currentMapName.equals("Old_Corridor.tmx") || currentMapName.equals("Old_Chapel.tmx")
            || currentMapName.equals("Exit_Chamber.tmx")) {
            AudioManager.getInstance().playBackgroundMusic("audio/Save_Room_Theme.ogg", 0.4f);
        } else if (currentMapName.equals("Ritual_Chamber.tmx") || currentMapName.equals("Queen_Chamber.tmx")
            || currentMapName.equals("Corridor.tmx")) {
            AudioManager.getInstance().playBackgroundMusic("audio/The_Gauntlet.ogg", 0.2f);
        } else {
            AudioManager.getInstance().playBackgroundMusic("audio/The_Gauntlet.ogg", 0.1f);
        }
    }

    // =========================================================
    // COLLISION API
    // =========================================================

    public boolean isColliding(Rectangle entityRect) { return MapCollisionHelper.isColliding(entityRect, wallCollision, doorObjects, pushables); }
    public Array<Rectangle> getFullCollision() { return MapCollisionHelper.getFullCollision(wallCollision, doorObjects, pushables); }
    public PushableBlock getCollidingPushable(Rectangle rect) { return MapCollisionHelper.getCollidingPushable(rect, pushables); }
    public Array<Rectangle> getWallCollision() { return wallCollision; }
    public Array<Rectangle> getSkullCollision() { return skullCollision; }
    public Array<Rectangle> getStoneCollision() { return stoneCollision; }
    public Array<RectangleMapObject> getDoorObjects() { return doorObjects; }

    // =========================================================
    // GENERAL GETTERS
    // =========================================================

    public String getCurrentMapName() { return currentMapName; }
    public String getLastMapName() { return lastMapName; }
    public TiledMap getMap() { return map; }
    public Array<RectangleMapObject> getInteractPoints() { return interactPoints; }
    public Array<PushableBlock> getPushables() { return pushables; }
    public Array<Key> getKeys() { return keys; }
    public Array<Door> getDoors() { return doors; }
    public Rectangle getHideTrigger() { return hideTrigger; }

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

    // =========================================================
    // DISPOSE
    // =========================================================

    public void dispose() {
        AudioManager.getInstance().stopBackgroundMusic();
        if (map != null) map.dispose();
        if (renderer != null) renderer.dispose();
    }
}
