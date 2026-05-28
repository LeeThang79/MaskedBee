package game.maskedbee.objects;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Rectangle;

public class Lever {
    public TiledMapTileMapObject mapObject;
    public String type;
    public String targetColor;
    public String targetName;
    public boolean isPulled;
    public Rectangle hitbox;

    private final Integer originalTileId;
    private final Integer otherTileId;

    public Lever(TiledMapTileMapObject obj) {
        this.mapObject = obj;

        this.type = getStringProperty(obj, "type");
        this.targetColor = getStringProperty(obj, "targetColor");
        this.targetName = getStringProperty(obj, "targetName");

        Boolean pulled = getBooleanProperty(obj, "isPulled");
        this.isPulled = pulled != null ? pulled : false;

        this.originalTileId = obj.getTile() != null ? obj.getTile().getId() : null;
        this.otherTileId = getIntProperty(obj, "otherTileID", "otherTileId", "otherID", "otherId");

        this.hitbox = new Rectangle(
            obj.getX() - 16f,
            obj.getY() - 32f - 16f,
            64f,
            64f
        );
    }

    public void toggle(TiledMap map) {
        isPulled = !isPulled;

        Integer nextTileId;

        if (isPulled) {
            nextTileId = otherTileId;
        } else {
            nextTileId = originalTileId;
        }

        if (nextTileId == null || nextTileId < 0) {
            System.out.println("⚠️ Lever missing otherTileID. type=" + type + ", targetColor=" + targetColor);
            return;
        }

        TiledMapTile tile = map.getTileSets().getTile(nextTileId);

        if (tile == null) {
            System.out.println("⚠️ Cannot find lever tile with ID = " + nextTileId);
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
