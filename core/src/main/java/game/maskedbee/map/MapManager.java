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

/**
 * Chỉ chịu trách nhiệm:
 *  - Load / unload TiledMap
 *  - Render background / foreground / object layers
 *  - Giữ data collision (wallCollision, doorObjects...)
 *  - Expose getters cho các hệ thống khác
 *
 * State persistence  → MapStateManager  (stateful, giữ currentMapName)
 * Collision logic    → MapCollisionHelper (stateless, tất cả hàm static)
 */
public class MapManager {

    // =========================================================
    // Dependencies
    // =========================================================
    private final MapStateManager state = new MapStateManager();

    // =========================================================
    // Map / Renderer
    // =========================================================
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    private String currentMapName = "";
    private String lastMapName    = "";

    // =========================================================
    // Collision data — MapManager sở hữu, MapCollisionHelper tính toán
    // =========================================================
    private final Array<Rectangle>          wallCollision  = new Array<>();
    private final Array<Rectangle>          stoneCollision = new Array<>();
    private final Array<Rectangle>          skullCollision = new Array<>();
    private final Array<RectangleMapObject> doorObjects    = new Array<>();

    // =========================================================
    // Object lists
    // =========================================================
    private final Array<MapObject>          portalObjects  = new Array<>();
    private final Array<RectangleMapObject> interactPoints = new Array<>();

    private final Array<PushableBlock> pushables = new Array<>();
    private final Array<Key>           keys      = new Array<>();
    private final Array<Door>          doors     = new Array<>();

    public final Array<Spike>  spikes  = new Array<>();
    public final Array<Lever>  levers  = new Array<>();
    public final Array<Guard>  guards  = new Array<>();
    public final Array<Boss>   bosses  = new Array<>();

    private Rectangle         hideTrigger;
    private TiledMapTileLayer doorCloseLayer;
    private TiledMapTileLayer doorOpenLayer;

    private boolean wasFloorHideTriggered = false;

    // =========================================================
    // LOAD MAP
    // =========================================================

    public void loadMap(String fileName) {
        try {
            lastMapName    = currentMapName;
            currentMapName = fileName.replace("map/", "");

            if (map != null)      map.dispose();
            if (renderer != null) renderer.dispose();

            map      = new TmxMapLoader().load(fileName);
            renderer = new OrthogonalTiledMapRenderer(map);

            // Reset collision data
            wallCollision.clear();
            stoneCollision.clear();
            skullCollision.clear();
            doorObjects.clear();

            // Reset object lists
            portalObjects.clear();
            interactPoints.clear();
            pushables.clear();
            keys.clear();
            doors.clear();
            spikes.clear();
            levers.clear();
            guards.clear();
            bosses.clear();
            hideTrigger = null;

            doorCloseLayer = getTileLayerSafe("Door_Close");
            doorOpenLayer  = getTileLayerSafe("Door_Open");

            // Cập nhật map name cho state manager
            state.setCurrentMapName(currentMapName);

            // Parse tất cả layer
            for (MapLayer layer : map.getLayers()) {
                if (layer != null) parseLayer(layer);
            }

            resetFloorHideToClosed();

            // Áp lại trạng thái đã lưu
            state.applyOpenedDoors(state.currentMapPrefix(), this::applyDoorOpened);
            if (state.isHiddenRoomOpened()) applyHiddenRoomOpened();
            state.applyCollectedKeys(state.currentMapPrefix(), this::hideCollectedKey);
            state.applyPushablePositions(pushables);
            state.applySpikeLeverStates(spikes, levers, map);

            updateFloorHide(null);

            System.out.println("✅ Loaded map: " + fileName);
            playMusicForCurrentMap();
        } catch (Exception e) {
            Gdx.app.error("MapManager", "Error loading map: " + fileName, e);
        }
    }

