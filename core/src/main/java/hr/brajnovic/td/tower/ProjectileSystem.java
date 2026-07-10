package hr.brajnovic.td.tower;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import hr.brajnovic.td.ecs.Mappers;
import hr.brajnovic.td.ecs.PositionComponent;
import hr.brajnovic.td.enemy.ActiveEffect;
import hr.brajnovic.td.enemy.EffectType;
import hr.brajnovic.td.enemy.EnemyComponent;

public class ProjectileSystem extends IteratingSystem {

    private static final Family ENEMY_FAMILY = Family.all(EnemyComponent.class, PositionComponent.class).get();

    public ProjectileSystem() {
        super(Family.all(ProjectileComponent.class, PositionComponent.class).get(), 2);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ProjectileComponent projectile = Mappers.PROJECTILE.get(entity);
        PositionComponent position = Mappers.POSITION.get(entity);

        if (projectile.impactTimer >= 0f) {
            projectile.impactTimer += deltaTime;
            position.value.set(projectile.targetPosition);
            if (projectile.impactTimer >= projectile.impactAnimationDuration) {
                getEngine().removeEntity(entity);
            }
            return;
        }

        projectile.timeToImpact -= deltaTime;
        if (projectile.timeToImpact <= 0f) {
            if (projectile.aoeRadiusTiles > 0f) {
                applyAoeDamage(projectile);
            } else {
                applySingleTargetDamage(projectile);
            }
            projectile.impactTimer = 0f;
        }

        float t = 1f - Math.max(0f, projectile.timeToImpact) / projectile.totalTime;
        position.value.set(projectile.startPosition).lerp(projectile.targetPosition, t);
    }

    private void applySingleTargetDamage(ProjectileComponent projectile) {
        EnemyComponent targetEnemy = projectile.target == null ? null : Mappers.ENEMY.get(projectile.target);
        if (targetEnemy != null && targetEnemy.spawnId == projectile.targetSpawnId
            && targetEnemy.hp > 0f && !targetEnemy.reachedGoal) {
            applyHit(targetEnemy, projectile);
        }
    }

    /** Damages every living enemy within aoeRadiusTiles of the impact point, not just the locked-on target. */
    private void applyAoeDamage(ProjectileComponent projectile) {
        for (Entity candidate : getEngine().getEntitiesFor(ENEMY_FAMILY)) {
            EnemyComponent enemy = Mappers.ENEMY.get(candidate);
            if (enemy.hp <= 0f || enemy.reachedGoal) {
                continue;
            }
            Vector2 enemyPosition = Mappers.POSITION.get(candidate).value;
            if (enemyPosition.dst(projectile.targetPosition) <= projectile.aoeRadiusTiles) {
                applyHit(enemy, projectile);
            }
        }
    }

    private void applyHit(EnemyComponent enemy, ProjectileComponent projectile) {
        enemy.hp -= projectile.damage;
        if (projectile.slowRatio > 0f) {
            enemy.activeEffects.add(new ActiveEffect(EffectType.SLOW, 1f - projectile.slowRatio, projectile.slowDurationSeconds));
        }
        if (projectile.poisonDamagePerSecond > 0f) {
            enemy.activeEffects.add(new ActiveEffect(EffectType.POISON, projectile.poisonDamagePerSecond, projectile.poisonDurationSeconds));
        }
    }
}
