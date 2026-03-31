package game.maskedbee.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Guard {
    public float x, y;
    public float speed = 90f;
    public Rectangle hitbox;
    private Rectangle futureHitbox = new Rectangle();

    public enum State { PATROL, CHASE, RETURN }
    public State currentState = State.PATROL;

    public float visionRadius = 250f;
    private float startX, startY;
    private boolean movingRight = true;
    private float patrolDistance = 150f;

    private float stateTime = 0f;
    private Animation<TextureRegion> walkAnimation;
    private boolean isFacingRight = true;

    // ==========================================
    // BỘ BIẾN "ROBOT HÚT BỤI" CHỐNG KẸT TƯỜNG
    // ==========================================
    private float stuckTimer = 0f;      // Thời gian đang bị kẹt
    private float bypassTimer = 0f;     // Thời gian đang đi lách tường
    private float bypassDirX = 0f;      // Hướng lách X
    private float bypassDirY = 0f;      // Hướng lách Y

    public Guard(float startX, float startY) {
        this.x = startX; this.y = startY;
        this.startX = startX; this.startY = startY;
        this.hitbox = new Rectangle(x, y, 32, 40);

        Array<TextureRegion> frames = new Array<>();
        frames.add(new TextureRegion(new Texture("guard/g_walk_1.png")));
        frames.add(new TextureRegion(new Texture("guard/g_walk_2.png")));
        frames.add(new TextureRegion(new Texture("guard/g_walk_3.png")));
        frames.add(new TextureRegion(new Texture("guard/g_walk_4.png")));
        walkAnimation = new Animation<TextureRegion>(0.15f, frames);
    }

    public void update(float deltaTime, Player player, Array<Rectangle> walls) {
        stateTime += deltaTime;

        float distanceToPlayer = Vector2.dst(x, y, player.x, player.y);
        float currentVision = player.isCreeping ? (visionRadius * 0.5f) : visionRadius;

        if (distanceToPlayer <= currentVision) {
            currentState = State.CHASE;
            bypassTimer = 0f; // Bỏ chế độ lách tường nếu thấy player
        } else if (currentState == State.CHASE) {
            currentState = State.RETURN;
            stuckTimer = 0f;
        }

        float moveX = 0, moveY = 0;

        if (currentState == State.CHASE) {
            //[RƯỢT ĐUỔI]
            if (this.x < player.x) moveX = 1; else if (this.x > player.x) moveX = -1;
            if (this.y < player.y) moveY = 1; else if (this.y > player.y) moveY = -1;
        }
        else if (currentState == State.RETURN) {
            //[TÌM ĐƯỜNG VỀ NHÀ]

            // Nếu đang trong chế độ "Nổi cáu lách tường" -> Đi theo hướng lách
            if (bypassTimer > 0) {
                bypassTimer -= deltaTime;
                moveX = bypassDirX;
                moveY = bypassDirY;
            }
            // Nếu bình thường -> Đi đường chim bay về nhà
            else {
                float dx = startX - this.x;
                float dy = startY - this.y;

                if (dx > 2) moveX = 1; else if (dx < -2) moveX = -1;
                if (dy > 2) moveY = 1; else if (dy < -2) moveY = -1;

                // Về sát nhà -> Đi tuần tra
                if (Math.abs(dx) <= 5 && Math.abs(dy) <= 5) {
                    this.x = startX; this.y = startY;
                    currentState = State.PATROL;
                }
            }
        }
        else if (currentState == State.PATROL) {
            // [TUẦN TRA]
            if (movingRight) {
                moveX = 1;
                if (this.x > startX + patrolDistance) movingRight = false;
            } else {
                moveX = -1;
                if (this.x < startX - patrolDistance) movingRight = true;
            }
        }

        if (moveX != 0 && moveY != 0) { moveX *= 0.707f; moveY *= 0.707f; }
        if (moveX > 0) isFacingRight = true; else if (moveX < 0) isFacingRight = false;

        // ==========================================
        // VẬT LÝ VÀ KÍCH HOẠT CHỐNG KẸT
        // ==========================================
        float stepX = moveX * speed * deltaTime;
        float stepY = moveY * speed * deltaTime;

        // --- CHECK X ---
        futureHitbox.set(hitbox.x + stepX, hitbox.y, hitbox.width, hitbox.height);
        boolean canMoveX = true;
        for (Rectangle wall : walls) {
            if (futureHitbox.overlaps(wall)) { canMoveX = false; break; }
        }
        if (canMoveX) { x += stepX; hitbox.x = x; }

        // --- CHECK Y ---
        futureHitbox.set(hitbox.x, hitbox.y + stepY, hitbox.width, hitbox.height);
        boolean canMoveY = true;
        for (Rectangle wall : walls) {
            if (futureHitbox.overlaps(wall)) { canMoveY = false; break; }
        }
        if (canMoveY) { y += stepY; hitbox.y = y; }

        hitbox.setPosition(x, y);

        // --- CƠ CHẾ ROBOT HÚT BỤI KÍCH HOẠT Ở ĐÂY ---
        if (currentState == State.RETURN && bypassTimer <= 0) {
            // Nếu AI muốn đi trục X nhưng bị tường cản
            if (!canMoveX && moveX != 0) {
                stuckTimer += deltaTime;
                if (stuckTimer > 0.5f) { // Kẹt 0.5 giây
                    bypassTimer = 1.5f;  // Lách trong 1.5 giây
                    bypassDirX = 0f;     // Bỏ trục X đi
                    bypassDirY = (Math.random() > 0.5) ? 1f : -1f; // Hên xui lách lên hoặc xuống
                    stuckTimer = 0f;
                }
            }
            // Nếu AI muốn đi trục Y nhưng bị tường cản
            else if (!canMoveY && moveY != 0) {
                stuckTimer += deltaTime;
                if (stuckTimer > 0.5f) {
                    bypassTimer = 1.5f;
                    bypassDirY = 0f;
                    bypassDirX = (Math.random() > 0.5) ? 1f : -1f; // Hên xui lách trái hoặc phải
                    stuckTimer = 0f;
                }
            }
            // Nếu không bị kẹt thì reset đồng hồ
            else {
                stuckTimer = 0f;
            }
        }

        // ==========================================
        // GAME OVER CHECK
        // ==========================================
        if (this.hitbox.overlaps(player.hitbox)) {
            player.x = 100; player.y = 100;
            player.hitbox.setPosition(100, 100);
            System.out.println("BỊ BẮT!!! QUAY LẠI TỪ ĐẦU!");
        }
    }

    public void draw(SpriteBatch batch) {
        TextureRegion currentFrame = walkAnimation.getKeyFrame(stateTime, true);
        if (!isFacingRight && !currentFrame.isFlipX()) currentFrame.flip(true, false);
        else if (isFacingRight && currentFrame.isFlipX()) currentFrame.flip(true, false);
        batch.draw(currentFrame, x, y);
    }
}
