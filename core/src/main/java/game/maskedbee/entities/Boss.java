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

    // Boss quan sát vòng tròn 360 độ.
    public float visionRadius = 180f;

    private float rotation = 0f;
    private float lostSightTimer = 0f;
    private float searchTimer = 0f;
    private int searchPhase = 0;

    // Boss đuổi lâu quá không bắt được thì bỏ cuộc.
    private float chaseTimer = 0f;
    private float ignorePlayerTimer = 0f;

    // Chống kẹt khi tuần tra.
    private float patrolStuckTimer = 0f;
    private float searchStuckTimer = 0f;

    private static final float LOSE_SIGHT_TIME = 1.2f;
    private static final float SEARCH_TIME = 1.6f;

    // Boss đuổi tối đa 4 giây. Muốn lâu hơn thì tăng lên 5f hoặc 6f.
    private static final float MAX_CHASE_TIME = 4f;

    // Sau khi bỏ cuộc, tạm bỏ qua player 1.2 giây để không đuổi lại ngay.
    private static final float GIVE_UP_IGNORE_TIME = 1.2f;

    // Nếu tuần tra mà bị kẹt quá thời gian này thì bỏ qua waypoint hiện tại.
    private static final float PATROL_STUCK_TIME = 0.45f;

    // Nếu SEARCH mà bị kẹt quá thời gian này thì quay lại PATROL.
    private static final float SEARCH_STUCK_TIME = 0.8f;

    private final Vector2 lastKnownPlayerPos = new Vector2();
    private final Vector2 tmpCenter = new Vector2();
    private final Vector2 tmpTarget = new Vector2();
    private final Vector2 tmpSearchTarget = new Vector2();

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
     * return true nếu Boss bắt được Player.
     *
     * skullVisionBlockers:
     * - Không còn là vùng tàng hình.
     * - Chỉ dùng để chặn tầm nhìn của Boss giống như vật cản.
     */
    public boolean update(
        float deltaTime,
        Player player,
        Array<Rectangle> walls,
        Array<Rectangle> skullVisionBlockers
    ) {
        stateTime += deltaTime;

        if (ignorePlayerTimer > 0f) {
            ignorePlayerTimer -= deltaTime;
        }

        boolean rawCanSeePlayer = canSeePlayer(player, walls, skullVisionBlockers);
        boolean canSeePlayer = rawCanSeePlayer && ignorePlayerTimer <= 0f;

        Vector2 bossCenter = getCenter(tmpCenter);
        Vector2 playerCenter = getPlayerCenter(player, tmpTarget);

        // Nhìn thấy player thì đuổi.
        // Nhưng nếu vừa bỏ cuộc thì ignorePlayerTimer sẽ chặn không cho đuổi lại ngay.
        if (canSeePlayer) {
            lastKnownPlayerPos.set(playerCenter);
            lostSightTimer = 0f;
            searchTimer = 0f;
            searchPhase = 0;
            searchStuckTimer = 0f;

            if (currentState != State.CHASE) {
                chaseTimer = 0f;
            }

            currentState = State.CHASE;
        }

        if (currentState == State.CHASE) {
            chaseTimer += deltaTime;

            if (canSeePlayer) {
                lastKnownPlayerPos.set(playerCenter);
                moveToward(lastKnownPlayerPos, chaseSpeed, deltaTime, walls);
            } else {
                lostSightTimer += deltaTime;

                if (lostSightTimer >= LOSE_SIGHT_TIME) {
                    currentState = State.SEARCH;
                    searchTimer = 0f;
                    searchPhase = 0;
                    searchStuckTimer = 0f;
                    chaseTimer = 0f;
                } else {
                    moveToward(lastKnownPlayerPos, chaseSpeed * 0.85f, deltaTime, walls);
                }
            }

            // Đuổi quá lâu mà chưa bắt được thì bỏ cuộc.
            if (chaseTimer >= MAX_CHASE_TIME) {
                giveUpAndReturnToPatrol();
            }

        } else if (currentState == State.SEARCH) {
            float distanceToLastKnown = bossCenter.dst(lastKnownPlayerPos);

            if (searchPhase == 0) {
                // Phase 0: đi tới vị trí cuối cùng nhìn thấy player.
                if (distanceToLastKnown > 8f) {
                    boolean moved = moveToward(lastKnownPlayerPos, patrolSpeed, deltaTime, walls);

                    if (!moved) {
                        searchStuckTimer += deltaTime;

                        if (searchStuckTimer >= SEARCH_STUCK_TIME) {
                            giveUpAndReturnToPatrol();
                        }
                    } else {
                        searchStuckTimer = 0f;
                    }
                } else {
                    searchPhase = 1;
                    searchTimer = 0f;
                    searchStuckTimer = 0f;
                }
            } else {
                // Phase 1: lượn vòng nhỏ quanh điểm đó rồi bỏ đi.
                searchTimer += deltaTime;
                rotation += 130f * deltaTime;

                float searchAngle = searchTimer * 90f * MathUtils.degreesToRadians;

                tmpSearchTarget.set(
                    lastKnownPlayerPos.x + MathUtils.cos(searchAngle) * 24f,
                    lastKnownPlayerPos.y + MathUtils.sin(searchAngle) * 24f
                );

                boolean moved = moveToward(tmpSearchTarget, patrolSpeed * 0.6f, deltaTime, walls);

                if (!moved) {
                    searchStuckTimer += deltaTime;
                } else {
                    searchStuckTimer = 0f;
                }

                if (searchTimer >= SEARCH_TIME || searchStuckTimer >= SEARCH_STUCK_TIME) {
                    currentState = State.PATROL;
                    searchPhase = 0;
                    searchTimer = 0f;
                    searchStuckTimer = 0f;
                    chaseTimer = 0f;
                    targetWaypointIndex = findNearestWaypointIndex();
                }
            }
        } else {
            chaseTimer = 0f;
            patrol(deltaTime, walls);
        }

        // Boss chạm Player là bắt luôn.
        // Skull_Collision không còn làm player miễn bắt nữa.
        if (this.hitbox.overlaps(player.hitbox)) {
            return true;
        }

        return false;
    }

    private void giveUpAndReturnToPatrol() {
        currentState = State.PATROL;

        lostSightTimer = 0f;
        searchTimer = 0f;
        chaseTimer = 0f;
        searchPhase = 0;
        searchStuckTimer = 0f;
        patrolStuckTimer = 0f;

        // Tạm bỏ qua player một chút, tránh vừa quay lại tuần tra đã đuổi tiếp ngay.
        ignorePlayerTimer = GIVE_UP_IGNORE_TIME;

        targetWaypointIndex = findNearestWaypointIndex();

        System.out.println("Boss gave up chasing and returned to patrol.");
    }

    private void patrol(float deltaTime, Array<Rectangle> walls) {
        if (patrolPath == null || patrolPath.size == 0) {
            rotation += 40f * deltaTime;
            return;
        }

        Vector2 target = patrolPath.get(targetWaypointIndex);

        float oldX = x;
        float oldY = y;

        moveToward(target, patrolSpeed, deltaTime, walls);

        Vector2 center = getCenter(tmpCenter);

        boolean reachedTarget = center.dst(target) < 8f;
        boolean barelyMoved = Math.abs(x - oldX) < 0.1f && Math.abs(y - oldY) < 0.1f;

        if (reachedTarget) {
            patrolStuckTimer = 0f;
            targetWaypointIndex = (targetWaypointIndex + 1) % patrolPath.size;
            return;
        }

        if (barelyMoved) {
            patrolStuckTimer += deltaTime;

            if (patrolStuckTimer >= PATROL_STUCK_TIME) {
                patrolStuckTimer = 0f;

                // Nếu bị kẹt do waypoint nằm sau xương/tường thì bỏ qua waypoint đó.
                targetWaypointIndex = (targetWaypointIndex + 1) % patrolPath.size;

                System.out.println("Boss patrol stuck. Skip waypoint: " + targetWaypointIndex);
            }
        } else {
            patrolStuckTimer = 0f;
        }
    }

    /**
     * return true nếu Boss thật sự di chuyển được.
     */
    private boolean moveToward(Vector2 target, float moveSpeed, float deltaTime, Array<Rectangle> walls) {
        Vector2 center = getCenter(tmpCenter);

        float dx = target.x - center.x;
        float dy = target.y - center.y;

        if (Math.abs(dx) < 1f && Math.abs(dy) < 1f) {
            return true;
        }

        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0f) return true;

        dx /= len;
        dy /= len;

        rotation = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;

        if (Math.abs(dx) > 0.05f) {
            isFacingRight = dx >= 0f;
        }

        float oldX = x;
        float oldY = y;

        moveWithCollision(dx * moveSpeed * deltaTime, dy * moveSpeed * deltaTime, walls);

        return Math.abs(x - oldX) > 0.05f || Math.abs(y - oldY) > 0.05f;
    }

    private boolean canSeePlayer(
        Player player,
        Array<Rectangle> walls,
        Array<Rectangle> skullVisionBlockers
    ) {
        Vector2 bossCenter = getCenter(tmpCenter);
        Vector2 playerCenter = getPlayerCenter(player, tmpTarget);

        // Player rón rén thì tầm nhìn Boss giảm còn 70%.
        float currentVisionRadius = player.isCreeping ? visionRadius * 0.7f : visionRadius;

        float dist = bossCenter.dst(playerCenter);

        if (dist > currentVisionRadius) {
            return false;
        }

        // Boss nhìn 360 độ nên không kiểm tra góc nhìn.
        // Chỉ cần trong bán kính và không bị tường/skull chắn là thấy.
        return hasLineOfSight(player, walls, skullVisionBlockers);
    }

    private boolean hasLineOfSight(
        Player player,
        Array<Rectangle> walls,
        Array<Rectangle> skullVisionBlockers
    ) {
        Vector2 bossPoint = getCenter(tmpCenter);
        Vector2 playerPoint = getPlayerCenter(player, tmpTarget);

        // Tường chặn tầm nhìn.
        if (walls != null) {
            for (Rectangle wall : walls) {
                if (wall.overlaps(this.hitbox) || wall.overlaps(player.hitbox)) {
                    continue;
                }

                if (Intersector.intersectSegmentRectangle(bossPoint, playerPoint, wall)) {
                    return false;
                }
            }
        }

        // Skull_Collision chặn tầm nhìn như vật cản.
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

        // Trả về waypoint sau waypoint gần nhất để Boss tiếp tục tuần tra,
        // tránh đứng quay về chính waypoint vừa ở gần.
        return (nearest + 1) % patrolPath.size;
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

    public void drawDebug(ShapeRenderer shape, Player player) {
        Vector2 center = getCenter(tmpCenter);

        float currentVisionRadius = player.isCreeping ? visionRadius * 0.7f : visionRadius;

        // Tím = tuần tra / tìm kiếm
        // Đỏ = đang đuổi player
        if (currentState == State.CHASE) {
            shape.setColor(Color.RED);
        } else {
            shape.setColor(Color.PURPLE);
        }

        shape.circle(center.x, center.y, currentVisionRadius);
        shape.rect(hitbox.x, hitbox.y, hitbox.width, hitbox.height);
    }
}
