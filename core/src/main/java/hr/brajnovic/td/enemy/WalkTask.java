package hr.brajnovic.td.enemy;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;

/** Fallback branch of the boss Selector: no special ability to run this tick, so EnemyLifecycleSystem's
 * normal per-frame movement (unaffected by BossComponent.healing) is left to carry the boss forward. */
public class WalkTask extends LeafTask<Entity> {

    @Override
    public Status execute() {
        return Status.SUCCEEDED;
    }

    @Override
    protected Task<Entity> copyTo(Task<Entity> task) {
        return task;
    }
}
