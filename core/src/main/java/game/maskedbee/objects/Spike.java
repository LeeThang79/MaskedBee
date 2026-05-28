package game.maskedbee.objects;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Rectangle;

public class Spike {
    public TiledMapTileMapObject mapObject;
    public String type;
    public boolean isUp;
    public Rectangle hitbox;

    private final int originalTileId;
    private final Integer upTileId;
    private final Integer downTileId;

    public Spike(TiledMapTileMapObject obj) {
        this.mapObject = obj;

        this.type = getStringProperty(obj, "type");

        Boolean up = getBooleanProperty(obj, "isUp");
        this.isUp = up != null ? up : true;

        this.originalTileId = obj.getTile() != null ? obj.getTile().getId() : -1;

        Integer readUpTileId = getIntProperty(obj, "upTileID", "upTileId", "upID", "upId");
        Integer readDownTileId = getIntProperty(obj, "downTileID", "downTileId", "downID", "downId");

        this.upTileId = readUpTileId != null ? readUpTileId : (isUp ? originalTileId : null);
        this.downTileId = readDownTileId != null ? readDownTileId : (!isUp ? originalTileId : null);

        this.hitbox = new Rectangle(obj.getX(), obj.getY() - 32f, 32f, 32f);
    }

    public void toggle(TiledMap map) {
        boolean nextIsUp = !isUp;
        Integer nextTileId = nextIsUp ? upTileId : downTileId;

        if (nextTileId == null || nextTileId < 0) {
            System.out.println("⚠️ Spike missing tile ID. type=" + type
                + ". Hãy thêm upTileID/downTileID trong Tiled.");
            // Vẫn đổi trạng thái để gameplay chạy, nhưng hình sẽ không đổi nếu thiếu tile ID.
            isUp = nextIsUp;
            return;
        }

        TiledMapTile tile = map.getTileSets().getTile(nextTileId);

        if (tile == null) {
            System.out.println("⚠️ Cannot find spike tile with ID = " + nextTileId);
            isUp = nextIsUp;
            return;
        }

        isUp = nextIsUp;
        mapObject.setTile(tile);
    }

    private String getStringProperty(TiledMapTileMapObject obj, String... names) {
        Object value = getProperty(obj, names);
        return value == null ? null : value.toString();
    }

    private Boolean getBooleanProperty(TiledMapTileMapObject obj, String... names) {
        Object value = getProperty(obj, names);

        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;

        return Boolean.parseBoolean(value.toString());
    }

    private Integer getIntProperty(TiledMapTileMapObject obj, String... names) {
        Object value = getProperty(obj, names);

        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;

        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Object getProperty(TiledMapTileMapObject obj, String... names) {
        for (String name : names) {
            Object value = obj.getProperties().get(name);

            if (value != null) return value;

            if (obj.getTile() != null) {
                value = obj.getTile().getProperties().get(name);
                if (value != null) return value;
            }
        }

        return null;
    }
}
