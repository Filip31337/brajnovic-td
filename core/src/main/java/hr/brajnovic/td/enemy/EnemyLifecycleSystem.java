package hr.brajnovic.td.enemy;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import hr.brajnovic.td.GameConstants;
import hr.brajnovic.td.ecs.Mappers;
import hr.brajnovic.td.ecs.PositionComponent;
import hr.brajnovic.td.economy.Economy;
import hr.brajnovic.td.fx.ParticleEffectManager;

import java.util.List;

/** Moves enemies along their precomputed path (gdx-ai Steering: FollowPath blended with Separation) and
 * removes them on death or reaching the goal. */
public class EnemyLifecycleSystem extends IteratingSystem {

    /** 8-way facing order matching increasing atan2 angle (E=0deg, going counter-clockwise). */
    private static final String[] FACING_DIRECTIONS = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};

    private final Economy economy;
    private final ParticleEffectManager particleEffectManager;

    public EnemyLifecycleSystem(Economy economy, ParticleEffectManager particleEffectManager) {
        super(Family.all(EnemyComponent.class, PositionComponent.class).get(), 0);
        this.economy = economy;
        this.particleEffectManager = particleEffectManager;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyComponent enemy = Mappers.ENEMY.get(entity);
        PositionComponent position = Mappers.POSITION.get(entity);

        if (enemy.stateMachine.getCurrentState() == EnemyState.DYING) {
            enemy.deathTimer += deltaTime;
            if (enemy.deathTimer >= enemy.definition.deathAnimationDurationSeconds) {
                getEngine().removeEntity(entity);
            }
            return;
        }
        if (enemy.hp <= 0f) {
            economy.addGold(enemy.definition.goldReward);
            particleEffectManager.spawn(enemy.definition.deathParticleId,
                position.value.x * GameConstants.SCALED_TILE_SIZE_PX,
                position.value.y * GameConstants.SCALED_TILE_SIZE_PX);
            enemy.stateMachine.changeState(EnemyState.DYING);
            return;
        }
        if (enemy.reachedGoal) {
            economy.loseLives(enemy.definition.livesDamage);
            getEngine().removeEntity(entity);
            return;
        }

        BossComponent boss = Mappers.BOSS.get(entity);
        if (boss != null && boss.healing) {
            return;
        }

        enemy.animationTime += deltaTime;

        SteeringComponent steering = Mappers.STEERING.get(entity);
        steering.maxLinearSpeed = enemy.definition.speedTilesPerSec * enemy.speedMultiplier;
        steering.blended.calculateSteering(steering.steeringOutput);
        applySteering(steering, deltaTime);
        clampLateralDeviation(enemy, position, steering);

        enemy.distanceTraveled = steering.followPath.getPathParam().getDistance();
        if (!steering.linearVelocity.isZero(0.0001f)) {
            enemy.facingDirection = directionFromDelta(steering.linearVelocity.x, steering.linearVelocity.y);
        }
        if (position.value.dst(steering.linePath.getEndPoint()) <= GameConstants.ENEMY_GOAL_ARRIVAL_TOLERANCE_TILES) {
            enemy.reachedGoal = true;
        }
    }

    /** Standard gdx-ai integration: accelerate velocity toward the steering output, clamp to max speed, integrate position. */
    private static void applySteering(SteeringComponent steering, float deltaTime) {
        SteeringAcceleration<Vector2> acceleration = steering.steeringOutput;
        if (!acceleration.linear.isZero()) {
            steering.linearVelocity.mulAdd(acceleration.linear, deltaTime).limit(steering.maxLinearSpeed);
        }
        steering.position.value.mulAdd(steering.linearVelocity, deltaTime);
    }

    /** Hard safety cap: Separation alone has no notion of corridor width, so without this an enemy could
     * visually drift through a blocked/wall tile in a 1-tile-wide maze corridor when several are crowded together. */
    private static void clampLateralDeviation(EnemyComponent enemy, PositionComponent position, SteeringComponent steering) {
        List<GridPoint2> path = enemy.path;
        int segmentIndex = MathUtils.clamp(steering.followPath.getPathParam().getSegmentIndex(), 0, path.size() - 2);
        GridPoint2 a = path.get(segmentIndex);
        GridPoint2 b = path.get(segmentIndex + 1);
        float ax = a.x + 0.5f, ay = a.y + 0.5f;
        float bx = b.x + 0.5f, by = b.y + 0.5f;
        float segDx = bx - ax, segDy = by - ay;
        float segLenSq = segDx * segDx + segDy * segDy;
        float t = segLenSq > 0.0001f
            ? MathUtils.clamp(((position.value.x - ax) * segDx + (position.value.y - ay) * segDy) / segLenSq, 0f, 1f)
            : 0f;
        float nearestX = ax + segDx * t;
        float nearestY = ay + segDy * t;
        float devX = position.value.x - nearestX;
        float devY = position.value.y - nearestY;
        float deviation = (float) Math.sqrt(devX * devX + devY * devY);
        if (deviation > GameConstants.ENEMY_MAX_PATH_DEVIATION_TILES) {
            float scale = GameConstants.ENEMY_MAX_PATH_DEVIATION_TILES / deviation;
            position.value.set(nearestX + devX * scale, nearestY + devY * scale);
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
}
