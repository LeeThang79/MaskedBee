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

public class Guard extends Entity {
    public float patrolSpeed = 36f;
    public float investigateSpeed = 48f;
    public float chaseSpeed = 60f;

    public enum State {
        PATROL,
        ALERT,
        CHASE,
        SEARCH,
        RETURN
    }

    public State currentState = State.PATROL;

    public float alertLevel = 0f;
    public float visionRadius = 110f;
    public float viewAngle = 65f;

    private float rotation = 0f;

    private Array<Vector2> patrolPath;
    private int targetWaypointIndex = 0;

    private Vector2 lastKnownPos;
    private Vector2 spawnPos;

    private Animation<TextureRegion> walkAnimation;
    private boolean isFacingRight = true;

    private final Vector2 tmpCenter = new Vector2();
    private final Vector2 tmpTarget = new Vector2();
    private final Vector2 tmpDir = new Vector2();
    private final Vector2 tmpMove = new Vector2();

    private float loseSightTimer = 0f;
    private float searchTimer = 0f;
    private float stuckTimer = 0f;
    private float waypointPauseTimer = 0f;

    private int avoidSide = 1;

    private static final float WAYPOINT_REACH_DISTANCE = 10f;
    private static final float TARGET_REACH_DISTANCE = 8f;
    private static final float SEARCH_DURATION = 2.2f;
    private static final float LOSE_SIGHT_TO_SEARCH_TIME = 0.45f;

    public Guard(float startX, float startY, Array<Vector2> path) {
        super(startX, startY, 16, 20, 60f);

        this.patrolPath = path;
        this.spawnPos = new Vector2(startX, startY);
        this.lastKnownPos = new Vector2(startX, startY);

        Array<TextureRegion> frames = new Array<>();
        frames.add(new TextureRegion(new Texture("guard/g_walk_1.png")));
        frames.add(new TextureRegion(new Texture("guard/g_walk_2.png")));
        frames.add(new TextureRegion(new Texture("guard/g_walk_3.png")));
        frames.add(new TextureRegion(new Texture("guard/g_walk_4.png")));

        walkAnimation = new Animation<TextureRegion>(0.15f, frames);

        if (patrolPath != null && patrolPath.size > 0) {
            targetWaypointIndex = 0;
            faceTo(patrolPath.get(0));
        }
    }

    public boolean update(float deltaTime, Player player, Array<Rectangle> walls) {
        stateTime += deltaTime;

        Vector2 guardCenter = getCenter(tmpCenter);
        Vector2 playerCenter = getPlayerCenter(player, tmpTarget);

        float distToPlayer = guardCenter.dst(playerCenter);

        boolean seeingPlayer = !player.isBeeDisguised && canSeePlayer(player, walls);
        boolean hearingPlayer = !player.isBeeDisguised && canHearPlayer(player, distToPlayer);

        updateAlertAndState(deltaTime, player, seeingPlayer, hearingPlayer, playerCenter);
        updateMovement(deltaTime, player, walls, seeingPlayer);

        checkCatchPlayer(player);
        return this.hitbox.overlaps(player.hitbox);
    }

    private void updateAlertAndState(
        float deltaTime,
        Player player,
        boolean seeingPlayer,
        boolean hearingPlayer,
        Vector2 playerCenter
    ) {
        if (seeingPlayer) {
            lastKnownPos.set(playerCenter);
            loseSightTimer = 0f;
            searchTimer = 0f;

            float distance = getCenter(tmpCenter).dst(playerCenter);
            float alertGain;

            if (distance < 70f) {
                alertGain = 260f;
            } else if (distance < 120f) {
                alertGain = 170f;
            } else {
                alertGain = 110f;
            }

            if (player.isCreeping) {
                alertGain *= 0.45f;
            }

            alertLevel += alertGain * deltaTime;

            if (alertLevel >= 100f) {
                currentState = State.CHASE;
            } else if (currentState == State.PATROL || currentState == State.RETURN) {
                currentState = State.ALERT;
            }
        }
        else if (hearingPlayer) {
            lastKnownPos.set(playerCenter);

            if (currentState == State.PATROL || currentState == State.RETURN) {
                currentState = State.ALERT;
            }

            alertLevel += 45f * deltaTime;

            // Nghe tiếng động chỉ làm guard nghi ngờ, không tự động full chase
            if (alertLevel > 75f && currentState != State.CHASE) {
                alertLevel = 75f;
            }
        }
        else {
            if (currentState == State.CHASE) {
                loseSightTimer += deltaTime;

                if (loseSightTimer >= LOSE_SIGHT_TO_SEARCH_TIME) {
                    currentState = State.SEARCH;
                    searchTimer = 0f;
                }
            }
            else if (currentState == State.ALERT) {
                alertLevel -= 35f * deltaTime;

                if (alertLevel <= 0f) {
                    alertLevel = 0f;
                    currentState = State.RETURN;
                    targetWaypointIndex = findNearestWaypointIndex();
                }
            }
            else if (currentState == State.SEARCH) {
                alertLevel -= 22f * deltaTime;
            }
            else if (currentState == State.RETURN || currentState == State.PATROL) {
                alertLevel -= 45f * deltaTime;
            }
        }

        alertLevel = MathUtils.clamp(alertLevel, 0f, 100f);
    }