    private void parseLayer(MapLayer layer) {
        String name = layer.getName();

        switch (name) {
            case "Skull_Collision":
                for (MapObject obj : layer.getObjects()) {
                    if (obj instanceof RectangleMapObject) {
                        Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                        skullCollision.add(rect);
                        wallCollision.add(rect);
                    }
                }
                break;

            case "Boss": case "Bosses":
                loadBossLayer(layer);
                break;

            case "Stone_Collision":
                for (MapObject obj : layer.getObjects()) {
                    if (obj instanceof RectangleMapObject) {
                        Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                        wallCollision.add(rect);
                        stoneCollision.add(rect);
                    }
                }
                break;

            case "Jail_Door_Collision":
                for (MapObject obj : layer.getObjects()) {
                    if (obj instanceof RectangleMapObject) {
                        RectangleMapObject r = (RectangleMapObject) obj;
                        if (r.getName() == null || r.getName().isEmpty())
                            r.setName("jail_door_hitbox");
                        doorObjects.add(r);
                    }
                }
                break;

            case "Chest_Collision":
                for (MapObject obj : layer.getObjects()) {
                    if (obj instanceof RectangleMapObject) {
                        RectangleMapObject r = (RectangleMapObject) obj;
                        if (r.getName() == null || r.getName().isEmpty())
                            r.setName("gold_key_chest_block");
                        doorObjects.add(r);
                    }
                }
                break;

            case "Doors":
                for (MapObject obj : layer.getObjects()) {
                    if (obj instanceof RectangleMapObject) {
                        RectangleMapObject r = (RectangleMapObject) obj;
                        doorObjects.add(r);
                        doors.add(new Door(r));
                    }
                }
                break;

            case "Pushable":
                parsePushableLayer(layer);
                break;

            case "Hide_Trigger":
                for (MapObject obj : layer.getObjects()) {
                    if (obj instanceof RectangleMapObject)
                        hideTrigger = ((RectangleMapObject) obj).getRectangle();
                }
                break;

            case "Keys":
                for (MapObject obj : layer.getObjects()) {
                    if (obj instanceof RectangleMapObject)
                        keys.add(new Key((RectangleMapObject) obj));
                }
                break;

            case "Spikes":
                for (MapObject obj : layer.getObjects()) {
                    if (obj instanceof TiledMapTileMapObject)
                        spikes.add(new Spike((TiledMapTileMapObject) obj));
                }
                break;

            case "Switch":
                for (MapObject obj : layer.getObjects()) {
                    if (obj instanceof TiledMapTileMapObject)
                        levers.add(new Lever((TiledMapTileMapObject) obj));
                }
                break;

            case "Guards":
                for (MapObject obj : layer.getObjects()) {
                    Array<Vector2> path = readPath(obj);
                    if (path.size > 0)
                        guards.add(new Guard(path.get(0).x - 16f, path.get(0).y - 20f, path));
                }
                break;

            default:
                if (name.contains("Collision")) {
                    parseGenericCollisionLayer(layer);
                } else if (name.equals("Exit") || name.contains("_Chamber") || name.contains("Corridor")) {
                    for (MapObject obj : layer.getObjects()) portalObjects.add(obj);
                } else if (name.contains("Interact")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject)
                            interactPoints.add((RectangleMapObject) obj);
                    }
                }
                break;
        }
    }

    private void parseGenericCollisionLayer(MapLayer layer) {
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

    private void parsePushableLayer(MapLayer layer) {
        int idx = 0;
        for (MapObject obj : layer.getObjects()) {
            if (obj instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                Rectangle fixed = new Rectangle(
                    (float) Math.floor(rect.x / 32f) * 32f,
                    (float) Math.floor(rect.y / 32f) * 32f,
                    32f, 32f
                );
                String id = obj.getName();
                if (id == null || id.isEmpty())
                    id = "pushable_" + Math.round(fixed.x) + "_" + Math.round(fixed.y) + "_" + idx;
                pushables.add(new PushableBlock(fixed, id));
                idx++;
            }
        }
    }

    // =========================================================
    // DOOR LOGIC
    // =========================================================

    public void openDoor(String doorName) {
        if (doorName == null) return;

        if (doorName.toLowerCase().contains("hidden")) {
            openHiddenRoom();
            return;
        }

        if (!state.isDoorOpened(doorName)) {
            AudioManager.getInstance().playSoundEffect("audio/Door_Open.wav", 0.5f);
            state.markDoorOpened(doorName);
            applyDoorOpened(doorName);
            System.out.println("🚪 Door opened: " + doorName);
        }
    }

    private void applyDoorOpened(String doorName) {
        if (doorName == null) return;

        // Dùng MapCollisionHelper static để xóa door collision
        MapCollisionHelper.removeDoorByName(doorObjects, doorName);

        hideDoorObjectInLayer("Door",  doorName);
        hideDoorObjectInLayer("Doors", doorName);
        hideDoorObjectInLayer("Jail",  doorName);

        for (Door door : doors) {
            if (doorName.equals(door.getName())) {
                door.open();
                Rectangle b = door.getBounds();
                int tileX = Math.round(b.x / 32f);
                int tileY = Math.round(b.y / 32f);
                if (doorCloseLayer != null) {
                    for (int i = 0; i < 2; i++) doorCloseLayer.setCell(tileX, tileY + i, null);
                }
                if (doorOpenLayer != null) doorOpenLayer.setVisible(true);
                break;
            }
        }
    }

    // =========================================================
    // HIDDEN ROOM
    // =========================================================

    public void openHiddenRoom() {
        if (!state.isHiddenRoomOpened()) {
            AudioManager.getInstance().playSoundEffect("audio/Door_Open.wav", 0.5f);
        }
        state.markHiddenRoomOpened();
        state.saveProgressCheckpointHere(currentMapName);
        applyHiddenRoomOpened();
        System.out.println("✅ Hidden room opened");
    }

    private void applyHiddenRoomOpened() {
        if (map == null) return;
        setLayerVisibleSafe("Hide_Floor", false);

        MapLayer hiddenCollision = map.getLayers().get("Hidden_Room_Collision");
        if (hiddenCollision != null) {
            hiddenCollision.setVisible(false);
            for (MapObject obj : hiddenCollision.getObjects()) {
                if (obj instanceof RectangleMapObject) {
                    // Dùng MapCollisionHelper static để xóa wall
                    MapCollisionHelper.removeWallByRect(
                        wallCollision,
                        ((RectangleMapObject) obj).getRectangle()
                    );
                }
            }
        }
    }

    // =========================================================
    // KEYS
    // =========================================================

    public void markKeyCollected(String keyName) {
        if (keyName == null || keyName.isEmpty()) return;
        state.markKeyCollected(keyName);
        for (Key key : keys) {
            if (keyName.equals(key.getName())) { key.collect(); break; }
        }
    }

    private void hideCollectedKey(String keyName) {
        if (keyName == null) return;
        for (Key key : keys) {
            if (keyName.equals(key.getName())) { key.collect(); break; }
        }
        MapLayer keyLayer = map.getLayers().get("Keys");
        if (keyLayer != null) {
            for (MapObject obj : keyLayer.getObjects()) {
                if (keyName.equals(obj.getName())) obj.setVisible(false);
            }
        }
        if ("Library.tmx".equalsIgnoreCase(currentMapName) && "gold_key".equalsIgnoreCase(keyName)) {
            showLibraryChestNoKey();
        }
    }

    public boolean isKeyCollected(String keyName)    { return state.isKeyCollected(keyName); }
    public boolean isHiddenRoomOpened()              { return state.isHiddenRoomOpened(); }

    // =========================================================
    // PUZZLE
    // =========================================================

    public void markPuzzleStepSolved(String stepName) {
        state.markPuzzleStepSolved(stepName);
        state.saveProgressCheckpointHere(currentMapName);
        System.out.println("Puzzle step solved: " + currentMapName + " / " + stepName);
    }

    public boolean isPuzzleStepSolved(String stepName) { return state.isPuzzleStepSolved(stepName); }

    // =========================================================
    // PUSHABLES
    // =========================================================

    public void rememberPushableState(PushableBlock block) {
        state.savePushablePosition(block);
        AudioManager.getInstance().playSoundEffect("audio/Cocoon_Push.wav", 0.8f);
    }

    public void clearPushableStateForCurrentMap() {
        state.clearPushableStateForCurrentMap();
    }

    public void resetPushables() {
        state.clearPushableStateForCurrentMap();
        for (PushableBlock block : pushables) block.resetPosition();
        updateFloorHide(null);
    }

    // =========================================================
    // SPIKES & LEVERS
    // =========================================================

    public void rememberSpikeState(Spike spike) { state.saveSpikeState(spike); }

    public void rememberLeverState(Lever lever) {
        state.saveLeverState(lever);
        AudioManager.getInstance().playSoundEffect("audio/Lever_Sound.wav", 0.5f);
    }

    public void clearSpikeLeverStateForCurrentMap() { state.clearSpikeLeverStateForCurrentMap(); }

    // =========================================================
    // FLOOR HIDE / TRIGGER
    // =========================================================

    public void updateFloorHide(Player player) {
        if (map == null) return;
        MapLayer hideLayer = map.getLayers().get("Floor_Hide");
        if (hideLayer == null) return;
        if (hideTrigger == null) { hideLayer.setVisible(true); return; }

        boolean triggeredByPlayer   = player != null && player.hitbox.overlaps(hideTrigger);
        boolean triggeredByPushable = false;

        for (PushableBlock block : pushables) {
            Rectangle b = block.getBounds();
            if (hideTrigger.contains(b.x + b.width / 2f, b.y + b.height / 2f)) {
                triggeredByPushable = true;
                break;
            }
        }

        boolean triggered = triggeredByPlayer || triggeredByPushable;
        hideLayer.setVisible(!triggered);

        if (triggered && !wasFloorHideTriggered) {
            AudioManager.getInstance().playSoundEffect("audio/Door_Open.wav", 0.6f);
        }
        wasFloorHideTriggered = triggered;

        if (triggeredByPushable) {
            NotificationManager.getInstance().show("Giải đố thành công\nĐã mở ra lối đi bí mật");
            NotificationManager.getInstance().show("Checkpoint mới được lưu thành Disposal");
            state.saveProgressCheckpointHere(currentMapName);
        }
    }

    private void resetFloorHideToClosed() {
        if (map == null) return;
        MapLayer hideLayer = map.getLayers().get("Floor_Hide");
        if (hideLayer != null) hideLayer.setVisible(true);
    }

    // =========================================================
    // PORTAL
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
                for (RectangleMapObject door : doorObjects) {
                    if (rect.overlaps(door.getRectangle())) return null;
                }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                if (dest != null && dest.endsWith(".tmx")) return "map/" + dest;
            }
        }
        return null;
    }

    private boolean isFloorHideOpened() {
        MapLayer layer = map == null ? null : map.getLayers().get("Floor_Hide");
        return layer != null && !layer.isVisible();
    }

    private boolean hasFloorHideLayer() {
        return map != null && map.getLayers().get("Floor_Hide") != null;
    }

    // =========================================================
    // SPAWN POINTS
    // =========================================================

    public Rectangle getSpawnPoint(String fromMap) {
        if (map == null || fromMap == null) return null;
        MapLayer spawnLayer = map.getLayers().get("SpawnPoints");
        if (spawnLayer == null) return null;

        String normalized = normalizeMapName(fromMap);
        for (MapObject obj : spawnLayer.getObjects()) {
            if (obj.getName() == null) continue;
            if (!normalized.equals(normalizeMapName(obj.getName()))) continue;
            if (obj instanceof RectangleMapObject) return ((RectangleMapObject) obj).getRectangle();
            if (obj instanceof PointMapObject) {
                float x = ((PointMapObject) obj).getPoint().x;
                float y = ((PointMapObject) obj).getPoint().y;
                return new Rectangle(x, y, 32, 32);
            }
        }
        System.out.println("WARNING: Khong tim thay spawn tu map " + fromMap);
        return null;
    }

    public Rectangle getPlayerSpawn() {
        if (map == null) return null;
        String[] layerNames  = { "Player_spawn", "Player_Spawn", "player_spawn", "SpawnPoints" };
        String[] objectNames = { "player_spawn", "Player_Spawn", "Player_spawn" };
        for (String ln : layerNames) {
            for (String on : objectNames) {
                Rectangle r = getPlayerSpawnFromLayer(ln, on);
                if (r != null) return r;
            }
        }
        return null;
    }

    private Rectangle getPlayerSpawnFromLayer(String layerName, String objectName) {
        MapLayer layer = map.getLayers().get(layerName);
        if (layer == null) return null;
        for (MapObject obj : layer.getObjects()) {
            if (!objectName.equals(obj.getName())) continue;
            if (obj instanceof RectangleMapObject) return ((RectangleMapObject) obj).getRectangle();
            if (obj instanceof PointMapObject) {
                float x = ((PointMapObject) obj).getPoint().x;
                float y = ((PointMapObject) obj).getPoint().y;
                return new Rectangle(x, y, 32, 32);
            }
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

    public void render(OrthographicCamera camera, boolean isMasked) {
        renderBackground(camera, isMasked);
        renderForeground(camera, isMasked);
    }

    public void renderBackground(OrthographicCamera camera, boolean isMasked) {
        if (renderer == null || map == null) return;
        renderer.setView(camera);

        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) {
            if (!layer.isVisible() || !(layer instanceof TiledMapTileLayer)) continue;
            String n = layer.getName();
            if (n.equals("Queen_Blood") && !isMasked) continue;
            if (n.equals("Blood") && isMasked) continue;
            if (!n.equals("Overhead") && !n.equals("Small_Cocon")) {
                renderer.renderTileLayer((TiledMapTileLayer) layer);
            }
        }
        renderer.getBatch().end();

        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) {
            if (!layer.isVisible() || layer instanceof TiledMapTileLayer) continue;
            String n = layer.getName();
            if (!n.contains("Collision") && !n.contains("spawn") && !n.contains("Spawn")
                && !n.equals("SpawnPoints") && !n.equals("Overhead") && !n.equals("Guards")
                && !n.equals("Pushable") && !n.equals("Small_Cocoon") && !n.equals("Keys")
                && !n.equals("Hide_Trigger") && !n.contains("Interact_Point_0")) {
                renderObjectLayer(layer);
            }
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
            if (overhead instanceof TiledMapTileLayer) renderer.renderTileLayer((TiledMapTileLayer) overhead);
            else renderObjectLayer(overhead);
            renderer.getBatch().end();
        }

        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) {
            if (layer.getName().contains("Interact_Point_0")) renderObjectLayer(layer);
        }
        renderer.getBatch().end();
    }

    public void renderInteractPoints(OrthographicCamera camera) {
        if (renderer == null || map == null) return;
        renderer.setView(camera);
        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) {
            if (layer.getName().contains("Interact")) renderObjectLayer(layer);
        }
        renderer.getBatch().end();
    }

    private void renderObjectLayer(MapLayer layer) {
        AnimatedTiledMapTile.updateAnimationBaseTime();
        for (MapObject obj : layer.getObjects()) {
            if (obj.isVisible() && obj instanceof TiledMapTileMapObject) {
                TiledMapTileMapObject tObj = (TiledMapTileMapObject) obj;
                renderer.getBatch().draw(
                    tObj.getTile().getTextureRegion(),
                    tObj.getX(), tObj.getY() - 32,
                    tObj.getOriginX(), tObj.getOriginY(),
                    tObj.getTextureRegion().getRegionWidth(),
                    tObj.getTextureRegion().getRegionHeight(),
                    tObj.getScaleX(), tObj.getScaleY(), tObj.getRotation()
                );
            }
        }
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
    // BOSS / GUARD PATH
    // =========================================================

    private void loadBossLayer(MapLayer layer) {
        Array<Vector2> path = null;
        Float bossX = null, bossY = null;

        for (MapObject obj : layer.getObjects()) {
            Array<Vector2> objPath = readPath(obj);
            if (objPath.size > 0) {
                path = objPath;
                if (bossX == null) { bossX = objPath.get(0).x; bossY = objPath.get(0).y; }
                continue;
            }
            if (obj instanceof RectangleMapObject) {
                Rectangle r = ((RectangleMapObject) obj).getRectangle();
                bossX = r.x; bossY = r.y;
            } else if (obj instanceof PointMapObject) {
                bossX = ((PointMapObject) obj).getPoint().x;
                bossY = ((PointMapObject) obj).getPoint().y;
            } else if (obj instanceof TiledMapTileMapObject) {
                TiledMapTileMapObject t = (TiledMapTileMapObject) obj;
                bossX = t.getX(); bossY = t.getY() - 32f;
            }
        }

        if (path != null && path.size > 0) bosses.add(new Boss(0, 0, path));
        else if (bossX != null && bossY != null) bosses.add(new Boss(bossX, bossY, null));
    }

    private Array<Vector2> readPath(MapObject obj) {
        Array<Vector2> path = new Array<>();
        if (obj instanceof PolylineMapObject) {
            float[] v = ((PolylineMapObject) obj).getPolyline().getTransformedVertices();
            for (int i = 0; i < v.length; i += 2) path.add(new Vector2(v[i], v[i + 1]));
        } else if (obj instanceof PolygonMapObject) {
            float[] v = ((PolygonMapObject) obj).getPolygon().getTransformedVertices();
            for (int i = 0; i < v.length; i += 2) path.add(new Vector2(v[i], v[i + 1]));
        }
        return path;
    }

    // =========================================================
    // VISIBILITY HELPERS
    // =========================================================

    private void hideDoorObjectInLayer(String layerName, String doorName) {
        if (map == null || doorName == null) return;
        MapLayer layer = map.getLayers().get(layerName);
        if (layer == null) return;
        for (MapObject obj : layer.getObjects()) {
            if (doorName.equals(obj.getName())) obj.setVisible(false);
        }
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
    // CHEST (Library specific)
    // =========================================================

    public void showLibraryChestWithKey() {
        if (!"Library.tmx".equalsIgnoreCase(currentMapName)) return;
        setLayerVisibleSafe("Chest_Close",       false);
        setLayerVisibleSafe("Chest_Open",        false);
        setLayerVisibleSafe("Chest_Open_Key",    true);
        setLayerVisibleSafe("Chest_Open_No_Key", false);
    }

    public void showLibraryChestNoKey() {
        if (!"Library.tmx".equalsIgnoreCase(currentMapName)) return;
        setLayerVisibleSafe("Chest_Close",       false);
        setLayerVisibleSafe("Chest_Open",        false);
        setLayerVisibleSafe("Chest_Open_Key",    false);
        setLayerVisibleSafe("Chest_Open_No_Key", true);
    }

    // =========================================================
    // COLLISION GETTERS — truyền data vào MapCollisionHelper static
    // =========================================================

    public boolean isColliding(Rectangle entityRect) {
        return MapCollisionHelper.isColliding(entityRect, wallCollision, doorObjects, pushables);
    }

    public Array<Rectangle> getFullCollision() {
        return MapCollisionHelper.getFullCollision(wallCollision, doorObjects, pushables);
    }

    public PushableBlock getCollidingPushable(Rectangle rect) {
        return MapCollisionHelper.getCollidingPushable(rect, pushables);
    }

    // Raw data getters — cho Guard, Boss, PuzzleLibrary dùng trực tiếp
    public Array<Rectangle>          getWallCollision()  { return wallCollision; }
    public Array<Rectangle>          getStoneCollision() { return stoneCollision; }
    public Array<Rectangle>          getSkullCollision() { return skullCollision; }
    public Array<RectangleMapObject> getDoorObjects()    { return doorObjects; }

    // =========================================================
    // STATE GETTERS — delegate sang MapStateManager
    // =========================================================

    public boolean hasProgressCheckpoint()        { return state.hasProgressCheckpoint(); }
    public String  getProgressCheckpointMapName() { return state.getProgressCheckpointMapName(); }
    public void    clearProgressCheckpoint()      { state.clearProgressCheckpoint(); }
    public void    clearAllProgressState()        { state.clearAllProgressState(); }
    public void    saveProgressCheckpointHere()   { state.saveProgressCheckpointHere(currentMapName); }

    // =========================================================
    // GENERAL GETTERS
    // =========================================================

    public String               getCurrentMapName()  { return currentMapName; }
    public String               getLastMapName()     { return lastMapName; }
    public TiledMap             getMap()             { return map; }
    public Array<PushableBlock> getPushables()       { return pushables; }
    public Array<Key>           getKeys()            { return keys; }
    public Array<Door>          getDoors()           { return doors; }
    public Rectangle            getHideTrigger()     { return hideTrigger; }
    public Array<RectangleMapObject> getInteractPoints() { return interactPoints; }

    public float getMapWidth() {
        int w = map.getProperties().get("width",     Integer.class);
        int t = map.getProperties().get("tilewidth", Integer.class);
        return w * t;
    }

    public float getMapHeight() {
        int h = map.getProperties().get("height",     Integer.class);
        int t = map.getProperties().get("tileheight", Integer.class);
        return h * t;
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
        if (map != null)      map.dispose();
        if (renderer != null) renderer.dispose();
    }
}
