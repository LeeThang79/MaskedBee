package game.maskedbee.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class Player extends Entity {
    public float walkSpeed = 150f;
    public float creepSpeed = 70f;

    public boolean isCreeping = false;
    public boolean isHidingAtStone = false;

    // Sau này dùng khi nhặt đủ vật phẩm
    public boolean hasKeyItem = false;
    public boolean hasMaskItem = false;

    public float noiseRadius = 0f;
    // Chìa khóa hiện tại Player đang giữ
    public boolean hasMask = false;
    public boolean isBeeDisguised = false;
    public String currentKey = "";

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private Direction currentDirection = Direction.DOWN;
    private Direction hideDirection = Direction.DOWN;

    private Rectangle currentHideStone = null;

    private static final float HIDE_RANGE = 30f;
    private static final float HIDE_GAP = 2f;

    private Animation<TextureRegion> walkSideAnimation;
    private Animation<TextureRegion> walkUpAnimation;
    private Animation<TextureRegion> walkDownAnimation;

    private Animation<TextureRegion> creepSideAnimation;
    private Animation<TextureRegion> creepUpAnimation;
    private Animation<TextureRegion> creepDownAnimation;

    private Animation<TextureRegion> beeWalkSideAnimation;
    private Animation<TextureRegion> beeWalkUpAnimation;
    private Animation<TextureRegion> beeWalkDownAnimation;

    private Animation<TextureRegion> beeCreepSideAnimation;
    private Animation<TextureRegion> beeCreepUpAnimation;
    private Animation<TextureRegion> beeCreepDownAnimation;

    private TextureRegion idleSide;
    private TextureRegion idleUp;
    private TextureRegion idleDown;

    private TextureRegion idleCreepSide;
    private TextureRegion idleCreepUp;
    private TextureRegion idleCreepDown;

    private TextureRegion beeIdleSide;
    private TextureRegion beeIdleUp;
    private TextureRegion beeIdleDown;

    private TextureRegion beeIdleCreepSide;
    private TextureRegion beeIdleCreepUp;
    private TextureRegion beeIdleCreepDown;

    // 4 ảnh núp thường
    private TextureRegion hideUp;
    private TextureRegion hideLeft;
    private TextureRegion hideRight;
    private TextureRegion hideDown;

    // 4 ảnh núp khi mặc bee
    private TextureRegion beeHideUp;
    private TextureRegion beeHideLeft;
    private TextureRegion beeHideRight;
    private TextureRegion beeHideDown;

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

        // Nếu bạn chưa có creep lên/xuống riêng thì dùng tạm walk lên/xuống
        creepUpAnimation = walkUpAnimation;
        idleCreepUp = idleUp;

        creepDownAnimation = walkDownAnimation;
        idleCreepDown = idleDown;

        // =========================
        // BEE PLAYER
        // =========================
        beeWalkSideAnimation = loadAnimation("main/walk_bee_", 5, 0.1f);
        beeIdleSide = beeWalkSideAnimation.getKeyFrame(0f);

        beeWalkUpAnimation = loadAnimation("main/walk_back_bee_", 4, 0.1f);
        beeIdleUp = beeWalkUpAnimation.getKeyFrame(0f);

        beeWalkDownAnimation = loadAnimation("main/walk_front_bee_", 4, 0.1f);
        beeIdleDown = beeWalkDownAnimation.getKeyFrame(0f);

        beeCreepSideAnimation = loadAnimation("main/creep_bee_", 4, 0.15f);
        beeIdleCreepSide = beeCreepSideAnimation.getKeyFrame(0f);

        beeCreepUpAnimation = beeWalkUpAnimation;
        beeIdleCreepUp = beeIdleUp;

        beeCreepDownAnimation = beeWalkDownAnimation;
        beeIdleCreepDown = beeIdleDown;

        // =========================
        // HIDE POSES
        // =========================
        hideUp = new TextureRegion(new Texture("main/hide_1.png"));
        hideLeft = new TextureRegion(new Texture("main/hide_2.png"));
        hideRight = new TextureRegion(new Texture("main/hide_3.png"));
        hideDown = new TextureRegion(new Texture("main/hide_4.png"));

        beeHideUp = new TextureRegion(new Texture("main/hide_bee_1.png"));
        beeHideLeft = new TextureRegion(new Texture("main/hide_bee_2.png"));
        beeHideRight = new TextureRegion(new Texture("main/hide_bee_3.png"));
        beeHideDown = new TextureRegion(new Texture("main/hide_bee_4.png"));
    }

    private Animation<TextureRegion> loadAnimation(String prefix, int frameCount, float frameDuration) {
        Array<TextureRegion> frames = new Array<>();

        for (int i = 1; i <= frameCount; i++) {
            frames.add(new TextureRegion(new Texture(prefix + i + ".png")));
        }

        return new Animation<>(frameDuration, frames);
    }

    // Giữ hàm update cũ để code hiện tại không bị lỗi
    public void update(float deltaTime, Array<Rectangle> walls) {
        // Test nhanh: dùng walls làm stones.
        // Sau này nên gọi update(deltaTime, walls, stoneCollisions).
        update(deltaTime, walls, walls);
    }

    // Hàm update mới: truyền riêng stoneCollisions để F chỉ núp vào cột Stone
    public void update(float deltaTime, Array<Rectangle> walls, Array<Rectangle> stones) {
        stateTime += deltaTime;

        boolean pressedHideKey = Gdx.input.isKeyJustPressed(Keys.F);

        // Bấm P để test mặc / tháo đồ bee
        if (Gdx.input.isKeyJustPressed(Keys.P)) {
            if (hasMask) {
                isBeeDisguised = !isBeeDisguised;
                System.out.println("Bee disguise: " + isBeeDisguised);
            } else {
                isBeeDisguised = false;
                System.out.println("Bạn chưa có mặt nạ nên chưa thể mặc bee.");
            }
        }
        // Bấm F để vào / thoát núp cột
        if (pressedHideKey) {
            if (isHidingAtStone) {
                exitHide();
            } else {
                Rectangle nearestStone = findNearestStone(stones);

                if (nearestStone != null) {
                    enterHide(nearestStone);
                }
            }
        }

        // Nếu đang núp thì đứng yên, không tiếng động
        if (isHidingAtStone) {
            noiseRadius = 0f;
            isCreeping = true;

            // Nếu đang núp mà bấm di chuyển thì rời khỏi cột
            // Trừ đúng frame vừa bấm F để vào núp
            if (!pressedHideKey && isMovementKeyPressed()) {
                exitHide();
            } else {
                return;
            }
        }

        isCreeping = Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)
            || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);

        float currentSpeed = isCreeping ? creepSpeed : walkSpeed;

        float moveX = 0f;
        float moveY = 0f;

        if (Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)) moveX -= 1f;
        if (Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)) moveX += 1f;
        if (Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)) moveY += 1f;
        if (Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S)) moveY -= 1f;

        if (moveX < 0f) currentDirection = Direction.LEFT;
        else if (moveX > 0f) currentDirection = Direction.RIGHT;
        else if (moveY > 0f) currentDirection = Direction.UP;
        else if (moveY < 0f) currentDirection = Direction.DOWN;

        boolean isMoving = moveX != 0f || moveY != 0f;

        // Tiếng ồn
        if (isBeeDisguised) {
            noiseRadius = 0f;
        } else if (isMoving && !isCreeping) {
            noiseRadius = 100f;
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

    private boolean isMovementKeyPressed() {
        return Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)
            || Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)
            || Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)
            || Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S);
    }

    private Rectangle findNearestStone(Array<Rectangle> stones) {
        if (stones == null) return null;

        float playerCenterX = hitbox.x + hitbox.width / 2f;
        float playerCenterY = hitbox.y + hitbox.height / 2f;

        Rectangle nearest = null;
        float bestDistance2 = HIDE_RANGE * HIDE_RANGE;

        for (Rectangle stone : stones) {
            float closestX = MathUtils.clamp(playerCenterX, stone.x, stone.x + stone.width);
            float closestY = MathUtils.clamp(playerCenterY, stone.y, stone.y + stone.height);

            float dx = playerCenterX - closestX;
            float dy = playerCenterY - closestY;

            float distance2 = dx * dx + dy * dy;

            if (distance2 <= bestDistance2) {
                bestDistance2 = distance2;
                nearest = stone;
            }
        }

        return nearest;
    }

    private void enterHide(Rectangle stone) {
        currentHideStone = stone;

        float playerCenterX = hitbox.x + hitbox.width / 2f;
        float playerCenterY = hitbox.y + hitbox.height / 2f;

        float stoneCenterX = stone.x + stone.width / 2f;
        float stoneCenterY = stone.y + stone.height / 2f;

        float dx = playerCenterX - stoneCenterX;
        float dy = playerCenterY - stoneCenterY;

        if (Math.abs(dx) > Math.abs(dy)) {
            // Player ở trái hoặc phải cột
            if (dx < 0f) {
                // Đứng bên trái cột, quay mặt vào cột
                hideDirection = Direction.RIGHT;
                x = stone.x - hitbox.width - HIDE_GAP;
            } else {
                // Đứng bên phải cột, quay mặt vào cột
                hideDirection = Direction.LEFT;
                x = stone.x + stone.width + HIDE_GAP;
            }

            y = stoneCenterY - hitbox.height / 2f;
        } else {
            // Player ở trên hoặc dưới cột
            if (dy < 0f) {
                // Đứng dưới cột, quay lên
                hideDirection = Direction.UP;
                y = stone.y - hitbox.height - HIDE_GAP;
            } else {
                // Đứng trên cột, quay xuống
                hideDirection = Direction.DOWN;
                y = stone.y + stone.height + HIDE_GAP;
            }

            x = stoneCenterX - hitbox.width / 2f;
        }

        x = MathUtils.clamp(x, 0, Gdx.graphics.getWidth() - hitbox.width);
        y = MathUtils.clamp(y, 0, Gdx.graphics.getHeight() - hitbox.height);

        hitbox.setPosition(x, y);

        currentDirection = hideDirection;
        isHidingAtStone = true;
        isCreeping = true;
        noiseRadius = 0f;
    }

    private void exitHide() {
        isHidingAtStone = false;
        currentHideStone = null;
    }

    private TextureRegion getHideFrame() {
        if (isBeeDisguised) {
            switch (hideDirection) {
                case UP: return beeHideUp;
                case LEFT: return beeHideLeft;
                case RIGHT: return beeHideRight;
                case DOWN:
                default: return beeHideDown;
            }
        }

        switch (hideDirection) {
            case UP: return hideUp;
            case LEFT: return hideLeft;
            case RIGHT: return hideRight;
            case DOWN:
            default: return hideDown;
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        TextureRegion currentFrame;

        boolean isMoving = Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)
            || Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)
            || Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)
            || Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S);

        if (isHidingAtStone) {
            currentFrame = getHideFrame();
        } else if (isBeeDisguised) {
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
