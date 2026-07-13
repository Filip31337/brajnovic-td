package hr.brajnovic.td.enemy;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import hr.brajnovic.td.ecs.Mappers;

/** Succeeds when the boss's heal is off cooldown and it isn't already at full HP. */
public class HealReadyCondition extends LeafTask<Entity> {

    @Override
    public Status execute() {
        EnemyComponent enemy = Mappers.ENEMY.get(getObject());
        BossComponent boss = Mappers.BOSS.get(getObject());
        boolean ready = boss.healCooldownTimer <= 0f && enemy.hp < enemy.maxHp;
        return ready ? Status.SUCCEEDED : Status.FAILED;
    }

    @Override
    protected Task<Entity> copyTo(Task<Entity> task) {
        return task;
    }
}
