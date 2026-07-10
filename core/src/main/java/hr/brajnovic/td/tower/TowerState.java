package hr.brajnovic.td.tower;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;

/** Purely a named reflection of {@code timeSinceLastShot}/{@code target} - TowerTargetingSystem still owns
 * the per-frame timing and syncs the current state each tick. Ready for V2 enter/exit hooks (e.g. muzzle flash). */
public enum TowerState implements State<Entity> {

    IDLE,
    TRACKING,
    FIRING;

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
