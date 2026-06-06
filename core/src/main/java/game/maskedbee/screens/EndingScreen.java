package game.maskedbee.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

import game.maskedbee.main.CORE;
import game.maskedbee.main.AudioManager;
import game.maskedbee.main.NotificationManager;

public class EndingScreen implements Screen {
    private final CORE game;
    private final String endingType;

    private BitmapFont titleFont;
    private BitmapFont bodyFont;
    private BitmapFont hintFont;
    private GlyphLayout layout;
    private Matrix4 screenMatrix;
    private float stateTime = 0f;

    // Biến cho menu lựa chọn
    private int selectedIndex = 0; // 0: Có, 1: Không
    private Texture arrowTexture;

    public EndingScreen(CORE game, String endingType) {
        this.game = game;
        this.endingType = endingType;
    }

    @Override
    public void show() {
        titleFont = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        titleFont.getData().setScale(2.5f);

        bodyFont = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        bodyFont.getData().setScale(1.2f);

        hintFont = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        hintFont.getData().setScale(0.8f);

        layout = new GlyphLayout();
        screenMatrix = new Matrix4();

        arrowTexture = new Texture(Gdx.files.internal("menu/pointer.png"));
    }

    @Override
    public void render(float delta) {
        stateTime += delta;
        handleInput();

        ScreenUtils.clear(0.05f, 0.05f, 0.05f, 1);
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        screenMatrix.setToOrtho2D(0, 0, screenWidth, screenHeight);
        game.batch.setProjectionMatrix(screenMatrix);
        game.batch.begin();

        // 1. VẼ NỘI DUNG ENDING
        renderEndingText(screenWidth, screenHeight);

        // 2. VẼ CÂU HỎI CHƠI LẠI
        bodyFont.setColor(Color.WHITE);
        drawCentered(bodyFont, "Bạn có muốn chơi lại không?", screenWidth, screenHeight * 0.32f);

        // 3. VẼ LỰA CHỌN CÓ / KHÔNG
        float optionY = screenHeight * 0.25f;
        float spacing = 150f; // Khoảng cách giữa Có và Không

        // --- Nút CÓ (Trái) ---
        String textYes = "CÓ";
        layout.setText(bodyFont, textYes);
        float yesX = (screenWidth / 2) - spacing;
        bodyFont.draw(game.batch, textYes, yesX, optionY);
        if (selectedIndex == 0) {
            game.batch.draw(arrowTexture, yesX - 30, optionY - 18, 20, 20);
        }

        // --- Nút KHÔNG (Phải) ---
        String textNo = "KHÔNG";
        layout.setText(bodyFont, textNo);
        float noX = (screenWidth / 2) + spacing - layout.width;
        bodyFont.draw(game.batch, textNo, noX, optionY);
        if (selectedIndex == 1) {
            game.batch.draw(arrowTexture, noX - 30, optionY - 18, 20, 20);
        }

        // 4. VẼ HINT BẤM SPACE (Góc dưới bên phải)
        float alphaPulse = 0.65f + 0.35f * MathUtils.sin(stateTime * 4f);
        hintFont.setColor(1f, 1f, 1f, alphaPulse);
        hintFont.draw(game.batch, "Bấm SPACE để lựa chọn", screenWidth - 20, 40, 0, Align.right, false);

        game.batch.end();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            selectedIndex = 0;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            selectedIndex = 1;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (selectedIndex == 0) {
                restartGameFromBeginning();
            } else {
                returnToMainMenu();
            }
        }
    }

    // =========================================================
    // HAI HÀM QUẢN LÝ RESET GAME ĐƯỢC TÁCH RIÊNG
    // =========================================================
    private void restartGameFromBeginning() {
        if (game.map != null) {
            game.map.clearAllProgressState();
            game.map.clearPushableStateForCurrentMap();
        }
        AudioManager.getInstance().stopBackgroundMusic();
        NotificationManager.getInstance().currentMessage = "";
        game.setScreen(new PlayScreen(game));
    }

    private void returnToMainMenu() {
        if (game.map != null) {
            game.map.clearAllProgressState();
            game.map.clearPushableStateForCurrentMap();
        }
        AudioManager.getInstance().stopBackgroundMusic();
        NotificationManager.getInstance().currentMessage = "";
        game.setScreen(new FirstScreen(game));
    }
    // =========================================================

    private void renderEndingText(float screenWidth, float screenHeight) {
        if ("queen".equalsIgnoreCase(endingType)) {
            titleFont.setColor(Color.GOLD);
            drawCentered(titleFont, "ENDING 1: THE NEW QUEEN", screenWidth, screenHeight * 0.75f);
            bodyFont.setColor(Color.WHITE);
            drawCentered(bodyFont, "You accepted the Queen's offer.", screenWidth, screenHeight * 0.58f);
            bodyFont.setColor(Color.LIGHT_GRAY);
            drawCentered(bodyFont, "You became the new ruler of the hive.", screenWidth, screenHeight * 0.52f);
        } else if ("escape".equalsIgnoreCase(endingType)) {
            titleFont.setColor(Color.SKY);
            drawCentered(titleFont, "ENDING 2: ESCAPE", screenWidth, screenHeight * 0.75f);
            bodyFont.setColor(Color.WHITE);
            drawCentered(bodyFont, "You refused the Queen.", screenWidth, screenHeight * 0.58f);
            bodyFont.setColor(Color.LIGHT_GRAY);
            drawCentered(bodyFont, "You escaped with your own free will.", screenWidth, screenHeight * 0.52f);
        } else if ("boss".equalsIgnoreCase(endingType)) {
            titleFont.setColor(Color.SCARLET);
            drawCentered(titleFont, "BAD ENDING", screenWidth, screenHeight * 0.75f);
            bodyFont.setColor(Color.WHITE);
            drawCentered(bodyFont, "The ritualer caught you.", screenWidth, screenHeight * 0.58f);
            bodyFont.setColor(Color.DARK_GRAY);
            drawCentered(bodyFont, "There is no return.", screenWidth, screenHeight * 0.52f);
        } else if ("no_mask".equalsIgnoreCase(endingType)) {
            titleFont.setColor(Color.ORANGE);
            drawCentered(titleFont, "ENDING: LOST WITHOUT THE MASK", screenWidth, screenHeight * 0.75f);
            bodyFont.setColor(Color.WHITE);
            drawCentered(bodyFont, "You left the hive without the Queen's mask.", screenWidth, screenHeight * 0.58f);
            bodyFont.setColor(Color.LIGHT_GRAY);
            drawCentered(bodyFont, "The truth remained forever hidden.", screenWidth, screenHeight * 0.52f);
        } else if ("lab_escape".equalsIgnoreCase(endingType)) {
            titleFont.setColor(Color.CYAN);
            drawCentered(titleFont, "ENDING: ESCAPE FROM THE WAX LAB", screenWidth, screenHeight * 0.75f);
            bodyFont.setColor(Color.WHITE);
            drawCentered(bodyFont, "You freed the prisoner and triggered the wax pump.", screenWidth, screenHeight * 0.58f);
            bodyFont.setColor(Color.LIGHT_GRAY);
            drawCentered(bodyFont, "The hive collapsed behind you as you escaped.", screenWidth, screenHeight * 0.52f);
        } else if ("lab_explosion".equalsIgnoreCase(endingType)) {
            titleFont.setColor(Color.RED);
            drawCentered(titleFont, "ENDING: MELTDOWN", screenWidth, screenHeight * 0.75f);
            bodyFont.setColor(Color.WHITE);
            drawCentered(bodyFont, "The wax lab exploded before you could escape.", screenWidth, screenHeight * 0.58f);
            bodyFont.setColor(Color.DARK_GRAY);
            drawCentered(bodyFont, "Your journey ends in the burning hive.", screenWidth, screenHeight * 0.52f);
        }
    }

    private void drawCentered(BitmapFont targetFont, String text, float screenWidth, float y) {
        layout.setText(targetFont, text);
        float x = (screenWidth - layout.width) / 2f;
        targetFont.draw(game.batch, text, x, y);
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (titleFont != null) titleFont.dispose();
        if (bodyFont != null) bodyFont.dispose();
        if (hintFont != null) hintFont.dispose();
        if (arrowTexture != null) arrowTexture.dispose();
    }
}
