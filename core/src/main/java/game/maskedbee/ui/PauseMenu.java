package game.maskedbee.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import game.maskedbee.screens.PlayScreen;

public class PauseMenu {
    private PlayScreen screen;

    private Rectangle continueBtn;
    private Rectangle quitBtn;
    private int selectedIndex = 0;
    private Texture arrowTexture;

    // Hàm khởi tạo nhận vào PlayScreen gốc
    public PauseMenu(PlayScreen screen) {
        this.screen = screen;
        this.continueBtn = new Rectangle(0, 0, 100, 30);
        this.quitBtn = new Rectangle(0, 0, 100, 30);
        this.arrowTexture = new Texture(Gdx.files.internal("menu/pointer.png"));
    }

    // Tách phần handlePauseMenuLogic cũ sang đây
    public void update() {
        float centerX = screen.getCamera().position.x;
        float centerY = screen.getCamera().position.y;

        continueBtn.set(centerX - 50, centerY - 15, 100, 25);
        quitBtn.set(centerX - 45, centerY - 40, 90, 25);

        // Lấy tọa độ chuột
        Vector3 touchPoint = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        screen.getCamera().unproject(touchPoint);

        if (continueBtn.contains(touchPoint.x, touchPoint.y)) {
            selectedIndex = 0;
        } else if (quitBtn.contains(touchPoint.x, touchPoint.y)) {
            selectedIndex = 1;
        }

        // Bàn phím
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selectedIndex = 0;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selectedIndex = 1;
        }

        // Chốt đơn bằng phím ENTER/SPACE
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (selectedIndex == 0) {
                screen.resumeGame(); // Gọi hàm tiếp tục chơi của PlayScreen
            } else if (selectedIndex == 1) {
                screen.returnToMainMenu(); // Gọi hàm thoát của PlayScreen
            }
        }

        // Chốt đơn bằng Chuột
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (continueBtn.contains(touchPoint.x, touchPoint.y)) {
                screen.resumeGame();
            } else if (quitBtn.contains(touchPoint.x, touchPoint.y)) {
                screen.returnToMainMenu();
            }
        }
    }

    // Tách phần drawPauseMenu cũ sang đây (Nhận Font và Render từ ngoài vào để đỡ tốn RAM tạo mới)
    public void draw(ShapeRenderer shapeRender, BitmapFont titleFont, BitmapFont menuFont, BitmapFont hintFont) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRender.setProjectionMatrix(screen.getCamera().combined);
        shapeRender.begin(ShapeRenderer.ShapeType.Filled);
        shapeRender.setColor(0, 0, 0, 0.5f);
        shapeRender.rect(
            screen.getCamera().position.x - screen.getViewport().getWorldWidth() / 2,
            screen.getCamera().position.y - screen.getViewport().getWorldHeight() / 2,
            screen.getViewport().getWorldWidth(),
            screen.getViewport().getWorldHeight()
        );
        shapeRender.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        screen.game.batch.setProjectionMatrix(screen.getCamera().combined);
        screen.game.batch.begin();

        float viewWidth = screen.getViewport().getWorldWidth();
        float viewHeight = screen.getViewport().getWorldHeight();
        float startX = screen.getCamera().position.x - viewWidth / 2;
        float centerX = screen.getCamera().position.x;
        float centerY = screen.getCamera().position.y;

        titleFont.setColor(Color.WHITE);
        titleFont.draw(screen.game.batch, "TẠM DỪNG", startX, centerY + 45, viewWidth, Align.center, false);

        float arrowWidth = 12f;
        float arrowHeight = 12f;

        if (selectedIndex == 0) {
            screen.game.batch.draw(arrowTexture, centerX - 55, centerY - 14, arrowWidth, arrowHeight);
        } else {
            menuFont.setColor(Color.WHITE);
        }
        menuFont.draw(screen.game.batch, "Tiếp tục", startX, centerY + 0, viewWidth, Align.center, false);

        if (selectedIndex == 1) {
            screen.game.batch.draw(arrowTexture, centerX - 45, centerY - 47, arrowWidth, arrowHeight);
        } else {
            menuFont.setColor(Color.WHITE);
        }
        menuFont.draw(screen.game.batch, "Thoát", startX, centerY - 35, viewWidth, Align.center, false);

        float textX = startX;
        float textWidth = viewWidth - 15f;
        float textY = (centerY - viewHeight / 2f) + 15f;

        hintFont.draw(screen.game.batch, "Bấm SPACE để lựa chọn", textX, textY, textWidth, Align.right, false);

        screen.game.batch.end();
    }

    public void dispose() {
        if (arrowTexture != null) arrowTexture.dispose();
    }
}
