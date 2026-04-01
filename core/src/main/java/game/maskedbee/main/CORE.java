package game.maskedbee.main;

import com.badlogic.gdx.Game;

public class Core extends Game {

    @Override
    public void create () {
        setScreen(new GameScreen());
    }
}

