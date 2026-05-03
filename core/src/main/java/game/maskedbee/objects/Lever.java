package game.maskedbee.objects;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Rectangle;

public class Lever {
    public TiledMapTileMapObject mapObject;
    public String type; // "lever" hoặc "door_lever"
    public String targetColor;
    public String targetName;
    public boolean isPulled;
    public Rectangle hitbox;

    public Lever(TiledMapTileMapObject obj) {
        this.mapObject = obj;
        this.type = obj.getProperties().get("type", String.class);
        this.targetColor = obj.getProperties().get("targetColor", String.class);
        this.targetName = obj.getProperties().get("targetName", String.class);
        Boolean pulled = obj.getProperties().get("isPulled", Boolean.class);
        this.isPulled = (pulled != null) ? pulled : false;

        // Vùng để Player đứng gần có thể tương tác (rộng hơn cần gạt 1 chút)
        this.hitbox = new Rectangle(obj.getX() - 10, obj.getY() - 32 - 10, 52, 52);
    }

    // Hàm lật cần gạt
    public void toggle(TiledMap map) {
        isPulled = !isPulled;
        // Đổi hình ảnh cần gạt trái/phải
        Integer otherId = mapObject.getTile().getProperties().get("otherTileID", Integer.class);
        if (otherId != null) {
            mapObject.setTile(map.getTileSets().getTile(otherId));
        }
    }
}
