package game.maskedbee.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import game.maskedbee.screens.PlayScreen;

public class ChoiceMenu {
    private final PlayScreen screen;
    private final String question;
    private final String option1;
    private final String option2;

    private int selectedIndex = 0; // 0: Option 1, 1: Option 2
    private final Texture arrowTexture;

    // Hàm khởi tạo nhận câu hỏi và các lựa chọn tùy biến từ ngoài vào
    public ChoiceMenu(PlayScreen screen, String question, String option1, String option2) {
        this.screen = screen;
        this.question = question;
        this.option1 = option1;
        this.option2 = option2;
        this.arrowTexture = new Texture(Gdx.files.internal("menu/pointer.png"));
    }

    // Hàm cập nhật phím bấm bấm. Trả về kết quả (0 hoặc 1) khi xác nhận, bình thường trả về -1
    public int update() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selectedIndex = 0;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selectedIndex = 1;
        }

        // Khi người chơi ấn nút chọn
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            return selectedIndex;
        }

        return -1; // Chưa chọn xong
    }

    // Hàm vẽ giao diện dạng hộp thoại (Hộp đen, viền trắng, chữ dóng hàng)
    public void draw(ShapeRenderer shapeRender, BitmapFont font) {
        float camX = screen.getCamera().position.x;
        float camY = screen.getCamera().position.y;
        float viewWidth = screen.getCamera().viewportWidth;
        float viewHeight = screen.getCamera().viewportHeight;

        float boxWidth = viewWidth * 0.9f;
        float boxHeight = 80f;
        float boxX = camX - (boxWidth / 2f);
        float boxY = camY - (viewHeight / 2f) + 10f;

        // 1. Vẽ khung đen mờ dưới đáy màn hình
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRender.setProjectionMatrix(screen.getCamera().combined);
        shapeRender.begin(ShapeRenderer.ShapeType.Filled);
        shapeRender.setColor(0, 0, 0, 0.8f);
        shapeRender.rect(boxX, boxY, boxWidth, boxHeight);
        shapeRender.setColor(Color.WHITE);
        shapeRender.rectLine(boxX, boxY, boxX + boxWidth, boxY, 2f);
        shapeRender.rectLine(boxX, boxY + boxHeight, boxX + boxWidth, boxY + boxHeight, 2f);
        shapeRender.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 2. Vẽ chữ và mũi tên chỉ định
        screen.game.batch.setProjectionMatrix(screen.getCamera().combined);
        screen.game.batch.begin();

        float textStartX = boxX + 20f;
        float textStartY = boxY + boxHeight - 10f;
        float arrowWidth = 14f;
        float arrowHeight = 14f;

        // Vẽ câu hỏi chính
        font.setColor(Color.WHITE);
        font.draw(screen.game.batch, question, textStartX, textStartY, boxWidth - 40f, Align.left, true);

        // Lựa chọn thứ nhất
        float option1Y = textStartY - 22f;
        if (selectedIndex == 0) {
            screen.game.batch.draw(arrowTexture, textStartX, option1Y - 11f, arrowWidth, arrowHeight);
        }
        font.draw(screen.game.batch, option1, textStartX + 20f, option1Y, boxWidth - 60f, Align.left, true);

        // Lựa chọn thứ hai
        float option2Y = option1Y - 20f;
        if (selectedIndex == 1) {
            screen.game.batch.draw(arrowTexture, textStartX, option2Y - 11f, arrowWidth, arrowHeight);
        }
        font.draw(screen.game.batch, option2, textStartX + 20f, option2Y, boxWidth - 60f, Align.left, true);

        screen.game.batch.end();
    }

    public void dispose() {
        if (arrowTexture != null) arrowTexture.dispose();
    }
}
