package hr.brajnovic.td.tower;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import hr.brajnovic.td.enemy.Enemy;

import java.util.List;

/** A placed tower: tracks a "first" (progress-based) target within range and fires leading-shot projectiles. */
public class Tower {

    private final TowerDefinition definition;
    private final int gridX;
    private final int gridY;
    private final Vector2 position;

    private float turretAngleDeg = 90f;
    private float fireCooldown = 0f;
    private Enemy target;

    public Tower(TowerDefinition definition, int gridX, int gridY) {
        this.definition = definition;
        this.gridX = gridX;
        this.gridY = gridY;
        this.position = new Vector2(gridX + 0.5f, gridY + 0.5f);
    }

    public void update(float delta, List<Enemy> candidates, List<Projectile> projectilesOut) {
        fireCooldown = Math.max(0f, fireCooldown - delta);
        retarget(candidates);

        if (target != null) {
            float angleToTarget = angleToDeg(target.getPosition());
            turretAngleDeg = rotateTowards(turretAngleDeg, angleToTarget, definition.turretTrackingDegPerSec * delta);

            if (fireCooldown <= 0f && angleDifferenceDeg(turretAngleDeg, angleToTarget) <= definition.fireAngleThresholdDeg) {
                fire(projectilesOut);
            }
        } else {
            float idleAngularSpeed = 360f / definition.turretIdleRevolutionSeconds;
            turretAngleDeg = (turretAngleDeg + idleAngularSpeed * delta) % 360f;
        }
    }

    private void retarget(List<Enemy> candidates) {
        if (target != null && (!target.isAlive() || position.dst(target.getPosition()) > definition.rangeTiles)) {
            target = null;
        }
        if (target == null) {
            Enemy best = null;
            float bestProgress = -1f;
            for (Enemy candidate : candidates) {
                if (!candidate.isAlive() || position.dst(candidate.getPosition()) > definition.rangeTiles) {
                    continue;
                }
                if (candidate.getProgress() > bestProgress) {
                    bestProgress = candidate.getProgress();
                    best = candidate;
                }
            }
            target = best;
        }
    }

    private void fire(List<Projectile> projectilesOut) {
        fireCooldown = 1f / definition.fireRatePerSecond;
        Vector2 predictedImpact = predictImpactPosition(target, definition.projectileSpeedTilesPerSec);
        projectilesOut.add(new Projectile(position, predictedImpact, target, definition.damage, definition.projectileSpeedTilesPerSec));
    }

    private Vector2 predictImpactPosition(Enemy enemy, float projectileSpeed) {
        Vector2 relative = enemy.getPosition().cpy().sub(position);
        Vector2 velocity = enemy.getVelocity();

        float a = velocity.dot(velocity) - projectileSpeed * projectileSpeed;
        float b = 2f * relative.dot(velocity);
        float c = relative.dot(relative);

        float t;
        if (Math.abs(a) < 1e-4f) {
            t = Math.abs(b) < 1e-4f ? 0f : -c / b;
        } else {
            float discriminant = b * b - 4f * a * c;
            if (discriminant < 0f) {
                t = relative.len() / projectileSpeed;
            } else {
                float sqrtDiscriminant = (float) Math.sqrt(discriminant);
                float t1 = (-b + sqrtDiscriminant) / (2f * a);
                float t2 = (-b - sqrtDiscriminant) / (2f * a);
                t = smallestPositive(t1, t2, relative.len() / projectileSpeed);
            }
        }
        t = Math.max(0f, t);

        return enemy.getPosition().cpy().add(velocity.cpy().scl(t));
    }

    private static float smallestPositive(float t1, float t2, float fallback) {
        boolean t1Valid = t1 > 0f;
        boolean t2Valid = t2 > 0f;
        if (t1Valid && t2Valid) {
            return Math.min(t1, t2);
        }
        if (t1Valid) {
            return t1;
        }
        if (t2Valid) {
            return t2;
        }
        return fallback;
    }

    private float angleToDeg(Vector2 point) {
        return MathUtils.atan2(point.y - position.y, point.x - position.x) * MathUtils.radiansToDegrees;
    }

    private static float angleDifferenceDeg(float a, float b) {
        float diff = (b - a) % 360f;
        if (diff < -180f) {
            diff += 360f;
        }
        if (diff > 180f) {
            diff -= 360f;
        }
        return Math.abs(diff);
    }

    private static float rotateTowards(float current, float desired, float maxDelta) {
        float diff = (desired - current) % 360f;
        if (diff < -180f) {
            diff += 360f;
        }
        if (diff > 180f) {
            diff -= 360f;
        }
        float clamped = MathUtils.clamp(diff, -maxDelta, maxDelta);
        return (current + clamped + 360f) % 360f;
    }

    public TowerDefinition getDefinition() {
        return definition;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public Vector2 getPosition() {
        return position;
    }

    public float getTurretAngleDeg() {
        return turretAngleDeg;
    }
}
