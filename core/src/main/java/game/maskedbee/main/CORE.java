package game.maskedbee.main;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import game.maskedbee.entities.Player;
import game.maskedbee.map.MapManager;
import game.maskedbee.screens.FirstScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class CORE extends Game {
    public SpriteBatch batch;
    public MapManager map;
    private MapManager mapManager;
    private Player player;
    @Override
    public void create() {
        batch = new SpriteBatch();
        map = new MapManager();

        this.setScreen(new FirstScreen(this)); // Chuyển sang màn hình FirstScreen của
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        AudioManager.getInstance().update(dt); // CẬP NHẬT ĐỂ NHẠC TỰ ĐỘNG NHỎ DẦN / TO DẦN

        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        map.dispose();
    }
}
