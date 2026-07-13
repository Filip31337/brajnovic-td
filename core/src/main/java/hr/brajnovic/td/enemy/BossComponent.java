package hr.brajnovic.td.enemy;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.utils.Pool.Poolable;

/** Marks a boss-wave enemy and holds its behavior tree + heal-ability timers. Only attached to boss
 * spawns (see WaveController.spawnEnemy); regular enemies never get this component. */
public class BossComponent implements Component, Poolable {

    public BehaviorTree<Entity> behaviorTree;
    /** Counts down to 0; heal is only available once this reaches 0 (see HealReadyCondition). */
    public float healCooldownTimer;
    /** True while the heal action is running (boss stands still, see EnemyLifecycleSystem). */
    public boolean healing;
    /** Counts up from 0 while healing, until it reaches EnemyDefinition.healDurationSeconds (see HealAction). */
    public float healActionTimer;
    /** Set by BossBehaviorSystem right before stepping the tree, read by HealAction - avoids needing GdxAI's global timepiece. */
    public float frameDeltaSeconds;

    @Override
    public void reset() {
        behaviorTree = null;
        healCooldownTimer = 0f;
        healing = false;
        healActionTimer = 0f;
        frameDeltaSeconds = 0f;
    }
}
