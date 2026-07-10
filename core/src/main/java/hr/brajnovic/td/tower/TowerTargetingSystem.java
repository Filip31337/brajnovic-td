package hr.brajnovic.td.tower;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import hr.brajnovic.td.ecs.Mappers;
import hr.brajnovic.td.ecs.PositionComponent;
import hr.brajnovic.td.enemy.EnemyComponent;

/** "First" (progress-based) targeting: tracks the furthest-progressed enemy in range and fires leading-shot projectiles. */
public class TowerTargetingSystem extends IteratingSystem {

    private static final Family ENEMY_FAMILY = Family.all(EnemyComponent.class, PositionComponent.class).get();

    public TowerTargetingSystem() {
        super(Family.all(TowerComponent.class, PositionComponent.class).get(), 1);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TowerComponent tower = Mappers.TOWER.get(entity);
        Vector2 position = Mappers.POSITION.get(entity).value;

        tower.fireCooldown = Math.max(0f, tower.fireCooldown - deltaTime);
        tower.timeSinceLastShot += deltaTime;
        retarget(tower, position);

        if (tower.target != null) {
            Vector2 targetPosition = Mappers.POSITION.get(tower.target).value;
            float angleToTarget = angleToDeg(position, targetPosition);
            tower.turretAngleDeg = rotateTowards(tower.turretAngleDeg, angleToTarget, tower.definition.turretTrackingDegPerSec * deltaTime);

            if (tower.fireCooldown <= 0f && angleDifferenceDeg(tower.turretAngleDeg, angleToTarget) <= tower.definition.fireAngleThresholdDeg) {
                fire(tower, position);
            }
        } else {
            float idleAngularSpeed = 360f / tower.definition.turretIdleRevolutionSeconds;
            tower.turretAngleDeg = (tower.turretAngleDeg + idleAngularSpeed * deltaTime) % 360f;
        }
    }

    private void retarget(TowerComponent tower, Vector2 position) {
        float rangeTiles = TowerUpgrade.rangeForLevel(tower.definition, tower.level);
        if (tower.target != null && !isValidTarget(tower.target, tower.targetSpawnId, position, rangeTiles)) {
            tower.target = null;
        }
        if (tower.target == null) {
            Entity best = null;
            int bestSpawnId = 0;
            float bestProgress = -1f;
            for (Entity candidate : getEngine().getEntitiesFor(ENEMY_FAMILY)) {
                EnemyComponent candidateEnemy = Mappers.ENEMY.get(candidate);
                if (candidateEnemy.hp <= 0f || candidateEnemy.reachedGoal) {
                    continue;
                }
                Vector2 candidatePosition = Mappers.POSITION.get(candidate).value;
                if (position.dst(candidatePosition) > rangeTiles) {
                    continue;
                }
                if (candidateEnemy.distanceTraveled > bestProgress) {
                    bestProgress = candidateEnemy.distanceTraveled;
                    best = candidate;
                    bestSpawnId = candidateEnemy.spawnId;
                }
            }
            tower.target = best;
            tower.targetSpawnId = bestSpawnId;
        }
    }

    private boolean isValidTarget(Entity target, int expectedSpawnId, Vector2 towerPosition, float rangeTiles) {
        EnemyComponent enemy = Mappers.ENEMY.get(target);
        if (enemy == null || enemy.spawnId != expectedSpawnId || enemy.hp <= 0f || enemy.reachedGoal) {
            return false;
        }
        Vector2 enemyPosition = Mappers.POSITION.get(target).value;
        return towerPosition.dst(enemyPosition) <= rangeTiles;
    }

    private void fire(TowerComponent tower, Vector2 position) {
        EnemyComponent targetEnemy = Mappers.ENEMY.get(tower.target);
        Vector2 targetPosition = Mappers.POSITION.get(tower.target).value;

        tower.fireCooldown = 1f / TowerUpgrade.fireRateForLevel(tower.definition, tower.level);
        tower.timeSinceLastShot = 0f;

        Vector2 targetVelocity = enemyVelocity(targetEnemy, targetPosition);
        Vector2 predictedImpact = predictImpactPosition(position, targetPosition, targetVelocity, tower.definition.projectileSpeedTilesPerSec);

        PositionComponent projectilePosition = getEngine().createComponent(PositionComponent.class);
        projectilePosition.value.set(position);
        ProjectileComponent projectile = getEngine().createComponent(ProjectileComponent.class);
        float damage = TowerUpgrade.damageForLevel(tower.definition, tower.level);
        projectile.init(position, predictedImpact, tower.target, tower.targetSpawnId, damage,
            tower.definition.projectileSpeedTilesPerSec, tower.definition.projectileImpactDurationSeconds,
            tower.definition.projectileSpriteRotationOffsetDeg, tower.definition.spriteSheetId);

        Entity projectileEntity = getEngine().createEntity();
        projectileEntity.add(projectilePosition);
        projectileEntity.add(projectile);
        getEngine().addEntity(projectileEntity);
    }

    private static Vector2 enemyVelocity(EnemyComponent enemy, Vector2 position) {
        if (enemy.reachedGoal) {
            return new Vector2();
        }
        GridPoint2 waypoint = enemy.path.get(enemy.waypointIndex);
        Vector2 direction = new Vector2(waypoint.x + 0.5f - position.x, waypoint.y + 0.5f - position.y);
        if (direction.isZero(0.0001f)) {
            return new Vector2();
        }
        return direction.nor().scl(enemy.definition.speedTilesPerSec);
    }

    private static Vector2 predictImpactPosition(Vector2 towerPosition, Vector2 enemyPosition, Vector2 enemyVelocity, float projectileSpeed) {
        Vector2 relative = enemyPosition.cpy().sub(towerPosition);

        float a = enemyVelocity.dot(enemyVelocity) - projectileSpeed * projectileSpeed;
        float b = 2f * relative.dot(enemyVelocity);
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

        return enemyPosition.cpy().add(enemyVelocity.cpy().scl(t));
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

    private static float angleToDeg(Vector2 from, Vector2 to) {
        return MathUtils.atan2(to.y - from.y, to.x - from.x) * MathUtils.radiansToDegrees;
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
}
