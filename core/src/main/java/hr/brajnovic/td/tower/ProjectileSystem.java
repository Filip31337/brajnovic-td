package hr.brajnovic.td.tower;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import hr.brajnovic.td.ecs.Mappers;
import hr.brajnovic.td.ecs.PositionComponent;
import hr.brajnovic.td.enemy.EnemyComponent;

public class ProjectileSystem extends IteratingSystem {

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
            EnemyComponent targetEnemy = projectile.target == null ? null : Mappers.ENEMY.get(projectile.target);
            if (targetEnemy != null && targetEnemy.spawnId == projectile.targetSpawnId
                && targetEnemy.hp > 0f && !targetEnemy.reachedGoal) {
                targetEnemy.hp -= projectile.damage;
            }
            projectile.impactTimer = 0f;
        }

        float t = 1f - Math.max(0f, projectile.timeToImpact) / projectile.totalTime;
        position.value.set(projectile.startPosition).lerp(projectile.targetPosition, t);
    }
}
