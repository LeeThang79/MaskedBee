package game.maskedbee.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Boss extends Entity {
    public enum State {
        PATROL,
        CHASE,
        SEARCH
    }

    public State currentState = State.PATROL;

    public float patrolSpeed = 45f;
    public float chaseSpeed = 250f;

    // Boss quan sát theo vòng tròn 360 độ quanh nó.
    public float visionRadius = 130f;

    private float rotation = 0f;
    private float lostSightTimer = 0f;
    private float searchTimer = 0f;

    private static final float LOSE_SIGHT_TIME = 1.2f;
    private static final float SEARCH_TIME = 1.6f;

    private final Vector2 lastKnownPlayerPos = new Vector2();
    private final Vector2 tmpCenter = new Vector2();
    private final Vector2 tmpTarget = new Vector2();

    private Array<Vector2> patrolPath;
    private int targetWaypointIndex = 0;

    private Animation<TextureRegion> walkAnimation;

    /*
     * Nếu ảnh gốc ritualer_1.png đang quay sang TRÁI thì để false.
     * Nếu ảnh gốc ritualer_1.png đang quay sang PHẢI thì đổi thành true.
     */
    private static final boolean SPRITE_FACES_RIGHT_BY_DEFAULT = false;

    private boolean isFacingRight = false;

    public Boss(float startX, float startY, Array<Vector2> patrolPath) {
        super(startX, startY, 24, 28, 40f);

        this.patrolPath = patrolPath;

        if (patrolPath != null && patrolPath.size > 0) {
            this.x = patrolPath.get(0).x;
            this.y = patrolPath.get(0).y;
            this.hitbox.setPosition(this.x, this.y);
        }

        lastKnownPlayerPos.set(this.x, this.y);

        Array<TextureRegion> frames = new Array<>();
        frames.add(new TextureRegion(new Texture("ritualer/ritualer_1.png")));
        frames.add(new TextureRegion(new Texture("ritualer/ritualer_2.png")));
        frames.add(new TextureRegion(new Texture("ritualer/ritualer_3.png")));
        frames.add(new TextureRegion(new Texture("ritualer/ritualer_4.png")));

        walkAnimation = new Animation<>(0.18f, frames, Animation.PlayMode.LOOP);
    }

    /**
     * return true nếu boss bắt được Player.
     *
     * skullVisionBlockers:
     * - Không còn là vùng tàng hình.
     * - Chỉ dùng để chặn tầm nhìn của boss giống như vật cản.
     */
    public boolean update(
        float deltaTime,
        Player player,
        Array<Rectangle> walls,
        Array<Rectangle> skullVisionBlockers
    ) {
        stateTime += deltaTime;

        boolean canSeePlayer = canSeePlayer(player, walls, skullVisionBlockers);

        Vector2 bossCenter = getCenter(tmpCenter);
        Vector2 playerCenter = getPlayerCenter(player, tmpTarget);

        // Nhìn thấy Player trong vòng tròn quan sát là đuổi luôn.
        if (canSeePlayer) {
            lastKnownPlayerPos.set(playerCenter);
            lostSightTimer = 0f;
            searchTimer = 0f;

            currentState = State.CHASE;
        }

        if (currentState == State.CHASE) {
            if (canSeePlayer) {
                lastKnownPlayerPos.set(playerCenter);
                moveToward(lastKnownPlayerPos, chaseSpeed, deltaTime, walls);
            } else {
                lostSightTimer += deltaTime;

                if (lostSightTimer >= LOSE_SIGHT_TIME) {
                    currentState = State.SEARCH;
                    searchTimer = 0f;
                } else {
                    moveToward(lastKnownPlayerPos, chaseSpeed * 0.85f, deltaTime, walls);
                }
            }
        } else if (currentState == State.SEARCH) {
            float distanceToLastKnown = bossCenter.dst(lastKnownPlayerPos);

            if (distanceToLastKnown > 8f) {
                moveToward(lastKnownPlayerPos, patrolSpeed, deltaTime, walls);
            } else {
                rotation += 130f * deltaTime;
                searchTimer += deltaTime;

                if (searchTimer >= SEARCH_TIME) {
                    currentState = State.PATROL;
                    targetWaypointIndex = findNearestWaypointIndex();
                }
            }
        } else {
            patrol(deltaTime, walls);
        }

        /*
         * Boss chạm Player là bắt luôn.
         * Skull_Collision KHÔNG còn làm player miễn bắt nữa.
         */
        if (this.hitbox.overlaps(player.hitbox)) {
            return true;
        }

        return false;
    }

    private void patrol(float deltaTime, Array<Rectangle> walls) {
        if (patrolPath == null || patrolPath.size == 0) {
            rotation += 40f * deltaTime;
            return;
        }

        Vector2 target = patrolPath.get(targetWaypointIndex);
        moveToward(target, patrolSpeed, deltaTime, walls);

        Vector2 center = getCenter(tmpCenter);

        if (center.dst(target) < 8f) {
            targetWaypointIndex = (targetWaypointIndex + 1) % patrolPath.size;
        }
    }

    private void moveToward(Vector2 target, float moveSpeed, float deltaTime, Array<Rectangle> walls) {
        Vector2 center = getCenter(tmpCenter);

        float dx = target.x - center.x;
        float dy = target.y - center.y;

        if (Math.abs(dx) < 1f && Math.abs(dy) < 1f) {
            return;
        }

        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0f) return;

        dx /= len;
        dy /= len;

        rotation = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;

        if (Math.abs(dx) > 0.05f) {
            isFacingRight = dx >= 0f;
        }

        moveWithCollision(dx * moveSpeed * deltaTime, dy * moveSpeed * deltaTime, walls);
    }

    private boolean canSeePlayer(
        Player player,
        Array<Rectangle> walls,
        Array<Rectangle> skullVisionBlockers
    ) {
        Vector2 bossCenter = getCenter(tmpCenter);
        Vector2 playerCenter = getPlayerCenter(player, tmpTarget);

        float dist = bossCenter.dst(playerCenter);

        // Chỉ cần nằm trong bán kính vòng tròn là có thể bị thấy.
        if (dist > visionRadius) {
            return false;
        }

        // Không kiểm tra góc nhìn nữa.
        // Chỉ kiểm tra có bị tường/skull che tầm nhìn không.
        return hasLineOfSight(player, walls, skullVisionBlockers);
    }

    private boolean hasLineOfSight(
        Player player,
        Array<Rectangle> walls,
        Array<Rectangle> skullVisionBlockers
    ) {
        Vector2 bossPoint = getCenter(tmpCenter);
        Vector2 playerPoint = getPlayerCenter(player, tmpTarget);

        // Tường vẫn chặn tầm nhìn.
        for (Rectangle wall : walls) {
            if (wall.overlaps(this.hitbox) || wall.overlaps(player.hitbox)) {
                continue;
            }

            if (Intersector.intersectSegmentRectangle(bossPoint, playerPoint, wall)) {
                return false;
            }
        }

        /*
         * Skull_Collision giờ chỉ là vật cản tầm nhìn.
         *
         * Khác đoạn cũ:
         * - Không còn check player đang đứng trong skull là tàng hình.
         * - Không bỏ qua skull khi skull overlaps player.
         *
         * Như vậy nếu đường nhìn từ boss tới player bị cục xương cắt ngang,
         * boss sẽ không thấy player.
         */
        if (skullVisionBlockers != null) {
            for (Rectangle skull : skullVisionBlockers) {
                if (skull.overlaps(this.hitbox)) {
                    continue;
                }

                if (Intersector.intersectSegmentRectangle(bossPoint, playerPoint, skull)) {
                    return false;
                }
            }
        }

        return true;
    }

    private Vector2 getCenter(Vector2 out) {
        return out.set(
            hitbox.x + hitbox.width / 2f,
            hitbox.y + hitbox.height / 2f
        );
    }

    private Vector2 getPlayerCenter(Player player, Vector2 out) {
        return out.set(
            player.hitbox.x + player.hitbox.width / 2f,
            player.hitbox.y + player.hitbox.height / 2f
        );
    }

    private int findNearestWaypointIndex() {
        if (patrolPath == null || patrolPath.size == 0) {
            return 0;
        }

        Vector2 center = getCenter(tmpCenter);

        int nearest = 0;
        float bestDistance = Float.MAX_VALUE;

        for (int i = 0; i < patrolPath.size; i++) {
            float distance = center.dst(patrolPath.get(i));

            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = i;
            }
        }

        return nearest;
    }

    @Override
    public void draw(SpriteBatch batch) {
        TextureRegion frame = walkAnimation.getKeyFrame(stateTime, true);

        boolean shouldFlipX = isFacingRight != SPRITE_FACES_RIGHT_BY_DEFAULT;

        if (frame.isFlipX() != shouldFlipX) {
            frame.flip(true, false);
        }

        float drawX = x - (frame.getRegionWidth() - hitbox.width) / 2f;
        float drawY = y;

        batch.draw(frame, drawX, drawY);
    }

    public void drawDebug(ShapeRenderer shape) {
        Vector2 center = getCenter(tmpCenter);

        // Vẽ vòng tròn quan sát thay vì hình phễu.
        if (currentState == State.CHASE) {
            shape.setColor(Color.RED);
        } else if (currentState == State.SEARCH) {
            shape.setColor(Color.ORANGE);
        } else {
            shape.setColor(Color.PURPLE);
        }

        shape.circle(center.x, center.y, visionRadius);

        // Vẽ hitbox boss.
        shape.rect(hitbox.x, hitbox.y, hitbox.width, hitbox.height);

        // Vẽ điểm cuối cùng boss nhớ player.
        if (currentState == State.CHASE || currentState == State.SEARCH) {
            shape.setColor(Color.YELLOW);
            shape.circle(lastKnownPlayerPos.x, lastKnownPlayerPos.y, 4f);
        }
    }
}
