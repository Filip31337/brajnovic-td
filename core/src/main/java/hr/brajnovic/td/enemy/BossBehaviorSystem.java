package hr.brajnovic.td.enemy;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import hr.brajnovic.td.ecs.Mappers;

/** Steps each boss's behavior tree once per frame. Runs before EnemyStatusEffectSystem/EnemyLifecycleSystem
 * (priority -2) so a heal decision made this frame (e.g. BossComponent.healing just turned true) is already
 * in effect when EnemyLifecycleSystem decides whether to move the boss this same frame. */
public class BossBehaviorSystem extends IteratingSystem {

    public BossBehaviorSystem() {
        super(Family.all(EnemyComponent.class, BossComponent.class).get(), -2);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        BossComponent boss = Mappers.BOSS.get(entity);
        if (boss.healCooldownTimer > 0f) {
            boss.healCooldownTimer -= deltaTime;
        }
        boss.frameDeltaSeconds = deltaTime;
        boss.behaviorTree.step();
    }
}
