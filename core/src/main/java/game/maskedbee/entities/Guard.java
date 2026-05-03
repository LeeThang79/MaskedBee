package game.maskedbee.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Guard extends Entity {
    public float patrolSpeed = 60f;
    public float chaseSpeed = 150f;

    public enum State { PATROL, SUSPICIOUS, INVESTIGATE, CHASE, RETURN }
    public State currentState = State.PATROL;

    public float alertLevel = 0f;
    private Vector2 lastKnownPos;

    public float visionRadius = 400f;
    public float viewAngle = 80f;
    private float rotation = 0f;
    private float startX, startY;

    private Array<Vector2> patrolPath;
    private int targetWaypointIndex = 0;
    private Animation<TextureRegion> walkAnimation;
    private boolean isFacingRight = true;

    public Guard(float startX, float startY, Array<Vector2> path) {
        super(startX, startY, 32, 40, 60f);
        this.startX = startX;
        this.startY = startY;
        this.patrolPath = path;
        this.lastKnownPos = new Vector2(startX, startY);

        Array<TextureRegion> frames = new Array<>();
        frames.add(new TextureRegion(new Texture("guard/g_walk_1.png")));
        frames.add(new TextureRegion(new Texture("guard/g_walk_2.png")));
        frames.add(new TextureRegion(new Texture("guard/g_walk_3.png")));
        frames.add(new TextureRegion(new Texture("guard/g_walk_4.png")));
        walkAnimation = new Animation<TextureRegion>(0.15f, frames);
    }

    public void update(float deltaTime, Player player, Array<Rectangle> walls) {
        stateTime += deltaTime;

        // ==========================================
        // 1. CẢM BIẾN (SENSORS)
        // ==========================================
        boolean seeingPlayer = false;
        boolean hearingPlayer = false;
        float distToPlayer = Vector2.dst(x, y, player.x, player.y);

        // Biến currentVisionRadius quyết định tầm nhìn thật sự của Guard
        float currentVisionRadius = player.isCreeping ? (visionRadius * 0.3f) : visionRadius;

        // A. Cảm biến MẮT
        if (distToPlayer <= currentVisionRadius) {
            if (currentState == State.CHASE) {
                seeingPlayer = true;
            } else {
                float angleToPlayer = MathUtils.atan2(player.y - (y+20), player.x - (x+16)) * MathUtils.radiansToDegrees;
                float angleDiff = Math.abs(angleToPlayer - rotation);
                if (angleDiff > 180) angleDiff = 360 - angleDiff;
                if (angleDiff <= viewAngle / 2f) seeingPlayer = true;
            }
        }

        // B. Cảm biến TAI
        if (player.noiseRadius > 0 && distToPlayer <= player.noiseRadius) {
            hearingPlayer = true;
        }

        // ==========================================
        // 2. THANH CẢNH BÁO
        // ==========================================
        if (seeingPlayer) {
            lastKnownPos.set(player.x, player.y);

            if (distToPlayer < 120f) {
                alertLevel += 200f * deltaTime; // Rất gần: Phát hiện ngay
            } else {
                float alertSpeed = player.isCreeping ? 25f : 100f;
                alertLevel += alertSpeed * deltaTime;
            }
        }
        else if (hearingPlayer) {
            lastKnownPos.set(player.x, player.y);
            alertLevel += 50f * deltaTime;
            if (alertLevel > 90f) alertLevel = 90f; // Nghe thấy chỉ lên max 90%
        }
        else {
            alertLevel -= 30f * deltaTime; // Tụt nhanh khi mất dấu
        }
        alertLevel = MathUtils.clamp(alertLevel, 0, 100);

        // ==========================================
        // 3. CHUYỂN TRẠNG THÁI AI
        // ==========================================
        if (alertLevel >= 100f) currentState = State.CHASE;
        else if (alertLevel > 0 && currentState == State.PATROL) currentState = State.SUSPICIOUS;
        else if (alertLevel < 100f && currentState == State.CHASE) currentState = State.INVESTIGATE;
        else if (alertLevel == 0 && (currentState == State.SUSPICIOUS || currentState == State.INVESTIGATE)) {
            currentState = State.RETURN;
            targetWaypointIndex = findNearestWaypointIndex();
        }

        // ==========================================
        // 4. DI CHUYỂN
        // ==========================================
        float moveX = 0, moveY = 0;
        float currentMoveSpeed = patrolSpeed;

        if (currentState == State.CHASE) {
            currentMoveSpeed = chaseSpeed;
            if (this.x < lastKnownPos.x) moveX = 1; else if (this.x > lastKnownPos.x) moveX = -1;
            if (this.y < lastKnownPos.y) moveY = 1; else if (this.y > lastKnownPos.y) moveY = -1;
        }
        else if (currentState == State.SUSPICIOUS) {
            rotation = MathUtils.atan2(lastKnownPos.y - (y+20), lastKnownPos.x - (x+16)) * MathUtils.radiansToDegrees;
            if (alertLevel > 60f) currentState = State.INVESTIGATE;
        }
        else if (currentState == State.INVESTIGATE) {
            if (this.x < lastKnownPos.x - 2) moveX = 1; else if (this.x > lastKnownPos.x + 2) moveX = -1;
            if (this.y < lastKnownPos.y - 2) moveY = 1; else if (this.y > lastKnownPos.y + 2) moveY = -1;
            if (Vector2.dst(x, y, lastKnownPos.x, lastKnownPos.y) < 10) {
                moveX = 0; moveY = 0;
                rotation += 100f * deltaTime;
            }
        }
        else if (currentState == State.RETURN || currentState == State.PATROL) {
            if (patrolPath != null && patrolPath.size > 0) {
                Vector2 target = patrolPath.get(targetWaypointIndex);
                if (this.x < target.x - 2) moveX = 1; else if (this.x > target.x + 2) moveX = -1;
                if (this.y < target.y - 2) moveY = 1; else if (this.y > target.y + 2) moveY = -1;

                if (Math.abs(x - target.x) < 6 && Math.abs(y - target.y) < 6) {
                    currentState = State.PATROL;
                    targetWaypointIndex = (targetWaypointIndex + 1) % patrolPath.size;
                }
            }
        }

        if (moveX != 0 || moveY != 0) rotation = MathUtils.atan2(moveY, moveX) * MathUtils.radiansToDegrees;
        if (moveX != 0 && moveY != 0) { moveX *= 0.707f; moveY *= 0.707f; }
        isFacingRight = (moveX >= 0);

        moveWithCollision(moveX * currentMoveSpeed * deltaTime, moveY * currentMoveSpeed * deltaTime, walls);

        // ==========================================
        // 5. FIX LỖI BẮT LIÊN TỤC (SPAWN KILL)
        // ==========================================
        if (this.hitbox.overlaps(player.hitbox)) {
            // Đưa Player về góc an toàn tít dưới cùng bên trái
            player.x = 50; player.y = 50;
            player.hitbox.setPosition(50, 50);

            // Đưa Guard về mốc số 0 và bắt nó đi tới mốc số 1 (Để nó quay mặt đi chỗ khác)
            if (patrolPath != null && patrolPath.size > 0) {
                Vector2 spawnPoint = patrolPath.get(0);
                this.x = spawnPoint.x;
                this.y = spawnPoint.y;
                this.hitbox.setPosition(this.x, this.y);
                this.targetWaypointIndex = 1;
            }

            // Tẩy não hoàn toàn
            this.currentState = State.PATROL;
            this.alertLevel = 0f;
            this.lastKnownPos.set(this.x, this.y); // Quên sạch vị trí cũ
        }
    }

    private int findNearestWaypointIndex() {
        if (patrolPath == null || patrolPath.size == 0) return 0;
        int nearest = 0; float minDst = Float.MAX_VALUE;
        for (int i = 0; i < patrolPath.size; i++) {
            float d = Vector2.dst(x, y, patrolPath.get(i).x, patrolPath.get(i).y);
            if (d < minDst) { minDst = d; nearest = i; }
        }
        return nearest;
    }

    @Override
    public void draw(SpriteBatch batch) {
        TextureRegion currentFrame = walkAnimation.getKeyFrame(stateTime, true);
        if (!isFacingRight && !currentFrame.isFlipX()) currentFrame.flip(true, false);
        else if (isFacingRight && currentFrame.isFlipX()) currentFrame.flip(true, false);
        batch.draw(currentFrame, x, y);
    }

    public void drawDebug(ShapeRenderer shape, Player player) {
        // Vẽ vòng ồn của Player
        if (player.noiseRadius > 0) {
            shape.setColor(Color.CYAN);
            shape.circle(player.x + 16, player.y + 20, player.noiseRadius);
        }

        // ==============================================
        // ĐÃ FIX LỖI HIỂN THỊ TAM GIÁC NHÌN (VISION CONE)
        // ==============================================
        // Thay vì dùng visionRadius cố định, bây giờ dùng currentRadius để vẽ!
        float currentRadius = player.isCreeping ? (visionRadius * 0.3f) : visionRadius;

        shape.setColor(currentState == State.CHASE ? Color.RED : (alertLevel > 0 ? Color.ORANGE : Color.YELLOW));
        float centerX = x + 16; float centerY = y + 20;

        // CÁC DÒNG DƯỚI ĐÂY ĐÃ ĐƯỢC THAY BẰNG currentRadius
        float x1 = centerX + MathUtils.cosDeg(rotation - viewAngle / 2f) * currentRadius;
        float y1 = centerY + MathUtils.sinDeg(rotation - viewAngle / 2f) * currentRadius;
        float x2 = centerX + MathUtils.cosDeg(rotation + viewAngle / 2f) * currentRadius;
        float y2 = centerY + MathUtils.sinDeg(rotation + viewAngle / 2f) * currentRadius;

        shape.line(centerX, centerY, x1, y1);
        shape.line(centerX, centerY, x2, y2);
        shape.line(x1, y1, x2, y2);

        // Vẽ thanh cảnh báo
        if (alertLevel > 0) {
            shape.end();
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(Color.BLACK);
            shape.rect(x, y + 45, 32, 6);
            shape.setColor(alertLevel == 100f ? Color.RED : Color.YELLOW);
            shape.rect(x + 1, y + 46, (30 * alertLevel / 100f), 4);
            shape.end();
            shape.begin(ShapeRenderer.ShapeType.Line);
        }
    }
}
