package hr.brajnovic.td.enemy;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

/** A single active enemy walking a precomputed waypoint path (tile-unit coordinates). */
public class Enemy {

    /** 8-way facing order matching increasing atan2 angle (E=0deg, going counter-clockwise). */
    private static final String[] FACING_DIRECTIONS = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};

    private final EnemyDefinition definition;
    private final List<GridPoint2> path;
    private final Vector2 position = new Vector2();
    private final float maxHp;

    private int waypointIndex = 1;
    private float hp;
    private float distanceTraveled = 0f;
    private boolean reachedGoal = false;
    private String facingDirection = "S";
    private float animationTime = 0f;

    public Enemy(EnemyDefinition definition, List<GridPoint2> path, float hpMultiplier) {
        this.definition = definition;
        this.path = path;
        this.maxHp = definition.maxHp * hpMultiplier;
        this.hp = maxHp;

        GridPoint2 start = path.get(0);
        position.set(start.x + 0.5f, start.y + 0.5f);
        if (path.size() == 1) {
            reachedGoal = true;
        }
    }

    public void update(float delta) {
        if (reachedGoal) {
            return;
        }

        animationTime += delta;

        GridPoint2 waypoint = path.get(waypointIndex);
        float targetX = waypoint.x + 0.5f;
        float targetY = waypoint.y + 0.5f;
        float dx = targetX - position.x;
        float dy = targetY - position.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float step = definition.speedTilesPerSec * delta;

        facingDirection = directionFromDelta(dx, dy);

        if (step >= distance) {
            distanceTraveled += distance;
            position.set(targetX, targetY);
            waypointIndex++;
            if (waypointIndex >= path.size()) {
                reachedGoal = true;
            }
        } else {
            distanceTraveled += step;
            position.x += dx / distance * step;
            position.y += dy / distance * step;
        }
    }

    private static String directionFromDelta(float dx, float dy) {
        float angleDeg = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        if (angleDeg < 0f) {
            angleDeg += 360f;
        }
        int index = Math.round(angleDeg / 45f) % FACING_DIRECTIONS.length;
        return FACING_DIRECTIONS[index];
    }

    public String getFacingDirection() {
        return facingDirection;
    }

    public float getAnimationTime() {
        return animationTime;
    }

    public Vector2 getVelocity() {
        if (reachedGoal) {
            return new Vector2();
        }
        GridPoint2 waypoint = path.get(waypointIndex);
        Vector2 direction = new Vector2(waypoint.x + 0.5f - position.x, waypoint.y + 0.5f - position.y);
        if (direction.isZero(0.0001f)) {
            return new Vector2();
        }
        return direction.nor().scl(definition.speedTilesPerSec);
    }

    public void applyDamage(float amount) {
        hp -= amount;
    }

    public boolean isAlive() {
        return hp > 0f && !reachedGoal;
    }

    public boolean isDead() {
        return hp <= 0f;
    }

    public boolean hasReachedGoal() {
        return reachedGoal;
    }

    /** Distance traveled along the path so far; used by "first" (progress-based) tower targeting. */
    public float getProgress() {
        return distanceTraveled;
    }

    public float getHp() {
        return hp;
    }

    public float getMaxHp() {
        return maxHp;
    }

    public Vector2 getPosition() {
        return position;
    }

    public EnemyDefinition getDefinition() {
        return definition;
    }
}
