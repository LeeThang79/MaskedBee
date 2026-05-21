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

    // Trạng thái cải trang Bee
    public boolean isBeeDisguised = false;

    // Sau này dùng cho điều kiện lấy vật phẩm
    public boolean hasKeyItem = false;
    public boolean hasMaskItem = false;

    public float noiseRadius = 0f;

    public enum Direction { UP, DOWN, LEFT, RIGHT }
    private Direction currentDirection = Direction.DOWN;

    private Animation<TextureRegion> walkSideAnimation, walkUpAnimation, walkDownAnimation;
    private Animation<TextureRegion> creepSideAnimation, creepUpAnimation, creepDownAnimation;

    private TextureRegion idleSide, idleUp, idleDown;
    private TextureRegion idleCreepSide, idleCreepUp, idleCreepDown;

    // Animation Bee
    private Animation<TextureRegion> beeWalkSideAnimation, beeWalkUpAnimation, beeWalkDownAnimation;
    private Animation<TextureRegion> beeCreepSideAnimation, beeCreepUpAnimation, beeCreepDownAnimation;

    private TextureRegion beeIdleSide, beeIdleUp, beeIdleDown;
    private TextureRegion beeIdleCreepSide, beeIdleCreepUp, beeIdleCreepDown;

    public Player(float startX, float startY) {
        super(startX, startY, 20, 16, 150f);

        // =========================
        // NORMAL PLAYER
        // =========================
        walkSideAnimation = loadAnimation("main/walk_", 5, 0.1f);
        idleSide = walkSideAnimation.getKeyFrame(0f);

        walkUpAnimation = loadAnimation("main/walk_back_", 4, 0.1f);
        idleUp = walkUpAnimation.getKeyFrame(0f);

        walkDownAnimation = loadAnimation("main/walk_front_", 4, 0.1f);
        idleDown = walkDownAnimation.getKeyFrame(0f);

        creepSideAnimation = loadAnimation("main/creep_", 4, 0.15f);
        idleCreepSide = creepSideAnimation.getKeyFrame(0f);

        // Nếu chưa có creep up/down riêng thì dùng tạm walk up/down
        creepUpAnimation = walkUpAnimation;
        idleCreepUp = idleUp;

        creepDownAnimation = walkDownAnimation;
        idleCreepDown = idleDown;

        // =========================
        // BEE DISGUISE PLAYER
        // =========================
        beeWalkSideAnimation = loadAnimation("main/walk_bee_", 5, 0.1f);
        beeIdleSide = beeWalkSideAnimation.getKeyFrame(0f);

        beeWalkUpAnimation = loadAnimation("main/walk_back_bee_", 4, 0.1f);
        beeIdleUp = beeWalkUpAnimation.getKeyFrame(0f);

        beeWalkDownAnimation = loadAnimation("main/walk_front_bee_", 4, 0.1f);
        beeIdleDown = beeWalkDownAnimation.getKeyFrame(0f);

        beeCreepSideAnimation = loadAnimation("main/creep_bee_", 4, 0.15f);
        beeIdleCreepSide = beeCreepSideAnimation.getKeyFrame(0f);

        // Nếu chưa cần animation crouch bee riêng cho lên/xuống thì dùng tạm walk bee
        beeCreepUpAnimation = beeWalkUpAnimation;
        beeIdleCreepUp = beeIdleUp;

        beeCreepDownAnimation = beeWalkDownAnimation;
        beeIdleCreepDown = beeIdleDown;
    }

    private Animation<TextureRegion> loadAnimation(String prefix, int frameCount, float frameDuration) {
        Array<TextureRegion> frames = new Array<>();

        for (int i = 1; i <= frameCount; i++) {
            frames.add(new TextureRegion(new Texture(prefix + i + ".png")));
        }

        return new Animation<>(frameDuration, frames);
    }

    public void update(float deltaTime, Array<Rectangle> walls) {
        stateTime += deltaTime;

        // Hiện tại cho bấm P để test mặc / tháo bộ Bee
        if (Gdx.input.isKeyJustPressed(Keys.P)) {
            isBeeDisguised = !isBeeDisguised;
        }

        /*
         * Sau này khi đã có key + mask thì đổi đoạn trên thành:
         *
         * if (Gdx.input.isKeyJustPressed(Keys.P) && hasKeyItem && hasMaskItem) {
         *     isBeeDisguised = !isBeeDisguised;
         * }
         */

        isCreeping = Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);
        float currentSpeed = isCreeping ? creepSpeed : walkSpeed;

        float moveX = 0f;
        float moveY = 0f;

        if (Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)) moveX -= 1f;
        if (Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)) moveX += 1f;
        if (Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)) moveY += 1f;
        if (Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S)) moveY -= 1f;

        if (moveX < 0) currentDirection = Direction.LEFT;
        else if (moveX > 0) currentDirection = Direction.RIGHT;
        else if (moveY > 0) currentDirection = Direction.UP;
        else if (moveY < 0) currentDirection = Direction.DOWN;

        boolean isMoving = moveX != 0f || moveY != 0f;

        // Khi mặc Bee thì không tạo tiếng ồn để guard không nghi ngờ
        if (isBeeDisguised) {
            noiseRadius = 0f;
        } else if (isMoving && !isCreeping) {
            noiseRadius = 100f; // map nhỏ nên giảm từ 180 xuống 100
        } else if (isMoving && isCreeping) {
            noiseRadius = 25f;
        } else {
            noiseRadius = 0f;
        }

        if (moveX != 0f && moveY != 0f) {
            moveX *= 0.707f;
            moveY *= 0.707f;
        }

        moveWithCollision(moveX * currentSpeed * deltaTime, moveY * currentSpeed * deltaTime, walls);
    }

    @Override
    public void draw(SpriteBatch batch) {
        boolean isMoving =
            Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)
                || Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)
                || Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)
                || Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S);

        TextureRegion currentFrame;

        if (isBeeDisguised) {
            currentFrame = getBeeFrame(isMoving);
        } else {
            currentFrame = getNormalFrame(isMoving);
        }

        float drawX = x - (32 - hitbox.width) / 2f;
        float drawY = y;

        batch.draw(currentFrame, drawX, drawY);
    }

    private TextureRegion getNormalFrame(boolean isMoving) {
        TextureRegion currentFrame = idleDown;

        switch (currentDirection) {
            case UP:
                currentFrame = isMoving
                    ? (isCreeping ? creepUpAnimation.getKeyFrame(stateTime, true) : walkUpAnimation.getKeyFrame(stateTime, true))
                    : (isCreeping ? idleCreepUp : idleUp);
                break;

            case DOWN:
                currentFrame = isMoving
                    ? (isCreeping ? creepDownAnimation.getKeyFrame(stateTime, true) : walkDownAnimation.getKeyFrame(stateTime, true))
                    : (isCreeping ? idleCreepDown : idleDown);
                break;

            case LEFT:
                currentFrame = isMoving
                    ? (isCreeping ? creepSideAnimation.getKeyFrame(stateTime, true) : walkSideAnimation.getKeyFrame(stateTime, true))
                    : (isCreeping ? idleCreepSide : idleSide);

                if (!currentFrame.isFlipX()) currentFrame.flip(true, false);
                break;

            case RIGHT:
                currentFrame = isMoving
                    ? (isCreeping ? creepSideAnimation.getKeyFrame(stateTime, true) : walkSideAnimation.getKeyFrame(stateTime, true))
                    : (isCreeping ? idleCreepSide : idleSide);

                if (currentFrame.isFlipX()) currentFrame.flip(true, false);
                break;
        }

        return currentFrame;
    }

    private TextureRegion getBeeFrame(boolean isMoving) {
        TextureRegion currentFrame = beeIdleDown;

        switch (currentDirection) {
            case UP:
                currentFrame = isMoving
                    ? (isCreeping ? beeCreepUpAnimation.getKeyFrame(stateTime, true) : beeWalkUpAnimation.getKeyFrame(stateTime, true))
                    : (isCreeping ? beeIdleCreepUp : beeIdleUp);
                break;

            case DOWN:
                currentFrame = isMoving
                    ? (isCreeping ? beeCreepDownAnimation.getKeyFrame(stateTime, true) : beeWalkDownAnimation.getKeyFrame(stateTime, true))
                    : (isCreeping ? beeIdleCreepDown : beeIdleDown);
                break;

            case LEFT:
                currentFrame = isMoving
                    ? (isCreeping ? beeCreepSideAnimation.getKeyFrame(stateTime, true) : beeWalkSideAnimation.getKeyFrame(stateTime, true))
                    : (isCreeping ? beeIdleCreepSide : beeIdleSide);

                if (!currentFrame.isFlipX()) currentFrame.flip(true, false);
                break;

            case RIGHT:
                currentFrame = isMoving
                    ? (isCreeping ? beeCreepSideAnimation.getKeyFrame(stateTime, true) : beeWalkSideAnimation.getKeyFrame(stateTime, true))
                    : (isCreeping ? beeIdleCreepSide : beeIdleSide);

                if (currentFrame.isFlipX()) currentFrame.flip(true, false);
                break;
        }

        return currentFrame;
    }
}
