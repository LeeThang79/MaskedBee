package game.maskedbee.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Align;
import game.maskedbee.main.NotificationManager;
import game.maskedbee.screens.PlayScreen;

public class HudRenderer {
    private final PlayScreen screen;

    public HudRenderer(PlayScreen screen) {
        this.screen = screen;
    }

    // ==========================================
    // 1. VẼ ĐỒNG HỒ ĐẾM NGƯỢC BOM WAX LAB
    // ==========================================
    public void drawWaxCountdownTimer(float timerValue, BitmapFont font) {
        int secondsLeft = Math.max(0, (int) Math.ceil(timerValue));
        int minutes = secondsLeft / 60;
        int seconds = secondsLeft % 60;

        String text = String.format("SELF-DESTRUCT %02d:%02d", minutes, seconds);

        screen.game.batch.setProjectionMatrix(screen.getCamera().combined);
        screen.game.batch.begin();

        font.draw(
            screen.game.batch,
            text,
            screen.getCamera().position.x - screen.getViewport().getWorldWidth() / 2f + 12f,
            screen.getCamera().position.y + screen.getViewport().getWorldHeight() / 2f - 12f
        );

        screen.game.batch.end();
    }

    // ==========================================
    // 2. VẼ BẢNG HƯỚNG DẪN TÂN THỦ (TUTORIAL)
    // ==========================================
    public void drawTutorial(ShapeRenderer shapeRender, BitmapFont font, BitmapFont hintFont) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRender.setProjectionMatrix(screen.getCamera().combined);

        // A. VẼ CÁC MẢNG NỀN TRONG SUỐT
        shapeRender.begin(ShapeRenderer.ShapeType.Filled);
        shapeRender.setColor(0f, 0f, 0f, 0.6f);
        shapeRender.rect(
            screen.getCamera().position.x - screen.getViewport().getWorldWidth() / 2,
            screen.getCamera().position.y - screen.getViewport().getWorldHeight() / 2,
            screen.getViewport().getWorldWidth(),
            screen.getViewport().getWorldHeight()
        );

        shapeRender.setColor(0.1f, 0.1f, 0.1f, 0.75f);
        shapeRender.rect(screen.getCamera().position.x - 170, screen.getCamera().position.y - 100, 360, 220);
        shapeRender.end();

        // B. VẼ ĐƯỜNG VIỀN
        shapeRender.begin(ShapeRenderer.ShapeType.Line);
        shapeRender.setColor(Color.GOLD);
        shapeRender.rect(screen.getCamera().position.x - 170, screen.getCamera().position.y - 125, 340, 250);
        shapeRender.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // C. VẼ CHỮ
        screen.game.batch.setProjectionMatrix(screen.getCamera().combined);
        screen.game.batch.begin();

        font.setColor(Color.PINK);
        font.draw(screen.game.batch, "--- HƯỚNG DẪN CƠ BẢN ---", screen.getCamera().position.x - 91, screen.getCamera().position.y + 100);

        font.setColor(Color.WHITE);
        hintFont.draw(screen.game.batch, "W, A, S, D hoặc Mũi tên: Di chuyển", screen.getCamera().position.x - 160, screen.getCamera().position.y + 75);
        hintFont.draw(screen.game.batch, "Ctrl : Đi rón rén", screen.getCamera().position.x - 160, screen.getCamera().position.y + 50);
        hintFont.draw(screen.game.batch, "G : Tìm manh mối", screen.getCamera().position.x - 160, screen.getCamera().position.y + 25);
        hintFont.draw(screen.game.batch, "E : Tương tác đồ vật", screen.getCamera().position.x - 160, screen.getCamera().position.y);
        hintFont.draw(screen.game.batch, "F : Mở khóa cửa/Núp", screen.getCamera().position.x - 160, screen.getCamera().position.y - 25);
        hintFont.draw(screen.game.batch, "P : Đeo/Tháo mặt nạ", screen.getCamera().position.x - 160, screen.getCamera().position.y - 50);
        hintFont.draw(screen.game.batch, "SPACE : Đi qua phòng tiếp theo", screen.getCamera().position.x - 160, screen.getCamera().position.y - 75);

        // Hiệu ứng chữ nhấp nháy báo hiệu bấm phím
        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            font.setColor(Color.PINK);
        } else {
            font.setColor(Color.RED);
        }
        font.draw(screen.game.batch, "Bấm SPACE để bắt đầu", screen.getCamera().position.x - 81, screen.getCamera().position.y - 100);

        font.setColor(Color.WHITE); // Reset màu
        screen.game.batch.end();
    }

    // ==========================================
    // 3. VẼ THÔNG BÁO NỔI (NOTIFICATION)
    // ==========================================
    public void drawNotification(float delta, ShapeRenderer shapeRender, BitmapFont hintFont) {
        // Cập nhật bộ đếm thời gian của thông báo
        NotificationManager.getInstance().update(delta);

        String notifMsg = NotificationManager.getInstance().currentMessage;
        float notifTimer = NotificationManager.getInstance().timer;

        if (notifMsg != null && !notifMsg.isEmpty()) {
            float alpha = Math.min(0.8f, notifTimer);

            GlyphLayout notifLayout = new GlyphLayout(hintFont, notifMsg);

            float paddingX = 15f;
            float paddingY = 8f;
            float boxWidth = notifLayout.width + (paddingX * 2);
            float boxHeight = notifLayout.height + (paddingY * 2);

            float boxX = screen.getCamera().position.x - boxWidth / 2f;
            float boxY = screen.getCamera().position.y - 110f;

            // Vẽ khung nền đen
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRender.setProjectionMatrix(screen.getCamera().combined);
            shapeRender.begin(ShapeRenderer.ShapeType.Filled);
            shapeRender.setColor(0f, 0f, 0f, alpha * 0.7f);
            shapeRender.rect(boxX, boxY, boxWidth, boxHeight);
            shapeRender.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);

            // Vẽ chữ
            screen.game.batch.setProjectionMatrix(screen.getCamera().combined);
            screen.game.batch.begin();

            hintFont.setColor(1f, 1f, 1f, alpha);
            float textX = boxX + paddingX;
            float textY = boxY + boxHeight - paddingY;

            hintFont.draw(screen.game.batch, notifMsg, textX, textY, notifLayout.width, Align.center, false);

            hintFont.setColor(Color.WHITE);
            screen.game.batch.end();
        }
    }
}
