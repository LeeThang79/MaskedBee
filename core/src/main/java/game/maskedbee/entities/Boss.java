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
        ALERT,
        CHASE,
        SEARCH
    }

    public State currentState = State.PATROL;

    public float patrolSpeed = 40f;
    public float chaseSpeed = 70f;

    public float visionRadius = 130f;
    public float viewAngle = 60f;

    private float rotation = 0f;
    private float alertTimer = 0f;
    private float lostSightTimer = 0f;
    private float searchTimer = 0f;

    private static final float ALERT_TIME = 0.35f;
    private static final float LOSE_SIGHT_TIME = 1.2f;
    private static final float SEARCH_TIME = 1.6f;

    private final Vector2 lastKnownPlayerPos = new Vector2();
    private final Vector2 tmpCenter = new Vector2();
    private final Vector2 tmpTarget = new Vector2();

    private Array<Vector2> patrolPath;
    private int targetWaypointIndex = 0;

    private Animation<TextureRegion> walkAnimation;
    private boolean isFacingRight = true;

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
     */
    public boolean update(
        float deltaTime,
        Player player,
        Array<Rectangle> walls,
        Array<Rectangle> skullHideZones
    ) {
        stateTime += deltaTime;

        boolean playerHiddenInSkull = isPlayerHiddenInSkull(player, skullHideZones);
        boolean canSeePlayer = !playerHiddenInSkull && canSeePlayer(player, walls, skullHideZones);

        Vector2 bossCenter = getCenter(tmpCenter);
        Vector2 playerCenter = getPlayerCenter(player, tmpTarget);

        if (canSeePlayer) {
            lastKnownPlayerPos.set(playerCenter);
            lostSightTimer = 0f;
            searchTimer = 0f;

            if (currentState == State.PATROL || currentState == State.SEARCH) {
                currentState = State.ALERT;
                alertTimer = 0f;
            }
        }

        if (currentState == State.ALERT) {
            faceTo(playerCenter);
            alertTimer += deltaTime;

            if (alertTimer >= ALERT_TIME) {
                currentState = State.CHASE;
            }
        } else if (currentState == State.CHASE) {
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

        // Boss bắt Player: mặt nạ bee không có tác dụng.
        // Nhưng nếu Player đang trong Skull_Collision thì coi như đang núp, không bị bắt.
        if (!playerHiddenInSkull && this.hitbox.overlaps(player.hitbox)) {
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

    private boolean canSeePlayer(Player player, Array<Rectangle> walls, Array<Rectangle> skullHideZones) {
        Vector2 bossCenter = getCenter(tmpCenter);
        Vector2 playerCenter = getPlayerCenter(player, tmpTarget);

        float dist = bossCenter.dst(playerCenter);
        if (dist > visionRadius) {
            return false;
        }

        float angleToPlayer = MathUtils.atan2(
            playerCenter.y - bossCenter.y,
            playerCenter.x - bossCenter.x
        ) * MathUtils.radiansToDegrees;

        float angleDiff = Math.abs(angleToPlayer - rotation);
        if (angleDiff > 180f) {
            angleDiff = 360f - angleDiff;
        }

        if (angleDiff > viewAngle / 2f) {
            return false;
        }

        return hasLineOfSight(player, walls, skullHideZones);
    }

    private boolean hasLineOfSight(Player player, Array<Rectangle> walls, Array<Rectangle> skullHideZones) {
        Vector2 bossPoint = getCenter(tmpCenter);
        Vector2 playerPoint = getPlayerCenter(player, tmpTarget);

        for (Rectangle wall : walls) {
            if (wall.overlaps(this.hitbox) || wall.overlaps(player.hitbox)) {
                continue;
            }

            if (Intersector.intersectSegmentRectangle(bossPoint, playerPoint, wall)) {
                return false;
            }
        }

        // Skull cũng có thể chặn tầm nhìn.
        for (Rectangle skull : skullHideZones) {
            if (skull.overlaps(this.hitbox) || skull.overlaps(player.hitbox)) {
                continue;
            }

            if (Intersector.intersectSegmentRectangle(bossPoint, playerPoint, skull)) {
                return false;
            }
        }

        return true;
    }

    private boolean isPlayerHiddenInSkull(Player player, Array<Rectangle> skullHideZones) {
        if (skullHideZones == null) return false;

        for (Rectangle skull : skullHideZones) {
            if (player.hitbox.overlaps(skull)) {
                return true;
            }
        }

        return false;
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

    private void faceTo(Vector2 target) {
        Vector2 center = getCenter(tmpCenter);

        rotation = MathUtils.atan2(
            target.y - center.y,
            target.x - center.x
        ) * MathUtils.radiansToDegrees;

        if (target.x < center.x) {
            isFacingRight = false;
        } else if (target.x > center.x) {
            isFacingRight = true;
        }
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

        if (!isFacingRight && !frame.isFlipX()) {
            frame.flip(true, false);
        } else if (isFacingRight && frame.isFlipX()) {
            frame.flip(true, false);
        }

        float drawX = x - (frame.getRegionWidth() - hitbox.width) / 2f;
        float drawY = y;

        batch.draw(frame, drawX, drawY);
    }

    public void drawDebug(ShapeRenderer shape) {
        shape.setColor(currentState == State.CHASE ? Color.RED : Color.PURPLE);

        Vector2 center = getCenter(tmpCenter);

        float x1 = center.x + MathUtils.cosDeg(rotation - viewAngle / 2f) * visionRadius;
        float y1 = center.y + MathUtils.sinDeg(rotation - viewAngle / 2f) * visionRadius;

        float x2 = center.x + MathUtils.cosDeg(rotation + viewAngle / 2f) * visionRadius;
        float y2 = center.y + MathUtils.sinDeg(rotation + viewAngle / 2f) * visionRadius;

        shape.line(center.x, center.y, x1, y1);
        shape.line(center.x, center.y, x2, y2);
        shape.line(x1, y1, x2, y2);

        shape.rect(hitbox.x, hitbox.y, hitbox.width, hitbox.height);
    }
}
