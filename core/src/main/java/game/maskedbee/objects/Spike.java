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

    public Spike(TiledMapTileMapObject obj) {
        this.mapObject = obj;

        this.type = getStringProperty(obj, "type");

        Boolean up = getBooleanProperty(obj, "isUp");
        this.isUp = up != null ? up : true;

        this.hitbox = new Rectangle(obj.getX(), obj.getY() - 32f, 32f, 32f);
    }

    public void toggle(TiledMap map) {
        isUp = !isUp;

        Integer tileId = isUp
            ? getIntProperty(mapObject, "upTileID", "upTileId", "upID", "upId")
            : getIntProperty(mapObject, "downTileID", "downTileId", "downID", "downId");

        if (tileId == null || tileId < 0) {
            System.out.println("⚠️ Spike missing tile ID. type=" + type + ", isUp=" + isUp);
            return;
        }

        TiledMapTile tile = map.getTileSets().getTile(tileId);
        if (tile == null) {
            System.out.println("⚠️ Cannot find spike tile with ID = " + tileId);
            return;
        }

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
