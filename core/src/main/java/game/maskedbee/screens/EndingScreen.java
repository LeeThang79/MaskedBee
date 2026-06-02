package game.maskedbee.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;

import game.maskedbee.main.CORE;

public class EndingScreen implements Screen {
    private final CORE game;
    private final String endingType;

    private BitmapFont font;
    private GlyphLayout layout;
    private Matrix4 screenMatrix;

    public EndingScreen(CORE game) {
        this(game, "default");
    }

    public EndingScreen(CORE game, String endingType) {
        this.game = game;
        this.endingType = endingType;
    }

    @Override
    public void show() {
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(2f);

        layout = new GlyphLayout();
        screenMatrix = new Matrix4();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return;
        }

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // Quan trọng: dùng tọa độ màn hình, không dùng camera của PlayScreen
        screenMatrix.setToOrtho2D(0, 0, screenWidth, screenHeight);
        game.batch.setProjectionMatrix(screenMatrix);

        game.batch.begin();

        if ("queen".equalsIgnoreCase(endingType)) {
            drawCentered("ENDING 1: QUEEN", screenWidth, screenHeight * 0.68f);
            drawCentered("You accepted the Queen's offer.", screenWidth, screenHeight * 0.56f);
            drawCentered("You became the new Queen of the hive.", screenWidth, screenHeight * 0.49f);
        } else if ("escape".equalsIgnoreCase(endingType)) {
            drawCentered("ENDING 2: ESCAPE", screenWidth, screenHeight * 0.68f);
            drawCentered("You refused the Queen.", screenWidth, screenHeight * 0.56f);
            drawCentered("You escaped with your own will.", screenWidth, screenHeight * 0.49f);
        } else if ("boss".equalsIgnoreCase(endingType)) {
            drawCentered("BAD ENDING", screenWidth, screenHeight * 0.68f);
            drawCentered("The ritualer caught you.", screenWidth, screenHeight * 0.56f);
            drawCentered("There is no return.", screenWidth, screenHeight * 0.49f);
        } else if ("no_mask".equalsIgnoreCase(endingType)) {
            drawCentered("ENDING: LOST WITHOUT THE MASK", screenWidth, screenHeight * 0.68f);
            drawCentered("You left the hive without the Queen's mask.", screenWidth, screenHeight * 0.56f);
            drawCentered("The truth remained hidden.", screenWidth, screenHeight * 0.49f);
        } else if ("inactive_mask".equalsIgnoreCase(endingType)) {
            drawCentered("ENDING: UNAWAKENED MASK", screenWidth, screenHeight * 0.68f);
            drawCentered("You found the mask, but never awakened its truth.", screenWidth, screenHeight * 0.56f);
            drawCentered("The hive let you leave, but not as yourself.", screenWidth, screenHeight * 0.49f);
        } else {
            drawCentered("ENDING", screenWidth, screenHeight * 0.68f);
            drawCentered("The story ends here.", screenWidth, screenHeight * 0.56f);
        }

        drawCentered("Press ENTER or ESC to quit", screenWidth, screenHeight * 0.28f);

        game.batch.end();
    }

    private void drawCentered(String text, float screenWidth, float y) {
        layout.setText(font, text);
        float x = (screenWidth - layout.width) / 2f;
        font.draw(game.batch, text, x, y);
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (font != null) {
            font.dispose();
        }
    }
}
