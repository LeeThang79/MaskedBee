package game.maskedbee.map; // Đổi lại package nếu bạn để ở thư mục khác

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
        this.font.getData().setScale(0.5f);
        this.dialogueLines = new Array<>();
    }

    // 1. HÀM KÍCH HOẠT HỘI THOẠI
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

    // 2. HÀM CẬP NHẬT (Xử lý phím Enter & Hiệu ứng chữ)
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

        // Nhấn phím SPACE hoặc ENTER để qua câu thoại / Chữ chạy nhanh
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
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

    // 3. HÀM VẼ (Vẽ khung nền đen mờ và chữ)
    public void draw(SpriteBatch batch, OrthographicCamera camera) {
        if (!isShowing) return;

        // Tính toán vị trí UI (Cố định ở góc dưới màn hình, đi theo camera)
        float camX = camera.position.x;
        float camY = camera.position.y;
        float viewWidth = camera.viewportWidth;
        float viewHeight = camera.viewportHeight;

        float boxWidth = viewWidth * 0.8f; // Rộng 80% màn hình
        float boxHeight = 70f;
        float boxX = camX - (boxWidth / 2f);
        float boxY = camY - (viewHeight / 2f) + 10f; // Cách đáy 10 pixel

        // 1. Vẽ khung nền trong suốt (Opacity 80%)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Nền đen
        shapeRenderer.setColor(0, 0, 0, 0.8f);
        shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);

        // Viền trắng
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rectLine(boxX, boxY, boxX + boxWidth, boxY, 2f); // Viền dưới
        shapeRenderer.rectLine(boxX, boxY + boxHeight, boxX + boxWidth, boxY + boxHeight, 2f); // Viền trên

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 2. Vẽ chữ lên trên khung
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // Căn chữ lùi vào trong khung 10 pixel
        font.draw(batch, currentTextToDraw, boxX + 10f, boxY + boxHeight - 10f);

        // Vẽ nút nhấp nháy báo hiệu "Bấm tiếp"
        if (characterIndex >= dialogueLines.get(currentLineIndex).length()) {
            if ((System.currentTimeMillis() / 500) % 2 == 0) { // Nhấp nháy nửa giây
                font.draw(batch, "▼", boxX + boxWidth - 20f, boxY + 20f);
            }
        }
        batch.end();
    }

    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
    }
}
