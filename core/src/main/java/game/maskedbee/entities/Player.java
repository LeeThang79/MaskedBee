package game.maskedbee.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class Player extends Entity {
    public float walkSpeed = 150f;
    public float creepSpeed = 70f;
    public boolean isCreeping = false;

    // ĐÃ THÊM LẠI BIẾN TIẾNG ỒN
    public float noiseRadius = 0f;

    public enum Direction { UP, DOWN, LEFT, RIGHT }
    private Direction currentDirection = Direction.DOWN;

    private Animation<TextureRegion> walkSideAnimation, walkUpAnimation, walkDownAnimation;
    private Animation<TextureRegion> creepSideAnimation, creepUpAnimation, creepDownAnimation;
    private TextureRegion idleSide, idleUp, idleDown, idleCreepSide, idleCreepUp, idleCreepDown;

    public Player(float startX, float startY) {
        super(startX, startY, 20, 16, 150f);

        // (PHẦN NẠP ẢNH CỦA BẠN - MÌNH RÚT GỌN CHÚT CHO ĐỠ DÀI NHƯNG VẪN ĐẦY ĐỦ LOGIC)
        Array<TextureRegion> frames = new Array<>();
        for(int i=1; i<=5; i++) frames.add(new TextureRegion(new Texture("main/walk_"+i+".png")));
        walkSideAnimation = new Animation<>(0.1f, frames);
        idleSide = frames.get(0);

        Array<TextureRegion> hideFrames = new Array<>();
        hideFrames.add(new TextureRegion(new Texture("main/hide_1.png")));
        hideFrames.add(new TextureRegion(new Texture("main/hide_1.png")));
        walkUpAnimation = new Animation<>(0.1f, hideFrames);
        idleUp = hideFrames.get(0);
        creepUpAnimation = walkUpAnimation; idleCreepUp = idleUp; // Đóng thế

        Array<TextureRegion> walkFrames = new Array<>();
        walkFrames.add(new TextureRegion(new Texture("main/walk_1.png")));
        walkFrames.add(new TextureRegion(new Texture("main/walk_1.png")));
        walkDownAnimation = new Animation<>(0.1f, walkFrames);
        idleDown = walkFrames.get(0);
        creepDownAnimation = walkDownAnimation; idleCreepDown = idleDown; // Đóng thế

        Array<TextureRegion> creepFrames = new Array<>();
        for(int i=1; i<=4; i++) creepFrames.add(new TextureRegion(new Texture("main/creep_"+i+".png")));
        creepFrames.add(new TextureRegion(new Texture("main/creep_1.png")));
        creepSideAnimation = new Animation<>(0.15f, creepFrames);
        idleCreepSide = creepFrames.get(0);
    }

    public void update(float deltaTime, Array<Rectangle> walls) {
        stateTime += deltaTime;

        isCreeping = Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);
        float currentSpeed = isCreeping ? creepSpeed : walkSpeed;

        float moveX = 0, moveY = 0;
        if (Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)) moveX -= 1;
        if (Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)) moveX += 1;
        if (Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)) moveY += 1;
        if (Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S)) moveY -= 1;

        if (moveX < 0) currentDirection = Direction.LEFT;
        else if (moveX > 0) currentDirection = Direction.RIGHT;
        else if (moveY > 0) currentDirection = Direction.UP;
        else if (moveY < 0) currentDirection = Direction.DOWN;

        // TÍNH TOÁN TIẾNG ỒN CHUẨN XÁC
        boolean isMoving = (moveX != 0 || moveY != 0);
        if (isMoving && !isCreeping) {
            noiseRadius = 180f; // Chạy bộ phát ra tiếng ồn
        } else {
            noiseRadius = 0f;   // Đứng im hoặc ngồi im lặng tuyệt đối
        }

        if (moveX != 0 && moveY != 0) { moveX *= 0.707f; moveY *= 0.707f; }
        moveWithCollision(moveX * currentSpeed * deltaTime, moveY * currentSpeed * deltaTime, walls);
    }

    @Override
    public void draw(SpriteBatch batch) {
        TextureRegion currentFrame = idleDown;
        boolean isMoving = Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)
            || Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)
            || Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)
            || Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S);

        switch (currentDirection) {
            case UP: currentFrame = isMoving ? (isCreeping?creepUpAnimation.getKeyFrame(stateTime,true):walkUpAnimation.getKeyFrame(stateTime,true)) : (isCreeping?idleCreepUp:idleUp); break;
            case DOWN: currentFrame = isMoving ? (isCreeping?creepDownAnimation.getKeyFrame(stateTime,true):walkDownAnimation.getKeyFrame(stateTime,true)) : (isCreeping?idleCreepDown:idleDown); break;
            case LEFT: currentFrame = isMoving ? (isCreeping?creepSideAnimation.getKeyFrame(stateTime,true):walkSideAnimation.getKeyFrame(stateTime,true)) : (isCreeping?idleCreepSide:idleSide); if (!currentFrame.isFlipX()) currentFrame.flip(true, false); break;
            case RIGHT: currentFrame = isMoving ? (isCreeping?creepSideAnimation.getKeyFrame(stateTime,true):walkSideAnimation.getKeyFrame(stateTime,true)) : (isCreeping?idleCreepSide:idleSide); if (currentFrame.isFlipX()) currentFrame.flip(true, false); break;
        }
        float drawX = x - (32 - hitbox.width) / 2f;
        float drawY = y; // Giữ nguyên đáy (chân) trùng khớp
        // Thay dòng batch.draw(currentFrame, x, y); cũ bằng dòng này:
        batch.draw(currentFrame, drawX, drawY);
    }
}