    private void updateMovement(float deltaTime, Player player, Array<Rectangle> walls, boolean seeingPlayer) {
        Vector2 center = getCenter(tmpCenter);

        if (waypointPauseTimer > 0f) {
            waypointPauseTimer -= deltaTime;
            return;
        }

        switch (currentState) {
            case PATROL:
                updatePatrol(deltaTime, walls);
                break;

            case ALERT:
                // Đứng lại nhìn về hướng nghi ngờ
                faceTo(lastKnownPos);

                // Nếu nghi ngờ đủ cao thì đi kiểm tra
                if (alertLevel > 45f) {
                    currentState = State.SEARCH;
                    searchTimer = 0f;
                }
                break;

            case CHASE:
                if (seeingPlayer) {
                    lastKnownPos.set(getPlayerCenter(player, tmpTarget));
                }

                moveToward(lastKnownPos, chaseSpeed, deltaTime, walls);
                break;

            case SEARCH:
                float distanceToLastKnown = center.dst(lastKnownPos);

                if (distanceToLastKnown > TARGET_REACH_DISTANCE) {
                    moveToward(lastKnownPos, investigateSpeed, deltaTime, walls);
                } else {
                    searchTimer += deltaTime;
                    rotation = snapAngleTo4Directions(rotation + 110f * deltaTime);

                    if (searchTimer >= SEARCH_DURATION) {
                        currentState = State.RETURN;
                        targetWaypointIndex = findNearestWaypointIndex();
                        loseSightTimer = 0f;
                        searchTimer = 0f;
                    }
                }
                break;

            case RETURN:
                if (patrolPath == null || patrolPath.size == 0) {
                    moveToward(spawnPos, patrolSpeed, deltaTime, walls);
                    if (center.dst(spawnPos) <= WAYPOINT_REACH_DISTANCE) {
                        currentState = State.PATROL;
                    }
                } else {
                    Vector2 target = patrolPath.get(targetWaypointIndex);

                    moveToward(target, patrolSpeed, deltaTime, walls);

                    if (center.dst(target) <= WAYPOINT_REACH_DISTANCE) {
                        currentState = State.PATROL;
                        waypointPauseTimer = 0.2f;
                    }
                }
                break;
        }
    }

    private void updatePatrol(float deltaTime, Array<Rectangle> walls) {
        if (patrolPath == null || patrolPath.size == 0) return;

        Vector2 center = getCenter(tmpCenter);
        Vector2 target = patrolPath.get(targetWaypointIndex);

        if (center.dst(target) <= WAYPOINT_REACH_DISTANCE) {
            targetWaypointIndex = (targetWaypointIndex + 1) % patrolPath.size;
            target = patrolPath.get(targetWaypointIndex);

            faceTo(target);
            waypointPauseTimer = 0.25f;
            stuckTimer = 0f;
            return;
        }

        moveToward(target, patrolSpeed, deltaTime, walls);
    }

    private void moveToward(Vector2 target, float moveSpeed, float deltaTime, Array<Rectangle> walls) {
        Vector2 center = getCenter(tmpCenter);

        tmpDir.set(target).sub(center);

        if (tmpDir.len2() < 1f) {
            return;
        }

        tmpDir.nor();

        boolean moved = tryMove(tmpDir, moveSpeed, deltaTime, walls);

        if (!moved) {
            // Nếu đi thẳng bị kẹt, thử đi ngang/dọc trước
            boolean tryHorizontalFirst = Math.abs(tmpDir.x) > Math.abs(tmpDir.y);

            if (tryHorizontalFirst) {
                tmpMove.set(Math.signum(tmpDir.x), 0f);
                moved = tryMove(tmpMove, moveSpeed, deltaTime, walls);

                if (!moved) {
                    tmpMove.set(0f, Math.signum(tmpDir.y));
                    moved = tryMove(tmpMove, moveSpeed, deltaTime, walls);
                }
            } else {
                tmpMove.set(0f, Math.signum(tmpDir.y));
                moved = tryMove(tmpMove, moveSpeed, deltaTime, walls);

                if (!moved) {
                    tmpMove.set(Math.signum(tmpDir.x), 0f);
                    moved = tryMove(tmpMove, moveSpeed, deltaTime, walls);
                }
            }
        }

        if (!moved) {
            // Né vật cản kiểu đơn giản: trượt sang cạnh bên
            tmpMove.set(-tmpDir.y * avoidSide, tmpDir.x * avoidSide);
            moved = tryMove(tmpMove, moveSpeed * 0.8f, deltaTime, walls);

            if (!moved) {
                tmpMove.set(tmpDir.y * avoidSide, -tmpDir.x * avoidSide);
                moved = tryMove(tmpMove, moveSpeed * 0.8f, deltaTime, walls);
            }
        }

        if (moved) {
            stuckTimer = 0f;
        } else {
            stuckTimer += deltaTime;

            if (stuckTimer > 0.55f) {
                handleStuck();
                stuckTimer = 0f;
                avoidSide *= -1;
            }
        }
    }

