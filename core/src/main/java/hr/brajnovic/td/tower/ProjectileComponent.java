package hr.brajnovic.td.tower;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool.Poolable;

/** A projectile in flight toward a predicted intercept point; applies damage on impact, then
 * lingers briefly at the impact point to let an impact animation play before being removed. */
public class ProjectileComponent implements Component, Poolable {

    public final Vector2 startPosition = new Vector2();
    public final Vector2 targetPosition = new Vector2();
    public Entity target;
    /** See {@link TowerComponent#targetSpawnId}. */
    public int targetSpawnId;
    public float damage;
    public float totalTime;
    public float timeToImpact;
    public float travelAngleDeg;
    public float impactAnimationDuration;
    public float impactTimer = -1f;
    public String spriteSheetId;

    public void init(Vector2 startPosition, Vector2 targetPosition, Entity target, int targetSpawnId, float damage,
                      float speedTilesPerSec, float impactAnimationDuration, float spriteRotationOffsetDeg,
                      String spriteSheetId) {
        this.startPosition.set(startPosition);
        this.targetPosition.set(targetPosition);
        this.target = target;
        this.targetSpawnId = targetSpawnId;
        this.damage = damage;
        this.totalTime = Math.max(0.01f, this.startPosition.dst(this.targetPosition) / speedTilesPerSec);
        this.timeToImpact = totalTime;
        this.travelAngleDeg = MathUtils.atan2(
            this.targetPosition.y - this.startPosition.y,
            this.targetPosition.x - this.startPosition.x
        ) * MathUtils.radiansToDegrees + spriteRotationOffsetDeg;
        this.impactAnimationDuration = impactAnimationDuration;
        this.impactTimer = -1f;
        this.spriteSheetId = spriteSheetId;
    }

    @Override
    public void reset() {
        startPosition.setZero();
        targetPosition.setZero();
        target = null;
        targetSpawnId = 0;
        damage = 0f;
        totalTime = 0f;
        timeToImpact = 0f;
        travelAngleDeg = 0f;
        impactAnimationDuration = 0f;
        impactTimer = -1f;
        spriteSheetId = null;
    }
}
