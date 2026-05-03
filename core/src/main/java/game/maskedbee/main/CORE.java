package game.maskedbee.main;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import game.maskedbee.screens.FirstScreen;
import game.maskedbee.map.MapManager;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class CORE extends Game {
    public SpriteBatch batch;
    public MapManager map;

    @Override
    public void create() {
        batch = new SpriteBatch();
        map = new MapManager();

        this.setScreen(new FirstScreen(this)); // Chuyển sang màn hình FirstScreen của
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        map.dispose();
    }
}