    private boolean tryMove(Vector2 dir, float moveSpeed, float deltaTime, Array<Rectangle> walls) {
        if (dir.isZero()) return false;

        Vector2 normalized = tmpMove.set(dir);

        if (normalized.len2() > 1f) {
            normalized.nor();
        }

        float oldX = x;
        float oldY = y;

        boolean moved = moveWithCollision(
            normalized.x * moveSpeed * deltaTime,
            normalized.y * moveSpeed * deltaTime,
            walls
        );

        float realMoveX = x - oldX;
        float realMoveY = y - oldY;

        if (Math.abs(realMoveX) > 0.001f || Math.abs(realMoveY) > 0.001f) {
            float rawAngle = MathUtils.atan2(realMoveY, realMoveX) * MathUtils.radiansToDegrees;
            rotation = snapAngleTo4Directions(rawAngle);

            isFacingRight = realMoveX >= 0f;
        }

        return moved;
    }

    private void handleStuck() {
        if (currentState == State.PATROL && patrolPath != null && patrolPath.size > 0) {
            targetWaypointIndex = (targetWaypointIndex + 1) % patrolPath.size;
        }
        else if (currentState == State.RETURN && patrolPath != null && patrolPath.size > 0) {
            targetWaypointIndex = findNearestWaypointIndex();
        }
        else if (currentState == State.SEARCH) {
            currentState = State.RETURN;
            targetWaypointIndex = findNearestWaypointIndex();
        }
        else if (currentState == State.CHASE) {
            currentState = State.SEARCH;
            searchTimer = 0f;
        }
    }

    private boolean canSeePlayer(Player player, Array<Rectangle> walls) {
        Vector2 guardCenter = getCenter(tmpCenter);
        Vector2 playerCenter = getPlayerCenter(player, tmpTarget);

        float distance = guardCenter.dst(playerCenter);
        float currentVisionRadius = player.isCreeping ? visionRadius * 0.45f : visionRadius;

        if (distance > currentVisionRadius) {
            return false;
        }

        float angleToPlayer = MathUtils.atan2(
            playerCenter.y - guardCenter.y,
            playerCenter.x - guardCenter.x
        ) * MathUtils.radiansToDegrees;

        float angleDiff = Math.abs(angleToPlayer - rotation);
        if (angleDiff > 180f) {
            angleDiff = 360f - angleDiff;
        }

        // Khi chưa chase thì phải nằm trong nón nhìn
        if (currentState != State.CHASE && angleDiff > viewAngle / 2f) {
            return false;
        }

        return hasLineOfSight(player, walls);
    }

    private boolean canHearPlayer(Player player, float distToPlayer) {
        return player.noiseRadius > 0f && distToPlayer <= player.noiseRadius;
    }

    private boolean hasLineOfSight(Player player, Array<Rectangle> blockers) {
        Vector2 guardEye = new Vector2(
            hitbox.x + hitbox.width / 2f,
            hitbox.y + hitbox.height * 0.65f
        );

        Vector2 playerBody = new Vector2(
            player.hitbox.x + player.hitbox.width / 2f,
            player.hitbox.y + player.hitbox.height * 0.5f
        );

        if (blockers == null) return true;

        for (Rectangle blocker : blockers) {
            if (blocker.overlaps(this.hitbox) || blocker.overlaps(player.hitbox)) {
                continue;
            }

            if (Intersector.intersectSegmentRectangle(guardEye, playerBody, blocker)) {
                return false;
            }
        }

        return true;
    }

