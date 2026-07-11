package hr.brajnovic.td.enemy;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import hr.brajnovic.td.ecs.Mappers;
import hr.brajnovic.td.sound.SoundManager;

/** Per-frame movement/removal logic stays in EnemyLifecycleSystem (it owns the scaled deltaTime);
 * this FSM only tracks the current state and its enter/exit side effects. */
public enum EnemyState implements State<Entity> {

    WALKING,
    DYING {
        @Override
        public void enter(Entity entity) {
            EnemyComponent enemy = Mappers.ENEMY.get(entity);
            enemy.animationTime = 0f;
            enemy.deathTimer = 0f;
            SoundManager.play(enemy.definition.deathSoundId);
        }
    };

    @Override
    public void enter(Entity entity) {
    }

    @Override
    public void update(Entity entity) {
    }

    @Override
    public void exit(Entity entity) {
    }

    @Override
    public boolean onMessage(Entity entity, Telegram telegram) {
        return false;
    }
}
