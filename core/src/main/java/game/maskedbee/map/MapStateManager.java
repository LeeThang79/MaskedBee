package game.maskedbee.map;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import game.maskedbee.objects.Door;
import game.maskedbee.objects.Key;
import game.maskedbee.objects.Lever;
import game.maskedbee.objects.PushableBlock;
import game.maskedbee.objects.Spike;
import game.maskedbee.main.AudioManager;

/**
 * Quản lý toàn bộ trạng thái trong phiên chơi:
 * - Cửa đã mở, phòng ẩn đã mở, chìa khóa đã thu
 * - Vị trí pushable block, trạng thái spike/lever
 * - Progress checkpoint
 */
public class MapStateManager {

    private String currentMapName = "";

    // --- Trạng thái bền vững trong phiên chơi ---
    private final ObjectSet<String> openedDoors         = new ObjectSet<>();
    private final ObjectSet<String> openedHiddenRooms   = new ObjectSet<>();
    private final ObjectSet<String> collectedKeys       = new ObjectSet<>();
    private final ObjectSet<String> solvedPuzzleSteps   = new ObjectSet<>();

    // --- Trạng thái động ---
    private final ObjectMap<String, Vector2>  savedPushablePositions = new ObjectMap<>();
    private final ObjectMap<String, Boolean>  savedSpikeStates       = new ObjectMap<>();
    private final ObjectMap<String, Boolean>  savedLeverStates       = new ObjectMap<>();

    // --- Progress checkpoint ---
    private boolean hasProgressCheckpoint      = false;
    private String  progressCheckpointMapName  = null;

    // =========================================================
    // Helpers: tạo key duy nhất cho từng đối tượng
    // =========================================================

    public void setCurrentMapName(String name) {
        this.currentMapName = (name != null) ? name : "";
    }

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

    // =========================================================
    // DOORS
    // =========================================================

    public boolean isDoorOpened(String doorName) {
        return openedDoors.contains(stateKey(doorName));
    }

    public void markDoorOpened(String doorName) {
        openedDoors.add(stateKey(doorName));
    }

    /** Áp lại trạng thái tất cả cửa đã mở cho map hiện tại. */
    public void applyOpenedDoors(String prefix, DoorApplier applier) {
        for (String key : openedDoors) {
            if (key.startsWith(prefix)) {
                String doorName = key.substring(prefix.length());
                applier.apply(doorName);
            }
        }
    }

    /** Callback interface để MapManager tự xử lý logic mở cửa. */
    public interface DoorApplier {
        void apply(String doorName);
    }

    // =========================================================
    // HIDDEN ROOMS
    // =========================================================

    public boolean isHiddenRoomOpened() {
        return openedHiddenRooms.contains(stateKey("hidden_room"));
    }

    public void markHiddenRoomOpened() {
        openedHiddenRooms.add(stateKey("hidden_room"));
    }

    // =========================================================
    // KEYS
    // =========================================================

    public boolean isKeyCollected(String keyName) {
        if (keyName == null || keyName.isEmpty()) return false;
        return collectedKeys.contains(stateKey(keyName));
    }

    public void markKeyCollected(String keyName) {
        if (keyName == null || keyName.isEmpty()) return;
        collectedKeys.add(stateKey(keyName));
    }

    /** Áp lại chìa khóa đã thu cho map hiện tại. */
    public void applyCollectedKeys(String prefix, KeyApplier applier) {
        for (String key : collectedKeys) {
            if (key.startsWith(prefix)) {
                String keyName = key.substring(prefix.length());
                applier.apply(keyName);
            }
        }
    }

    public interface KeyApplier {
        void apply(String keyName);
    }

    // =========================================================
    // PUZZLE STEPS
    // =========================================================

    public boolean isPuzzleStepSolved(String stepName) {
        if (stepName == null || stepName.isEmpty()) return false;
        return solvedPuzzleSteps.contains(stateKey(stepName));
    }

    public void markPuzzleStepSolved(String stepName) {
        if (stepName == null || stepName.isEmpty()) return;
        solvedPuzzleSteps.add(stateKey(stepName));
    }

