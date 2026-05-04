package game.maskedbee.objects;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Rectangle;

public class Spike {
    public TiledMapTileMapObject mapObject;
    public String type;
    public boolean isUp;
    public Rectangle hitbox;

    public Spike(TiledMapTileMapObject obj) {
        this.mapObject = obj;
        // Đọc dữ liệu từ Tiled
        this.type = obj.getProperties().get("type", String.class);
        Boolean up = obj.getProperties().get("isUp", Boolean.class);
        this.isUp = (up != null) ? up : true;

        // Tạo vùng va chạm cho gai (dùng tọa độ X, Y của object)
        this.hitbox = new Rectangle(obj.getX(), obj.getY() - 32, 32, 32);
    }

    // Hàm đảo trạng thái và ĐỔI HÌNH ẢNH
    public void toggle(TiledMap map) {
        isUp = !isUp;
        if (isUp) {
            // Lấy ID ảnh Gai Nhọn và đổi
            Integer upId = mapObject.getTile().getProperties().get("upTileID", Integer.class);
            if (upId != null) mapObject.setTile(map.getTileSets().getTile(upId));
        } else {
            // Lấy ID ảnh Gai Phẳng và đổi
            Integer downId = mapObject.getTile().getProperties().get("downTileID", Integer.class);
            if (downId != null) mapObject.setTile(map.getTileSets().getTile(downId));
        }
    }
}
