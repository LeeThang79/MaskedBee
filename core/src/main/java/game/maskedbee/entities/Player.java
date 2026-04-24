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

    // BIẾN TIẾNG ỒN
    public float noiseRadius = 0f;

    public enum Direction { UP, DOWN, LEFT, RIGHT }
    private Direction currentDirection = Direction.DOWN;

    private Animation<TextureRegion> walkSideAnimation, walkUpAnimation, walkDownAnimation;
    private Animation<TextureRegion> creepSideAnimation, creepUpAnimation, creepDownAnimation;
    private TextureRegion idleSide, idleUp, idleDown, idleCreepSide, idleCreepUp, idleCreepDown;

    public Player(float startX, float startY) {
        super(startX, startY, 20, 10, 150f); // 32, 40

        Array<TextureRegion> walkSideFrames = new Array<>();
        walkSideFrames.add(new TextureRegion(new Texture("main/walk_1.png")));
        walkSideFrames.add(new TextureRegion(new Texture("main/walk_2.png")));
        walkSideFrames.add(new TextureRegion(new Texture("main/walk_3.png")));
        walkSideFrames.add(new TextureRegion(new Texture("main/walk_4.png")));
        walkSideFrames.add(new TextureRegion(new Texture("main/walk_5.png")));
        walkSideAnimation = new Animation<TextureRegion>(0.1f, walkSideFrames);
        idleSide = walkSideFrames.get(0);

        Array<TextureRegion> walkUpFrames = new Array<>();
        walkUpFrames.add(new TextureRegion(new Texture("main/hide_1.png")));
        walkUpFrames.add(new TextureRegion(new Texture("main/hide_1.png")));
        walkUpAnimation = new Animation<TextureRegion>(0.1f, walkUpFrames);
        idleUp = walkUpFrames.get(0);

        Array<TextureRegion> walkDownFrames = new Array<>();
        walkDownFrames.add(new TextureRegion(new Texture("main/walk_1.png")));
        walkDownFrames.add(new TextureRegion(new Texture("main/walk_1.png")));
        walkDownAnimation = new Animation<TextureRegion>(0.1f, walkDownFrames);
        idleDown = walkDownFrames.get(0);

        Array<TextureRegion> creepSideFrames = new Array<>();
        creepSideFrames.add(new TextureRegion(new Texture("main/creep_1.png")));
        creepSideFrames.add(new TextureRegion(new Texture("main/creep_2.png")));
        creepSideFrames.add(new TextureRegion(new Texture("main/creep_3.png")));
        creepSideFrames.add(new TextureRegion(new Texture("main/creep_4.png")));
        creepSideFrames.add(new TextureRegion(new Texture("main/creep_1.png")));
        creepSideAnimation = new Animation<TextureRegion>(0.15f, creepSideFrames);
        idleCreepSide = creepSideFrames.get(0);

        Array<TextureRegion> creepUpFrames = new Array<>();
        creepUpFrames.add(new TextureRegion(new Texture("main/hide_1.png")));
        creepUpFrames.add(new TextureRegion(new Texture("main/hide_1.png")));
        creepUpAnimation = new Animation<TextureRegion>(0.15f, creepUpFrames);
        idleCreepUp = creepUpFrames.get(0);

        Array<TextureRegion> creepDownFrames = new Array<>();
        creepDownFrames.add(new TextureRegion(new Texture("main/creep_1.png")));
        creepDownFrames.add(new TextureRegion(new Texture("main/creep_1.png")));
        creepDownAnimation = new Animation<TextureRegion>(0.15f, creepDownFrames);
        idleCreepDown = creepDownFrames.get(0);
    }

    public void update(float deltaTime, Array<Rectangle> walls) {
        stateTime += deltaTime;
        if (Gdx.input.isKeyJustPressed(Keys.L)) {
            System.out.println("Hiện đang có: " + walls.size + " bức tường trong Map");
        }
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
            noiseRadius = 180f;
        } else {
            noiseRadius = 0f;
        }

        if (moveX != 0 && moveY != 0) { moveX *= 0.707f; moveY *= 0.707f; }

        moveWithCollision(moveX * currentSpeed * deltaTime, moveY * currentSpeed * deltaTime, walls);
        hitbox.setPosition(x + 6, y);
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

        batch.draw(currentFrame, x, y);
    }
}
