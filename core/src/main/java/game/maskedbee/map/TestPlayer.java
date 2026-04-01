package game.maskedbee.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class TestPlayer {
    public float x, y;
    public float speed = 250;
    public Rectangle bounds;

    public TestPlayer(float startX, float startY) {
        this.x = startX;
        this.y = startY;
        // Tạo một khối vuông nhỏ 20x20 để làm nhân vật test
        this.bounds = new Rectangle(x, y, 20, 20);
    }

    public void update(float delta, GameScreen screen) {
        float oldX = x;
        float oldY = y;

        // Xử lý di chuyển bằng phím mũi tên hoặc WASD
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) x -= speed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) x += speed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) y += speed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) y -= speed * delta;

        // Cập nhật vị trí tạm thời cho khung va chạm
        bounds.setPosition(x, y);

        // KIỂM TRA VA CHẠM: Nếu vị trí mới đè lên tường, trả về vị trí cũ
        if (screen.isColliding(bounds)) {
            x = oldX;
            y = oldY;
            bounds.setPosition(x, y);
        }

        // Kiểm tra vùng chuyển map (Portal/Exit)
        screen.updateGameplayLogic(bounds);
    }

    public void draw(ShapeRenderer shape, OrthographicCamera camera) {
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(Color.RED); // Khối vuông màu đỏ
        shape.rect(x, y, bounds.width, bounds.height);
        shape.end();
    }
}
