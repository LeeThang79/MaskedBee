package game.maskedbee.map;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import game.maskedbee.objects.Lever;
import game.maskedbee.objects.PushableBlock;
import game.maskedbee.objects.Spike;

public class MapStateManager {

    private String currentMapName = "";

    // =========================================================
    // STATE DATA — CỬA / KEY / PUZZLE / CHECKPOINT
    // =========================================================

    private final ObjectSet<String> openedDoors = new ObjectSet<>();
    private final ObjectSet<String> openedHiddenRooms = new ObjectSet<>();
    private final ObjectSet<String> collectedKeys = new ObjectSet<>();
    private final ObjectSet<String> solvedPuzzleSteps = new ObjectSet<>();

    private boolean hasProgressCheckpoint = false;
    private String progressCheckpointMapName = null;

    // =========================================================
    // STATE DATA — PUSHABLE / SPIKE / LEVER
    // =========================================================

    private final ObjectMap<String, Vector2> savedPushablePositions = new ObjectMap<>();
    private final ObjectMap<String, Boolean> savedSpikeStates = new ObjectMap<>();
    private final ObjectMap<String, Boolean> savedLeverStates = new ObjectMap<>();

    // =========================================================
    // KEY HELPERS
    // =========================================================

    public void setCurrentMapName(String name) {
        currentMapName = (name != null) ? name : "";
    }

    public String currentMapPrefix() {
        return currentMapName + ":";
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
    // DOOR STATE
    // =========================================================

    public boolean isDoorOpened(String doorName) {
        if (doorName == null || doorName.isEmpty()) return false;
        return openedDoors.contains(stateKey(doorName));
    }

    public void markDoorOpened(String doorName) {
        if (doorName == null || doorName.isEmpty()) return;
        openedDoors.add(stateKey(doorName));
    }

    public void applyOpenedDoors(String prefix, DoorApplier applier) {
        if (applier == null) return;

        for (String key : openedDoors) {
            if (key.startsWith(prefix)) {
                String doorName = key.substring(prefix.length());
                applier.apply(doorName);
            }
        }
    }

    public interface DoorApplier {
        void apply(String doorName);
    }

    // =========================================================
    // HIDDEN ROOM STATE
    // =========================================================

    public boolean isHiddenRoomOpened() {
        return openedHiddenRooms.contains(stateKey("hidden_room"));
    }

    public void markHiddenRoomOpened() {
        openedHiddenRooms.add(stateKey("hidden_room"));
    }

    // =========================================================
    // KEY STATE
    // =========================================================

    public boolean isKeyCollected(String keyName) {
        if (keyName == null || keyName.isEmpty()) return false;
        return collectedKeys.contains(stateKey(keyName));
    }

    public void markKeyCollected(String keyName) {
        if (keyName == null || keyName.isEmpty()) return;
        collectedKeys.add(stateKey(keyName));
    }

    public void applyCollectedKeys(String prefix, KeyApplier applier) {
        if (applier == null) return;

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
    // PUZZLE STEP STATE
    // =========================================================

    public boolean isPuzzleStepSolved(String stepName) {
        if (stepName == null || stepName.isEmpty()) return false;
        return solvedPuzzleSteps.contains(stateKey(stepName));
    }

    public void markPuzzleStepSolved(String stepName) {
        if (stepName == null || stepName.isEmpty()) return;

        solvedPuzzleSteps.add(stateKey(stepName));
        saveProgressCheckpointHere();

        System.out.println("Puzzle step solved: " + currentMapName + " / " + stepName);
    }

    // =========================================================
    // CHECKPOINT STATE
    // =========================================================

    public void saveProgressCheckpointHere() {
        if (currentMapName == null || currentMapName.isEmpty()) return;

        if (hasProgressCheckpoint && currentMapName.equals(progressCheckpointMapName)) {
            return;
        }

        hasProgressCheckpoint = true;
        progressCheckpointMapName = currentMapName;

        System.out.println("Checkpoint saved at solved puzzle: " + progressCheckpointMapName);
    }

    public boolean hasProgressCheckpoint() {
        return hasProgressCheckpoint;
    }

    public String getProgressCheckpointMapName() {
        return progressCheckpointMapName;
    }

    public void clearProgressCheckpoint() {
        hasProgressCheckpoint = false;
        progressCheckpointMapName = null;
    }

    // =========================================================
    // PUSHABLE STATE
    // =========================================================

    public void rememberPushableState(PushableBlock block) {
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

    // =========================================================
    // SPIKE / LEVER STATE
    // =========================================================

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
    }

    public void applySpikeLeverStates(Array<Spike> spikes, Array<Lever> levers, TiledMap map) {
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
        clearBooleanMapByPrefix(savedSpikeStates, currentMapName + ":spike:");
        clearBooleanMapByPrefix(savedLeverStates, currentMapName + ":lever:");
    }

    private void clearBooleanMapByPrefix(ObjectMap<String, Boolean> map, String prefix) {
        Array<String> keysToRemove = new Array<>();

        for (String key : map.keys()) {
            if (key.startsWith(prefix)) {
                keysToRemove.add(key);
            }
        }

        for (String key : keysToRemove) {
            map.remove(key);
        }
    }

    // =========================================================
    // RESET ALL STATE
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
}
