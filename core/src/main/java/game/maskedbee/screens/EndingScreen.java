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
            drawCentered(titleFont, "ENDING: ONG CHÚA", screenWidth, screenHeight * 0.75f);
            bodyFont.setColor(Color.WHITE);
            drawCentered(bodyFont, "Bạn chấp nhận trở thành nữ hoàng mới của nơi này.", screenWidth, screenHeight * 0.58f);
            bodyFont.setColor(Color.LIGHT_GRAY);
            drawCentered(bodyFont, "\"Chiếc mặt nạ không còn cảm thấy nặng nề nữa. Tôi có thể cảm nhận được từng con ong, từng tế bào, từng nhịp thở của nơi này.\"", screenWidth, screenHeight * 0.50f);
        } else if ("escape".equalsIgnoreCase(endingType)) {
            titleFont.setColor(Color.SKY);
            drawCentered(titleFont, "ENDING: GIỮ LẠI NHÂN TÍNH", screenWidth, screenHeight * 0.75f);
            bodyFont.setColor(Color.WHITE);
            drawCentered(bodyFont, "Bạn từ chối trở thành ong chúa và trốn thoát khỏi nơi này.", screenWidth, screenHeight * 0.58f);
            bodyFont.setColor(Color.LIGHT_GRAY);
            drawCentered(bodyFont, "\"Tôi tháo bỏ chiếc mặt nạ.\nTôi chọn bước tiếp với tư cách là một con người.\"", screenWidth, screenHeight * 0.50f);
        } else if ("boss".equalsIgnoreCase(endingType)) {
            titleFont.setColor(Color.SCARLET);
            drawCentered(titleFont, "BAD ENDING: BỊ BẮT GIỮ", screenWidth, screenHeight * 0.75f);
            bodyFont.setColor(Color.WHITE);
            drawCentered(bodyFont, "Bạn đã bị bắt giữ bởi kẻ trùm đầu.", screenWidth, screenHeight * 0.52f);
        } else if ("no_mask".equalsIgnoreCase(endingType)) {
            titleFont.setColor(Color.ORANGE);
            drawCentered(titleFont, "ENDING: TRỐN THOÁT", screenWidth, screenHeight * 0.75f);
            bodyFont.setColor(Color.WHITE);
            drawCentered(bodyFont, "Bạn trốn thoát mà chưa khám phá hết sự thật.", screenWidth, screenHeight * 0.58f);
            bodyFont.setColor(Color.LIGHT_GRAY);
            drawCentered(bodyFont, "\"Tôi không quay đầu lại và cũng không tìm kiếm câu trả lời. Tôi đã tự do... nhưng tại sao vẫn nghe thấy tiếng ngân nga?\"", screenWidth, screenHeight * 0.50f);
        } else if ("lab_escape".equalsIgnoreCase(endingType)) {
            titleFont.setColor(Color.CYAN);
            drawCentered(titleFont, "ENDING: KẺ HỦY DIỆT", screenWidth, screenHeight * 0.75f);
            bodyFont.setColor(Color.WHITE);
            drawCentered(bodyFont, "Bạn giải cứu tù nhân và tìm ra cách phá hủy nơi này.", screenWidth, screenHeight * 0.58f);
            bodyFont.setColor(Color.LIGHT_GRAY);
            drawCentered(bodyFont, "\"Đôi bàn tay tôi cháy sạm,\nnhưng thế giới cuối cùng đã bình yên.\"", screenWidth, screenHeight * 0.50f);
        } else if ("lab_explosion".equalsIgnoreCase(endingType)) {
            titleFont.setColor(Color.RED);
            drawCentered(titleFont, "BAD ENDING: MỘT CHÚT NỮA THÔI", screenWidth, screenHeight * 0.75f);
            bodyFont.setColor(Color.WHITE);
            drawCentered(bodyFont, "Tầng hầm sụp đổ trước khi bạn kịp trốn thoát.", screenWidth, screenHeight * 0.52f);
        }
    }

    private void drawCentered(BitmapFont targetFont, String text, float screenWidth, float y) {
        float maxWidth = screenWidth * 0.90f; // Xác định chiều rộng tối đa cho text
        float x = (screenWidth - maxWidth) / 2f;

        targetFont.draw(
            game.batch,
            text,
            x,
            y,
            maxWidth,
            Align.center, // Căn giữa các dòng chữ với nhau
            true          // Bật tính năng tự động xuống dòng
        );
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
