package game.maskedbee.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class Player {
    // Tọa độ và tốc độ
    public float x, y;
    public float walkSpeed = 150f;
    public float creepSpeed = 70f;

    // Các biến trạng thái
    private float stateTime = 0f;
    public boolean isCreeping = false;

    // Quản lý hướng quay mặt
    public enum Direction { UP, DOWN, LEFT, RIGHT }
    private Direction currentDirection = Direction.DOWN; // Mới vào game cho quay mặt xuống

    // --- HOẠT ẢNH ĐI BỘ (WALK) ---
    private Animation<TextureRegion> walkSideAnimation;
    private Animation<TextureRegion> walkUpAnimation;
    private Animation<TextureRegion> walkDownAnimation;

    // --- HOẠT ẢNH RÓN RÉN (CREEP) ---
    private Animation<TextureRegion> creepSideAnimation;
    private Animation<TextureRegion> creepUpAnimation;
    private Animation<TextureRegion> creepDownAnimation;

    // --- ẢNH ĐỨNG IM (IDLE) ---
    private TextureRegion idleSide;
    private TextureRegion idleUp;
    private TextureRegion idleDown;
    //--- ẢNH ĐỨNG IM RÓN RÉN (IDLE CREEP) ---
    private TextureRegion idleCreepSide;
    private TextureRegion idleCreepUp;
    private TextureRegion idleCreepDown;

    public Player(float startX, float startY) {
        this.x = startX;
        this.y = startY;

        // ==========================================
        // 1. NẠP ẢNH ĐI BỘ (WALK)
        // ==========================================

        // Đi ngang (Trái/Phải)
        Array<TextureRegion> walkSideFrames = new Array<>();
        walkSideFrames.add(new TextureRegion(new Texture("main/walk_1.png")));
        walkSideFrames.add(new TextureRegion(new Texture("main/walk_2.png")));
        walkSideFrames.add(new TextureRegion(new Texture("main/walk_3.png")));
        walkSideFrames.add(new TextureRegion(new Texture("main/walk_4.png")));
        walkSideFrames.add(new TextureRegion(new Texture("main/walk_5.png")));
        walkSideAnimation = new Animation<TextureRegion>(0.1f, walkSideFrames);
        idleSide = walkSideFrames.get(0); // Lấy ảnh walk_1 làm ảnh đứng im

        // Đi lên (Tạm dùng hide_1.png đóng thế)
        Array<TextureRegion> walkUpFrames = new Array<>();
        walkUpFrames.add(new TextureRegion(new Texture("main/hide_1.png")));
        walkUpFrames.add(new TextureRegion(new Texture("main/hide_1.png")));
        walkUpAnimation = new Animation<TextureRegion>(0.1f, walkUpFrames);
        idleUp = walkUpFrames.get(0);

        // Đi xuống (Tạm dùng walk_1.png đóng thế)
        Array<TextureRegion> walkDownFrames = new Array<>();
        walkDownFrames.add(new TextureRegion(new Texture("main/walk_1.png")));
        walkDownFrames.add(new TextureRegion(new Texture("main/walk_1.png")));
        walkDownAnimation = new Animation<TextureRegion>(0.1f, walkDownFrames);
        idleDown = walkDownFrames.get(0);


        // ==========================================
        // 2. NẠP ẢNH RÓN RÉN (CREEP)
        // ==========================================

        // Creep ngang (Trái/Phải)
        Array<TextureRegion> creepSideFrames = new Array<>();
        creepSideFrames.add(new TextureRegion(new Texture("main/creep_1.png")));
        creepSideFrames.add(new TextureRegion(new Texture("main/creep_2.png")));
        creepSideFrames.add(new TextureRegion(new Texture("main/creep_3.png")));
        creepSideFrames.add(new TextureRegion(new Texture("main/creep_4.png")));
        creepSideFrames.add(new TextureRegion(new Texture("main/creep_1.png")));
        creepSideAnimation = new Animation<TextureRegion>(0.15f, creepSideFrames); // Tốc độ creep chậm hơn xíu (0.15f)
        idleCreepSide = creepSideFrames.get(0); // THÊM DÒNG NÀY: Lấy ảnh số 1 làm ảnh đứng im rón rén

        // Creep lên (Tạm dùng hide_1.png đóng thế - Chờ Thảo vẽ)
        Array<TextureRegion> creepUpFrames = new Array<>();
        creepUpFrames.add(new TextureRegion(new Texture("main/hide_1.png")));
        creepUpFrames.add(new TextureRegion(new Texture("main/hide_1.png")));
        creepUpAnimation = new Animation<TextureRegion>(0.15f, creepUpFrames);
        idleCreepUp = creepUpFrames.get(0);

        // Creep xuống (Tạm dùng creep_1.png đóng thế - Chờ Thảo vẽ)
        Array<TextureRegion> creepDownFrames = new Array<>();
        creepDownFrames.add(new TextureRegion(new Texture("main/creep_1.png")));
        creepDownFrames.add(new TextureRegion(new Texture("main/creep_1.png")));
        creepDownAnimation = new Animation<TextureRegion>(0.15f, creepDownFrames);
        idleCreepDown = creepDownFrames.get(0);
    }

    public void update(float deltaTime) {
        // Cập nhật đồng hồ thời gian cho Animation
        stateTime += deltaTime;

        // 1. Kiểm tra xem có đang giữ phím Ctrl không
        isCreeping = Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);

        // Chốt tốc độ hiện tại
        float currentSpeed = isCreeping ? creepSpeed : walkSpeed;

        // 2. Kiểm tra phím bấm (Độc lập từng trục để đi chéo được)
        boolean isLeft = Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A);
        boolean isRight = Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D);
        boolean isUp = Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W);
        boolean isDown = Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S);

        float moveX = 0;
        float moveY = 0;

        if (isLeft) moveX -= 1;
        if (isRight) moveX += 1;
        if (isUp) moveY += 1;
        if (isDown) moveY -= 1;

        // 3. Cập nhật hướng quay mặt (Ưu tiên nhìn ngang khi đi chéo)
        if (moveX < 0) {
            currentDirection = Direction.LEFT;
        } else if (moveX > 0) {
            currentDirection = Direction.RIGHT;
        } else if (moveY > 0) {
            currentDirection = Direction.UP;
        } else if (moveY < 0) {
            currentDirection = Direction.DOWN;
        }

        // 4. Công thức toán học: Ghìm tốc độ khi đi chéo
        if (moveX != 0 && moveY != 0) {
            moveX *= 0.707f;
            moveY *= 0.707f;
        }

        // 5. Cập nhật tọa độ thật của nhân vật
        x += moveX * currentSpeed * deltaTime;
        y += moveY * currentSpeed * deltaTime;
    }

    public void draw(SpriteBatch batch) {
        TextureRegion currentFrame = idleDown;

        // Kiểm tra xem nhân vật có đang di chuyển không
        boolean isMoving = Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)
            || Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)
            || Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)
            || Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S);

        // Quyết định vẽ bức ảnh nào dựa trên HƯỚNG MẶT
        switch (currentDirection) {
            case UP:
                if (isMoving) {
                    currentFrame = isCreeping ? creepUpAnimation.getKeyFrame(stateTime, true) : walkUpAnimation.getKeyFrame(stateTime, true);
                } else {
                    // NẾU ĐỨNG IM: Kiểm tra xem có đang giữ Ctrl không để chọn dáng đứng
                    currentFrame = isCreeping ? idleCreepUp : idleUp;
                }
                break;

            case DOWN:
                if (isMoving) {
                    currentFrame = isCreeping ? creepDownAnimation.getKeyFrame(stateTime, true) : walkDownAnimation.getKeyFrame(stateTime, true);
                } else {
                    currentFrame = isCreeping ? idleCreepDown : idleDown;
                }
                break;

            case LEFT:
                if (isMoving) {
                    currentFrame = isCreeping ? creepSideAnimation.getKeyFrame(stateTime, true) : walkSideAnimation.getKeyFrame(stateTime, true);
                } else {
                    currentFrame = isCreeping ? idleCreepSide : idleSide;
                }
                // Lật ảnh nếu cần
                if (!currentFrame.isFlipX()) {
                    currentFrame.flip(true, false);
                }
                break;

            case RIGHT:
                if (isMoving) {
                    currentFrame = isCreeping ? creepSideAnimation.getKeyFrame(stateTime, true) : walkSideAnimation.getKeyFrame(stateTime, true);
                } else {
                    currentFrame = isCreeping ? idleCreepSide : idleSide;
                }
                // Lật ảnh nếu cần
                if (currentFrame.isFlipX()) {
                    currentFrame.flip(true, false);
                }
                break;
        }

        // Vẽ ảnh ra màn hình
        batch.draw(currentFrame, x, y);
    }
}
