package hr.brajnovic.td.enemy;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import hr.brajnovic.td.GameConstants;
import hr.brajnovic.td.ecs.Mappers;
import hr.brajnovic.td.ecs.PositionComponent;
import hr.brajnovic.td.fx.ParticleEffectManager;
import hr.brajnovic.td.sound.SoundManager;

/** Stops the boss for EnemyDefinition.healDurationSeconds (see EnemyLifecycleSystem's BossComponent.healing
 * check), then restores healPercentOfMaxHp and starts the cooldown over. Runs every frame while RUNNING. */
public class HealAction extends LeafTask<Entity> {

    private final ParticleEffectManager particleEffectManager;

    public HealAction(ParticleEffectManager particleEffectManager) {
        this.particleEffectManager = particleEffectManager;
    }

    @Override
    public Status execute() {
        Entity entity = getObject();
        EnemyComponent enemy = Mappers.ENEMY.get(entity);
        BossComponent boss = Mappers.BOSS.get(entity);
        PositionComponent position = Mappers.POSITION.get(entity);

        if (!boss.healing) {
            boss.healing = true;
            boss.healActionTimer = 0f;
            particleEffectManager.spawn(enemy.definition.healParticleId,
                position.value.x * GameConstants.SCALED_TILE_SIZE_PX,
                position.value.y * GameConstants.SCALED_TILE_SIZE_PX);
            SoundManager.play(enemy.definition.healSoundId);
        }

        boss.healActionTimer += boss.frameDeltaSeconds;
        if (boss.healActionTimer < enemy.definition.healDurationSeconds) {
            return Status.RUNNING;
        }

        enemy.hp = Math.min(enemy.maxHp, enemy.hp + enemy.maxHp * enemy.definition.healPercentOfMaxHp);
        boss.healing = false;
        boss.healCooldownTimer = enemy.definition.healIntervalSeconds;
        return Status.SUCCEEDED;
    }

    @Override
    protected Task<Entity> copyTo(Task<Entity> task) {
        return task;
    }
}
