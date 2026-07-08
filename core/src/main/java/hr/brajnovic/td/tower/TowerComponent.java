package hr.brajnovic.td.tower;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Pool.Poolable;

public class TowerComponent implements Component, Poolable {

    public TowerDefinition definition;
    public int gridX;
    public int gridY;
    public float turretAngleDeg = 90f;
    public float fireCooldown = 0f;
    public float timeSinceLastShot = Float.MAX_VALUE;
    public Entity target;
    /** EnemyComponent.spawnId captured when {@code target} was acquired; guards against PooledEngine
     * reusing the same Entity/Component objects for an unrelated later enemy before this tower re-targets. */
    public int targetSpawnId;

    @Override
    public void reset() {
        definition = null;
        gridX = 0;
        gridY = 0;
        turretAngleDeg = 90f;
        fireCooldown = 0f;
        timeSinceLastShot = Float.MAX_VALUE;
        target = null;
        targetSpawnId = 0;
    }
}
