package game.maskedbee.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.objects.PointMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.math.Vector2;
import game.maskedbee.entities.Guard;

import game.maskedbee.objects.Spike;
import game.maskedbee.objects.Lever;

public class MapManager {

    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    private final Array<Rectangle> wallCollision = new Array<>();
    private final Array<RectangleMapObject> doorObjects = new Array<>();
    private final Array<MapObject> portalObjects = new Array<>();

    private final Array<RectangleMapObject> interactPoints = new Array<>();

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

    public Array<Rectangle> getFullCollision() {
        Array<Rectangle> allHitboxes = new Array<>();
        allHitboxes.addAll(wallCollision);
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

            // Reset tất cả danh sách dữ liệu
            wallCollision.clear();
            doorObjects.clear();
            portalObjects.clear();
            spikes.clear(); // tai them
            levers.clear();
            guards.clear();

            interactPoints.clear();
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
                        if (obj instanceof RectangleMapObject)
                            doorObjects.add((RectangleMapObject) obj);
                    }
                }
                //    XỬ LÝ PORTAL CHUYỂN MAP
                else if (layerName.equals("Exit") || layerName.contains("_Chamber") || layerName.equals("Corridor")) {
                    for (MapObject obj : layer.getObjects())
                        portalObjects.add(obj);
                }
                // Quét tìm gai và cần gạt
                else if (layerName.equals("Spikes")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof TiledMapTileMapObject)
                            spikes.add(new Spike((TiledMapTileMapObject) obj));
                    }
                }
                else if (layerName.equals("Switch")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof TiledMapTileMapObject)
                            levers.add(new Lever((TiledMapTileMapObject) obj));
                    }
                } // Interact cua Nhi
                else if (layerName.contains("Interact")) {
                    for (MapObject obj : layer.getObjects()) {
                        if (obj instanceof RectangleMapObject) {
                            interactPoints.add((RectangleMapObject) obj);
                        }
                    }
                } // Goi Guards cua Thang
                else if (layerName.equals("Guards")) {
                    for (MapObject obj : layer.getObjects()) {
                        Array<Vector2> path = new Array<>();
                        // TRƯỜNG HỢP 1: Nếu bạn vẽ bằng công cụ Polyline (Đường zíc zắc/Đường thẳng hở)
                        if (obj instanceof PolylineMapObject) {
                            Polyline polyline = ((PolylineMapObject) obj).getPolyline();
                            float[] vertices = polyline.getTransformedVertices();
                            for (int i = 0; i < vertices.length; i += 2)
                                path.add(new Vector2(vertices[i], vertices[i + 1]));
                        }
                        // TRƯỜNG HỢP 2: Nếu bạn vẽ bằng công cụ Polygon (Hình đa giác, vuông khép kín)

                        else if (obj instanceof PolygonMapObject) {
                            com.badlogic.gdx.math.Polygon polygon = ((PolygonMapObject) obj).getPolygon();
                            float[] vertices = polygon.getTransformedVertices();
                            for (int i = 0; i < vertices.length; i += 2)
                                path.add(new Vector2(vertices[i], vertices[i + 1]));
                        }
                        if (path.size > 0) {
                            float startX = path.get(0).x - 16;
                            float startY = path.get(0).y - 20;
                            guards.add(new Guard(path.get(0).x - 16, path.get(0).y - 20, path));
                        }
                    }
                }
            }
            System.out.println("✅ Loaded map: " + fileName);
        } catch (Exception e) {
            Gdx.app.error("MapManager", "❌ Error loading map: " + fileName, e);
        }
    }
    public Rectangle getSpawnPoint(String fromMap) {
        if (map == null || fromMap == null) return null;

        // Tìm layer chứa các điểm xuất hiện cửa ngõ
        MapLayer spawnLayer = map.getLayers().get("SpawnPoints");
        if (spawnLayer == null) return null;
        for (MapObject obj : spawnLayer.getObjects()) {
            // Nếu tên Object trong Tiled trùng với tên map cũ (ví dụ: "corridor.tmx")
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
        if (map == null)
            return null;
        MapLayer layer = map.getLayers().get("Player_spawn");
        if (layer == null)
            return null;
        for (MapObject obj : layer.getObjects()) {
            if ("player_spawn".equals(obj.getName())) {
                if (obj instanceof RectangleMapObject)
                    return ((RectangleMapObject) obj).getRectangle();
                if (obj instanceof PointMapObject)
                    return new Rectangle(((PointMapObject) obj).getPoint().x, ((PointMapObject) obj).getPoint().y, 32,
                        32);
            }
        }
        return null;
    }
    // =========================================
    // LOGIC VẼ
    // =========================================

    public void renderBackground(OrthographicCamera camera) {
        if (renderer == null || map == null) return;
        renderer.setView(camera);
        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) {
            if (layer.isVisible() && layer instanceof com.badlogic.gdx.maps.tiled.TiledMapTileLayer) {
                if (!layer.getName().equals("Overhead")) {
                    renderer.renderTileLayer((com.badlogic.gdx.maps.tiled.TiledMapTileLayer) layer);
                }
            }
        }
        renderer.getBatch().end();
        // 2. Vẽ các Object Puzzle (Gai, Cần gạt, Cửa)
        renderer.getBatch().begin();
        for (MapLayer layer : map.getLayers()) {
            String n = layer.getName();
            if (layer.isVisible() && (n.equals("Spikes") || n.equals("Switch") || n.equals("Door") || n.contains("Interact"))) {
                renderObjectLayer(layer);
            }
        }
        renderer.getBatch().end();
    }

    public void renderForeground(OrthographicCamera camera) {
        if (renderer == null || map == null)
            return;
        MapLayer overhead = map.getLayers().get("Overhead");
        if (overhead != null && overhead.isVisible()) {
            renderer.getBatch().begin();
            if (overhead instanceof com.badlogic.gdx.maps.tiled.TiledMapTileLayer) {
                renderer.renderTileLayer((com.badlogic.gdx.maps.tiled.TiledMapTileLayer) overhead);
            } else {
                renderObjectLayer(overhead);
            }
            renderer.getBatch().end();
        }
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
                    tObj.getScaleX(), tObj.getScaleY(), tObj.getRotation());
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
                    if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                        String dest = portal.getName();
                        if (dest != null && dest.endsWith(".tmx"))
                            return "map/" + dest;
                    }
                }
            }
        }
        return null;
    }
    // =========================================
    // DOOR
    // =========================================

    public void openDoor(String doorName) {
        for (int i = doorObjects.size - 1; i >= 0; i--) {
            String objName = doorObjects.get(i).getName();
            if (objName != null && objName.contains(doorName))
                doorObjects.removeIndex(i);
        }
        MapLayer visualDoorLayer = map.getLayers().get("Door");
        if (visualDoorLayer != null) {
            for (MapObject obj : visualDoorLayer.getObjects()) {
                if (doorName.equals(obj.getName()))
                    obj.setVisible(false);
            }
        }
    }

    public float getSortY() {
        if (map == null)
            return 0;
        Float y = map.getProperties().get("sortY", Float.class);
        return (y != null) ? y : 0;
    }
    // =========================
    // GETTER
    // =========================
    public String getCurrentMapName() {
        return currentMapName;
    }
    public TiledMap getMap() {
        return map;
    }
    public float getMapWidth() {
        int width = map.getProperties().get("width", Integer.class);
        int tileWidth = map.getProperties().get("tilewidth", Integer.class);
        return width * tileWidth;    }

    public float getMapHeight() {
        int height = map.getProperties().get("height", Integer.class);
        int tileHeight = map.getProperties().get("tileheight", Integer.class);
        return height * tileHeight;
    }
    public Array<RectangleMapObject> getInteractPoints() {
        return interactPoints;
    }
    // =========================
    // DISPOSE
    // =========================
    public void dispose() {
        if (map != null) map.dispose();
        if (renderer != null) renderer.dispose();
    }
}
