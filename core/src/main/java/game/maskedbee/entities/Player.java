package entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class Player {
    // --- TỌA ĐỘ VÀ TỐC ĐỘ ---
    public float x, y;
    public float walkSpeed = 150f;
    public float creepSpeed = 70f;
    public boolean isMoving = false;

    // --- VẬT LÝ VÀ VA CHẠM (HITBOX) ---
    public Rectangle hitbox; // "Cái Hồn" (Khung vật lý thật sự)
    private Rectangle futureHitbox = new Rectangle(); // Dùng để đi dò đường (Khắc phục lỗi tạo rác gây giật lag)

    // --- TRẠNG THÁI ---
    private float stateTime = 0f;
    public boolean isCreeping = false;
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

        // Khởi tạo Khung vật lý (Giả sử nhân vật rộng 32px, cao 40px)
        this.hitbox = new Rectangle(x, y, 32, 40);

        // ==========================================
        // 1. NẠP ẢNH ĐI BỘ (WALK)
        // ==========================================
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
        walkUpFrames.add(new TextureRegion(new Texture("main/hide_1.png"))); // Đóng thế
        walkUpFrames.add(new TextureRegion(new Texture("main/hide_1.png")));
        walkUpAnimation = new Animation<TextureRegion>(0.1f, walkUpFrames);
        idleUp = walkUpFrames.get(0);

        // Đi xuống (Tạm dùng walk_1.png đóng thế)
        Array<TextureRegion> walkDownFrames = new Array<>();
        walkDownFrames.add(new TextureRegion(new Texture("main/walk_1.png"))); // Đóng thế
        walkDownFrames.add(new TextureRegion(new Texture("main/walk_1.png")));
        walkDownAnimation = new Animation<TextureRegion>(0.1f, walkDownFrames);
        idleDown = walkDownFrames.get(0);

        // ==========================================
        // 2. NẠP ẢNH RÓN RÉN (CREEP)
        // ==========================================
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
        creepUpFrames.add(new TextureRegion(new Texture("main/hide_1.png"))); // Đóng thế
        creepUpFrames.add(new TextureRegion(new Texture("main/hide_1.png")));
        creepUpAnimation = new Animation<TextureRegion>(0.15f, creepUpFrames);
        idleCreepUp = creepUpFrames.get(0);

        // Creep xuống (Tạm dùng creep_1.png đóng thế - Chờ Thảo vẽ)
        Array<TextureRegion> creepDownFrames = new Array<>();
        creepDownFrames.add(new TextureRegion(new Texture("main/creep_1.png"))); // Đóng thế
        creepDownFrames.add(new TextureRegion(new Texture("main/creep_1.png")));
        creepDownAnimation = new Animation<TextureRegion>(0.15f, creepDownFrames);
        idleCreepDown = creepDownFrames.get(0);
    }

    public void update(float deltaTime, Array<Rectangle> walls) {
        stateTime += deltaTime;

        // 1. Kiểm tra phím Rón rén
        isCreeping = Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);
        float currentSpeed = isCreeping ? creepSpeed : walkSpeed;

        // 2. Nhận lệnh di chuyển
        boolean isLeft = Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A);
        boolean isRight = Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D);
        boolean isUp = Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W);
        boolean isDown = Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S);

        isMoving = isLeft || isRight || isUp || isDown; //Gán giá trị cho isMoving

        float moveX = 0, moveY = 0;
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

        // 4. Chuẩn hóa tốc độ đi chéo
        if (moveX != 0 && moveY != 0) {
            moveX *= 0.707f;
            moveY *= 0.707f;
        }

        // 5. VẬT LÝ VÀ VA CHẠM (Hitbox đi dò đường)
        float stepX = moveX * currentSpeed * deltaTime;
        float stepY = moveY * currentSpeed * deltaTime;

        // --- CHECK TRỤC X (NGANG) ---
        futureHitbox.set(hitbox.x + stepX, hitbox.y, hitbox.width, hitbox.height);
        boolean canMoveX = true;
        for (Rectangle wall : walls) {
            if (futureHitbox.overlaps(wall)) {
                canMoveX = false;
                break;
            }
        }
        if (canMoveX) {
            x += stepX;         // Xác đi
            hitbox.x = x;       // Hồn đi theo
        }

        // --- CHECK TRỤC Y (DỌC) ---
        futureHitbox.set(hitbox.x, hitbox.y + stepY, hitbox.width, hitbox.height);
        boolean canMoveY = true;
        for (Rectangle wall : walls) {
            if (futureHitbox.overlaps(wall)) {
                canMoveY = false;
                break;
            }
        }
        if (canMoveY) {
            y += stepY;         // Xác đi
            hitbox.y = y;       // Hồn đi theo
        }

        // 6. GIỚI HẠN BẢN ĐỒ (Không cho lọt ra khỏi viền màn hình)
        float mapWidth = 352f;
        float mapHeight = 256f;

        x = MathUtils.clamp(x, 0, mapWidth - hitbox.width);
        y = MathUtils.clamp(y, 0, mapHeight - hitbox.height);

        // Chốt vị trí cuối cùng để Xác và Hồn luôn khớp nhau 100%
        hitbox.setPosition(x, y);
    }

    public void draw(SpriteBatch batch) {
        TextureRegion currentFrame = idleDown;

        switch (currentDirection) {
            case UP:
                if (isMoving) currentFrame = isCreeping ? creepUpAnimation.getKeyFrame(stateTime, true) : walkUpAnimation.getKeyFrame(stateTime, true);
                else currentFrame = isCreeping ? idleCreepUp : idleUp;
                break;

            case DOWN:
                if (isMoving) currentFrame = isCreeping ? creepDownAnimation.getKeyFrame(stateTime, true) : walkDownAnimation.getKeyFrame(stateTime, true);
                else currentFrame = isCreeping ? idleCreepDown : idleDown;
                break;

            case LEFT:
                if (isMoving) currentFrame = isCreeping ? creepSideAnimation.getKeyFrame(stateTime, true) : walkSideAnimation.getKeyFrame(stateTime, true);
                else currentFrame = isCreeping ? idleCreepSide : idleSide;
                if (!currentFrame.isFlipX()) currentFrame.flip(true, false);
                break;

            case RIGHT:
                if (isMoving) currentFrame = isCreeping ? creepSideAnimation.getKeyFrame(stateTime, true) : walkSideAnimation.getKeyFrame(stateTime, true);
                else currentFrame = isCreeping ? idleCreepSide : idleSide;
                if (currentFrame.isFlipX()) currentFrame.flip(true, false);
                break;
        }
        batch.draw(currentFrame, x, y);
    }
}
