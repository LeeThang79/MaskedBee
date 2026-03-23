package game.maskedbee;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import sun.tools.jconsole.Tab;

/** First screen of the application. Displayed after the application is created. */
public class FirstScreen implements Screen {
    private final CORE game;
    public SpriteBatch batch;
    Texture background;
    private Stage stage;
    private Texture button;

    public FirstScreen(CORE game) {
        this.game = game;
    }
    @Override
    public void show() {
        batch = new SpriteBatch();

        background = new Texture("background.png");

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        button = new Texture("button.png");
        BitmapFont font = new BitmapFont();
        TextButtonStyle style = new TextButtonStyle();
        style.up = new TextureRegionDrawable(button);
        style.font = font;

        TextButton startbtn = new TextButton("START", style);
        TextButton settingbtn = new TextButton("SETTING", style);
        TextButton quitbtn = new TextButton("QUIT", style);

        Table table = new Table();
        table.setFillParent(true);

        table.add(startbtn).padBottom(20).row();
        table.add(settingbtn).padBottom(20).row();
        table.add(quitbtn);

        stage.addActor(table);

        startbtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new PlayScreen(game));
            }
        });
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        batch.begin();
        batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        // If the window is minimized on a desktop (LWJGL3) platform, width and height are 0, which causes problems.
        // In that case, we don't resize anything, and wait for the window to be a normal size before updating.
        if(width <= 0 || height <= 0);
        // Resize your screen here. The parameters represent the new window size.
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    @Override
    public void hide() {
        // This method is called when another screen replaces this one.
    }

    @Override
    public void dispose() {
        if (background != null) background.dispose();
        if (button != null) button.dispose();
        if (stage != null) stage.dispose();
    }
}