    // =========================================================
    // PUSHABLE BLOCKS
    // =========================================================

    public void savePushablePosition(PushableBlock block) {
        if (block == null) return;
        Rectangle b = block.getBounds();
        savedPushablePositions.put(pushableStateKey(block), new Vector2(b.x, b.y));
    }

    public void applyPushablePositions(Array<PushableBlock> pushables) {
        for (PushableBlock block : pushables) {
            Vector2 saved = savedPushablePositions.get(pushableStateKey(block));
            if (saved != null) {
                block.setPosition(saved.x, saved.y);
            }
        }
    }

    public void clearPushableStateForCurrentMap() {
        String prefix = currentMapName + ":pushable:";
        Array<String> toRemove = new Array<>();
        for (String key : savedPushablePositions.keys()) {
            if (key.startsWith(prefix)) toRemove.add(key);
        }
        for (String key : toRemove) savedPushablePositions.remove(key);
    }

    // =========================================================
    // SPIKES & LEVERS
    // =========================================================

    public void saveSpikeState(Spike spike) {
        if (spike == null || spike.mapObject == null) return;
        savedSpikeStates.put(tileObjectStateKey("spike", spike.mapObject), spike.isUp);
    }

    public void saveLeverState(Lever lever) {
        if (lever == null || lever.mapObject == null) return;
        savedLeverStates.put(tileObjectStateKey("lever", lever.mapObject), lever.isPulled);
    }

    public void applySpikeLeverStates(Array<Spike> spikes, Array<Lever> levers, TiledMap map) {
        for (Spike spike : spikes) {
            if (spike == null || spike.mapObject == null) continue;
            Boolean saved = savedSpikeStates.get(tileObjectStateKey("spike", spike.mapObject));
            if (saved != null && spike.isUp != saved) {
                spike.toggle(map);
            }
        }
        for (Lever lever : levers) {
            if (lever == null || lever.mapObject == null) continue;
            Boolean saved = savedLeverStates.get(tileObjectStateKey("lever", lever.mapObject));
            if (saved != null && lever.isPulled != saved) {
                lever.toggle(map);
            }
        }
    }

    public void clearSpikeLeverStateForCurrentMap() {
        clearByPrefix(savedSpikeStates, currentMapName + ":spike:");
        clearByPrefix(savedLeverStates, currentMapName + ":lever:");
    }

    private void clearByPrefix(ObjectMap<String, Boolean> map, String prefix) {
        Array<String> toRemove = new Array<>();
        for (String key : map.keys()) {
            if (key.startsWith(prefix)) toRemove.add(key);
        }
        for (String key : toRemove) map.remove(key);
    }

    // =========================================================
    // PROGRESS CHECKPOINT
    // =========================================================

    public void saveProgressCheckpointHere(String mapName) {
        if (mapName == null || mapName.isEmpty()) return;
        if (hasProgressCheckpoint && mapName.equals(progressCheckpointMapName)) return;

        hasProgressCheckpoint     = true;
        progressCheckpointMapName = mapName;
        System.out.println("Checkpoint saved at: " + progressCheckpointMapName);
    }

    public boolean hasProgressCheckpoint() {
        return hasProgressCheckpoint;
    }

    public String getProgressCheckpointMapName() {
        return progressCheckpointMapName;
    }

    public void clearProgressCheckpoint() {
        hasProgressCheckpoint     = false;
        progressCheckpointMapName = null;
    }

    // =========================================================
    // RESET TOÀN BỘ
    // =========================================================

    public void clearAllProgressState() {
        openedDoors.clear();
        openedHiddenRooms.clear();
        collectedKeys.clear();
        solvedPuzzleSteps.clear();
        savedPushablePositions.clear();
        savedSpikeStates.clear();
        savedLeverStates.clear();
        clearProgressCheckpoint();
    }

    // =========================================================
    // HELPER: prefix cho map hiện tại
    // =========================================================

    public String currentMapPrefix() {
        return currentMapName + ":";
    }
}
