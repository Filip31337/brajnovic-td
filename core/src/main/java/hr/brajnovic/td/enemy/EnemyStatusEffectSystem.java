package hr.brajnovic.td.enemy;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import hr.brajnovic.td.ecs.Mappers;

import java.util.Iterator;

/** Ticks down each enemy's active stackable effects (see ActiveEffect), dropping expired stacks and
 * recomputing the cached speedMultiplier that EnemyLifecycleSystem applies to movement. Runs before
 * EnemyLifecycleSystem (priority -1 vs 0) so a stack expiring this frame no longer affects this frame's movement. */
public class EnemyStatusEffectSystem extends IteratingSystem {

    public EnemyStatusEffectSystem() {
        super(Family.all(EnemyComponent.class).get(), -1);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyComponent enemy = Mappers.ENEMY.get(entity);

        if (enemy.hitFlashTimer > 0f) {
            enemy.hitFlashTimer = Math.max(0f, enemy.hitFlashTimer - deltaTime);
        }

        Iterator<ActiveEffect> iterator = enemy.activeEffects.iterator();
        while (iterator.hasNext()) {
            ActiveEffect effect = iterator.next();
            effect.remainingSeconds -= deltaTime;
            if (effect.remainingSeconds <= 0f) {
                iterator.remove();
            }
        }

        float speedMultiplier = 1f;
        float poisonDamagePerSecond = 0f;
        for (ActiveEffect effect : enemy.activeEffects) {
            switch (effect.type) {
                case SLOW -> speedMultiplier *= effect.magnitude;
                case POISON -> poisonDamagePerSecond += effect.magnitude;
            }
        }
        enemy.speedMultiplier = speedMultiplier;

        if (poisonDamagePerSecond > 0f && enemy.hp > 0f && !enemy.reachedGoal) {
            enemy.hp -= poisonDamagePerSecond * deltaTime;
        }
    }
}
