package hr.brajnovic.td.enemy;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.branch.Selector;
import com.badlogic.gdx.ai.btree.branch.Sequence;
import hr.brajnovic.td.fx.ParticleEffectManager;

/** Builds the boss behavior tree in code rather than authoring a .tree DSL file - same tradeoff as
 * ParticleEffectManager: a hand-written .tree script would be another opaque, hard-to-review text format
 * for a tree this small, while gdx-ai's Task API is fully public and just as readable inline. */
public final class BossBehaviorTreeFactory {

    public static BehaviorTree<Entity> build(Entity entity, ParticleEffectManager particleEffectManager) {
        Sequence<Entity> healSequence = new Sequence<>(new HealReadyCondition(), new HealAction(particleEffectManager));
        Selector<Entity> root = new Selector<>(healSequence, new WalkTask());
        return new BehaviorTree<>(root, entity);
    }

    private BossBehaviorTreeFactory() {
    }
}
