package hr.brajnovic.td.enemy;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Pool.Poolable;

import java.util.List;

public class EnemyComponent implements Component, Poolable {

    /** Identifies this spawn across PooledEngine component-reuse cycles; see TowerTargetingSystem/ProjectileSystem. */
    public int spawnId;
    public EnemyDefinition definition;
    public List<GridPoint2> path;
    public int waypointIndex = 1;
    public float maxHp;
    public float hp;
    public float distanceTraveled = 0f;
    public boolean reachedGoal = false;
    public String facingDirection = "S";
    public float animationTime = 0f;

    @Override
    public void reset() {
        spawnId = 0;
        definition = null;
        path = null;
        waypointIndex = 1;
        maxHp = 0f;
        hp = 0f;
        distanceTraveled = 0f;
        reachedGoal = false;
        facingDirection = "S";
        animationTime = 0f;
    }
}