    private int findNearestWaypointIndex() {
        if (patrolPath == null || patrolPath.size == 0) return 0;

        Vector2 center = getCenter(tmpCenter);

        int nearest = 0;
        float minDst = Float.MAX_VALUE;

        for (int i = 0; i < patrolPath.size; i++) {
            float d = center.dst(patrolPath.get(i));

            if (d < minDst) {
                minDst = d;
                nearest = i;
            }
        }

        return nearest;
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

        float rawAngle = MathUtils.atan2(
            target.y - center.y,
            target.x - center.x
        ) * MathUtils.radiansToDegrees;

        rotation = snapAngleTo4Directions(rawAngle);
    }

    private void checkCatchPlayer(Player player) {
        if (player.isBeeDisguised) return;
        if (!this.hitbox.overlaps(player.hitbox)) {
            return;
        }

        // Tạm thời reset player về góc an toàn
        player.x = 50f;
        player.y = 50f;
        player.hitbox.setPosition(player.x, player.y);

        if (patrolPath != null && patrolPath.size > 0) {
            Vector2 spawnPoint = patrolPath.get(0);
            this.x = spawnPoint.x;
            this.y = spawnPoint.y;
            this.hitbox.setPosition(this.x, this.y);

            targetWaypointIndex = patrolPath.size > 1 ? 1 : 0;
        } else {
            this.x = spawnPos.x;
            this.y = spawnPos.y;
            this.hitbox.setPosition(this.x, this.y);
        }

        currentState = State.PATROL;
        alertLevel = 0f;
        loseSightTimer = 0f;
        searchTimer = 0f;
        stuckTimer = 0f;

        lastKnownPos.set(getCenter(tmpCenter));
    }
    private float snapAngleTo4Directions(float angle) {
        angle = (angle + 360f) % 360f;

        if (angle >= 315f || angle < 45f) return 0f;      // RIGHT
        if (angle >= 45f && angle < 135f) return 90f;     // UP
        if (angle >= 135f && angle < 225f) return 180f;   // LEFT
        return 270f;                                      // DOWN
    }

    @Override
    public void draw(SpriteBatch batch) {
        TextureRegion currentFrame = walkAnimation.getKeyFrame(stateTime, true);

        if (!isFacingRight && !currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        } else if (isFacingRight && currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        }

        batch.draw(currentFrame, x, y);
    }

    public void drawDebug(ShapeRenderer shape, Player player) {
        Vector2 center = getCenter(tmpCenter);

        // Vòng tiếng ồn của player
        if (player.noiseRadius > 0f) {
            shape.setColor(Color.CYAN);
            shape.circle(
                player.hitbox.x + player.hitbox.width / 2f,
                player.hitbox.y + player.hitbox.height / 2f,
                player.noiseRadius
            );
        }

        float currentVisionRadius = player.isCreeping ? visionRadius * 0.45f : visionRadius;

        if (currentState == State.CHASE) {
            shape.setColor(Color.RED);
        } else if (currentState == State.SEARCH || currentState == State.ALERT) {
            shape.setColor(Color.ORANGE);
        } else {
            shape.setColor(Color.YELLOW);
        }

        float x1 = center.x + MathUtils.cosDeg(rotation - viewAngle / 2f) * currentVisionRadius;
        float y1 = center.y + MathUtils.sinDeg(rotation - viewAngle / 2f) * currentVisionRadius;

        float x2 = center.x + MathUtils.cosDeg(rotation + viewAngle / 2f) * currentVisionRadius;
        float y2 = center.y + MathUtils.sinDeg(rotation + viewAngle / 2f) * currentVisionRadius;

        shape.line(center.x, center.y, x1, y1);
        shape.line(center.x, center.y, x2, y2);
        shape.line(x1, y1, x2, y2);

        // Vẽ điểm cuối cùng guard biết vị trí player
        if (currentState == State.CHASE || currentState == State.SEARCH || currentState == State.ALERT) {
            shape.setColor(Color.PINK);
            shape.circle(lastKnownPos.x, lastKnownPos.y, 5f);
            shape.line(center.x, center.y, lastKnownPos.x, lastKnownPos.y);
        }

        // Thanh cảnh báo
        if (alertLevel > 0f) {
            shape.end();
            shape.begin(ShapeRenderer.ShapeType.Filled);

            shape.setColor(Color.BLACK);
            shape.rect(x, y + 34f, 32f, 6f);

            if (alertLevel >= 100f) {
                shape.setColor(Color.RED);
            } else {
                shape.setColor(Color.YELLOW);
            }

            shape.rect(x + 1f, y + 35f, 30f * alertLevel / 100f, 4f);

            shape.end();
            shape.begin(ShapeRenderer.ShapeType.Line);
        }
    }
}
