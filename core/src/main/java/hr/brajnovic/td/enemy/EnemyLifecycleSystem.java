package hr.brajnovic.td.enemy;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import hr.brajnovic.td.GameConstants;
import hr.brajnovic.td.ecs.Mappers;
import hr.brajnovic.td.ecs.PositionComponent;
import hr.brajnovic.td.economy.Economy;
import hr.brajnovic.td.fx.ParticleEffectManager;

/** Moves enemies along their precomputed path and removes them on death or reaching the goal. */
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

        enemy.animationTime += deltaTime;

        GridPoint2 waypoint = enemy.path.get(enemy.waypointIndex);
        float targetX = waypoint.x + 0.5f;
        float targetY = waypoint.y + 0.5f;
        float dx = targetX - position.value.x;
        float dy = targetY - position.value.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float step = enemy.definition.speedTilesPerSec * enemy.speedMultiplier * deltaTime;

        enemy.facingDirection = directionFromDelta(dx, dy);

        if (step >= distance) {
            enemy.distanceTraveled += distance;
            position.value.set(targetX, targetY);
            enemy.waypointIndex++;
            if (enemy.waypointIndex >= enemy.path.size()) {
                enemy.reachedGoal = true;
            }
        } else {
            enemy.distanceTraveled += step;
            position.value.x += dx / distance * step;
            position.value.y += dy / distance * step;
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
