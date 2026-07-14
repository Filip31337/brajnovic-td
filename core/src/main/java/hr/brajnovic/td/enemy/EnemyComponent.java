package hr.brajnovic.td.enemy;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Pool.Poolable;

import java.util.ArrayList;
import java.util.List;

public class EnemyComponent implements Component, Poolable {

    /** Identifies this spawn across PooledEngine component-reuse cycles; see TowerTargetingSystem/ProjectileSystem. */
    public int spawnId;
    public EnemyDefinition definition;
    public List<GridPoint2> path;
    public float maxHp;
    public float hp;
    public float distanceTraveled = 0f;
    public boolean reachedGoal = false;
    public String facingDirection = "S";
    public float animationTime = 0f;
    public StateMachine<Entity, EnemyState> stateMachine;
    public float deathTimer = 0f;
    /** Counts down from GameConstants.ENEMY_HIT_FLASH_DURATION_SECONDS on a direct hit; see EnemyStatusEffectSystem/GameScreen. */
    public float hitFlashTimer = 0f;
    /** Currently active stackable effects (e.g. ice tower slow stacks); see EnemyStatusEffectSystem. */
    public final List<ActiveEffect> activeEffects = new ArrayList<>();
    /** Product of all active effect magnitudes affecting movement speed; recomputed by EnemyStatusEffectSystem. */
    public float speedMultiplier = 1f;

    @Override
    public void reset() {
        spawnId = 0;
        definition = null;
        path = null;
        maxHp = 0f;
        hp = 0f;
        distanceTraveled = 0f;
        reachedGoal = false;
        facingDirection = "S";
        animationTime = 0f;
        stateMachine = null;
        deathTimer = 0f;
        hitFlashTimer = 0f;
        activeEffects.clear();
        speedMultiplier = 1f;
    }
}
