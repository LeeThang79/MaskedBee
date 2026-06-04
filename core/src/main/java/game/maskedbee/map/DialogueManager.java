package game.maskedbee.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

public class DialogueManager {
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    // Trạng thái hội thoại
    public boolean isShowing = false;
    private Array<String> dialogueLines;
    private int currentLineIndex = 0;

    // Hiệu ứng gõ chữ (Typewriter)
    private String currentTextToDraw = "";
    private float timer = 0f;
    private float timePerCharacter = 0.05f; // Tốc độ gõ chữ (Càng nhỏ càng nhanh)
    private int characterIndex = 0;

    public DialogueManager() {
        this.shapeRenderer = new ShapeRenderer();
        this.font = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        this.font.getData().setScale(0.4f);
        this.dialogueLines = new Array<>();
    }

    // HÀM KÍCH HOẠT HỘI THOẠI
    public void startDialogue(String[] lines) {
        dialogueLines.clear();
        for (String line : lines) {
            dialogueLines.add(line);
        }
        currentLineIndex = 0;
        isShowing = true;
        resetTypewriter();
    }

    private void resetTypewriter() {
        currentTextToDraw = "";
        timer = 0f;
        characterIndex = 0;
    }

    // HÀM CẬP NHẬT (Xử lý phím G & Hiệu ứng chữ)
    public void update(float delta) {
        if (!isShowing) return;

        String fullLine = dialogueLines.get(currentLineIndex);

        // Chạy hiệu ứng gõ chữ
        if (characterIndex < fullLine.length()) {
            timer += delta;
            if (timer >= timePerCharacter) {
                timer = 0f;
                characterIndex++;
                currentTextToDraw = fullLine.substring(0, characterIndex);
            }
        }

        // Nhấn phím G để qua câu thoại / Chữ chạy nhanh
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            if (characterIndex < fullLine.length()) {
                // Nếu chữ chưa chạy hết -> Bấm để hiển thị toàn bộ câu luôn
                characterIndex = fullLine.length();
                currentTextToDraw = fullLine;
            } else {
                // Nếu chữ đã chạy hết -> Bấm để sang câu tiếp theo
                currentLineIndex++;
                if (currentLineIndex >= dialogueLines.size) {
                    isShowing = false; // Hết thoại thì đóng
                } else {
                    resetTypewriter(); // Chạy câu tiếp theo
                }
            }
        }
    }

    // HÀM VẼ (Vẽ khung nền đen mờ và chữ)
    public void draw(SpriteBatch batch, OrthographicCamera camera) {
        if (!isShowing) return;

        // TÍNH TOÁN VỊ TRÍ KHUNG TO
        float camX = camera.position.x;
        float camY = camera.position.y;
        float viewWidth = camera.viewportWidth;
        float viewHeight = camera.viewportHeight;

        float boxWidth = viewWidth * 0.9f; // Rộng 80% màn hình
        float boxHeight = 80f;
        float boxX = camX - (boxWidth / 2f);
        float boxY = camY - (viewHeight / 2f) + 10f; // Cách đáy 10 pixel

        // TÍNH TOÁN VỊ TRÍ KHUNG NHỎ
        float smallBoxWidth = 80f; // Chiều rộng khung nhỏ (Tùy chỉnh cho vừa chữ)
        float smallBoxHeight = 15f; // Chiều cao khung nhỏ
        float smallBoxX = boxX; // Nằm sát mép trái cùng với khung to
        float smallBoxY = boxY + boxHeight; // Đặt đè ngay trên nóc khung to

        // VẼ KHUNG NỀN
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Nền đen mờ cho cả 2 khung
        shapeRenderer.setColor(0, 0, 0, 0.8f);
        shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight); // Khung to
        shapeRenderer.rect(smallBoxX, smallBoxY, smallBoxWidth, smallBoxHeight); // Khung nhỏ

        // Viền trắng
        shapeRenderer.setColor(Color.WHITE);

        // Viền khung to (chỉ vẽ viền dưới và viền trên)
        shapeRenderer.rectLine(boxX, boxY, boxX + boxWidth, boxY, 2f); // Viền dưới
        shapeRenderer.rectLine(boxX, boxY + boxHeight, boxX + boxWidth, boxY + boxHeight, 2f); // Viền trên

        // Viền khung nhỏ (Vẽ hình chữ U ngược: Trái, Trên, Phải)
        shapeRenderer.rectLine(smallBoxX, smallBoxY, smallBoxX, smallBoxY + smallBoxHeight, 2f); // Cạnh trái
        shapeRenderer.rectLine(smallBoxX, smallBoxY + smallBoxHeight, smallBoxX + smallBoxWidth, smallBoxY + smallBoxHeight, 2f); // Cạnh trên
        shapeRenderer.rectLine(smallBoxX + smallBoxWidth, smallBoxY, smallBoxX + smallBoxWidth, smallBoxY + smallBoxHeight, 2f); // Cạnh phải

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // VẼ CHỮ (SPRITE BATCH)
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Giữ lại cỡ chữ cũ đang dùng
        float originalScale = font.getData().scaleX;

        // Chữ cho khung nhỏ (Có thể ép nhỏ font xuống một chút cho đẹp)
        font.getData().setScale(originalScale * 0.8f);
        // Vì font bạn đã ép qua Hiero nên giờ gõ tiếng Việt có dấu vô tư!
        font.draw(batch, "Cô gái bí ẩn", smallBoxX + 10f, smallBoxY + smallBoxHeight - 6f);

        // Trả lại cỡ chữ bình thường và vẽ chữ cho khung to
        font.getData().setScale(originalScale);
        // Dòng mới: Tự động xuống dòng khi chữ chạm vách khung thoại
        font.draw(
            batch,
            currentTextToDraw,
            boxX + 15f,                          // Cách lề trái 15px
            boxY + boxHeight - 15f,              // Cách lề trên 15px
            boxWidth - 30f,                      // Chiều rộng tối đa của vùng chữ (trừ hao 2 bên viền)
            com.badlogic.gdx.utils.Align.left,   // Căn lề trái
            true                                 // Bật tính năng tự động xuống dòng (Wrap)
        );

        if (characterIndex >= dialogueLines.get(currentLineIndex).length()) {
            // Thu nhỏ font một chút để dòng hướng dẫn trông tinh tế hơn
            font.getData().setScale(originalScale * 0.75f);

            // Toạ độ X: Lùi lại khoảng 95 pixel so với mép phải khung to (Vừa đủ cho cụm chữ)
            float promptX = boxX + boxWidth - 90f;
            // Toạ độ Y: Cách cạnh đáy khung to 15 pixel
            float promptY = boxY + 15f;

            // Tạo hiệu ứng nhấp nháy theo thời gian thực (Cứ 500ms đổi trạng thái ẩn/hiện)
            if ((System.currentTimeMillis() / 500) % 2 == 0) {
                font.draw(batch, "Bấm G để tiếp tục", promptX, promptY);
            }

            // Trả lại scale gốc để tránh lỗi size font ở frame tiếp theo
            font.getData().setScale(originalScale);
        }
        batch.end();
    }

    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
    }
}
