package hr.brajnovic.td.tower;

import box2dLight.PointLight;
import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool.Poolable;

/** A projectile in flight toward a predicted intercept point; applies damage (and any on-hit effects)
 * on impact, then lingers briefly at the impact point to let an impact animation play before being removed. */
public class ProjectileComponent implements Component, Poolable {

    public final Vector2 startPosition = new Vector2();
    public final Vector2 targetPosition = new Vector2();
    public Entity target;
    /** See {@link TowerComponent#targetSpawnId}. */
    public int targetSpawnId;
    public float damage;
    /** Splash radius in tiles around the impact point; 0 = only {@link #target} takes damage. */
    public float aoeRadiusTiles;
    /** Fraction (0..1) applied as a fresh slow stack to each hit enemy; 0 = no slow effect. */
    public float slowRatio;
    /** How long (seconds) the applied slow stack lasts; only meaningful when slowRatio > 0. */
    public float slowDurationSeconds;
    /** Damage/sec applied as a fresh poison stack to each hit enemy; 0 = no poison effect. */
    public float poisonDamagePerSecond;
    /** How long (seconds) the applied poison stack lasts; only meaningful when poisonDamagePerSecond > 0. */
    public float poisonDurationSeconds;
    public float totalTime;
    public float timeToImpact;
    public float travelAngleDeg;
    public float impactAnimationDuration;
    public float impactTimer = -1f;
    public String spriteSheetId;
    public String impactSoundId;
    public String impactParticleId;
    public String projectileLightId;
    /** Handle to the LightEffectManager-pooled light travelling with this projectile while it's alive; null if projectileLightId is null. */
    public PointLight projectileLight;

    /** damage/slowRatio/poisonDamagePerSecond are level-scaled by the caller (see TowerUpgrade); everything
     * else here is shot-independent and copied straight off the firing tower's definition, so this signature
     * doesn't grow with every new on-hit effect type. */
    public void init(Vector2 startPosition, Vector2 targetPosition, Entity target, int targetSpawnId,
                      float damage, float slowRatio, float poisonDamagePerSecond, TowerDefinition definition) {
        this.startPosition.set(startPosition);
        this.targetPosition.set(targetPosition);
        this.target = target;
        this.targetSpawnId = targetSpawnId;
        this.damage = damage;
        this.aoeRadiusTiles = definition.aoeRadiusTiles;
        this.slowRatio = slowRatio;
        this.slowDurationSeconds = definition.slowDurationSeconds;
        this.poisonDamagePerSecond = poisonDamagePerSecond;
        this.poisonDurationSeconds = definition.poisonDurationSeconds;
        this.totalTime = Math.max(0.01f,
            this.startPosition.dst(this.targetPosition) / definition.projectileSpeedTilesPerSec);
        this.timeToImpact = totalTime;
        this.travelAngleDeg = MathUtils.atan2(
            this.targetPosition.y - this.startPosition.y,
            this.targetPosition.x - this.startPosition.x
        ) * MathUtils.radiansToDegrees + definition.projectileSpriteRotationOffsetDeg;
        this.impactAnimationDuration = definition.projectileImpactDurationSeconds;
        this.impactTimer = -1f;
        this.spriteSheetId = definition.spriteSheetId;
        this.impactSoundId = definition.impactSoundId;
        this.impactParticleId = definition.impactParticleId;
        this.projectileLightId = definition.projectileLightId;
    }

    @Override
    public void reset() {
        startPosition.setZero();
        targetPosition.setZero();
        target = null;
        targetSpawnId = 0;
        damage = 0f;
        aoeRadiusTiles = 0f;
        slowRatio = 0f;
        slowDurationSeconds = 0f;
        poisonDamagePerSecond = 0f;
        poisonDurationSeconds = 0f;
        totalTime = 0f;
        timeToImpact = 0f;
        travelAngleDeg = 0f;
        impactAnimationDuration = 0f;
        impactTimer = -1f;
        spriteSheetId = null;
        impactSoundId = null;
        impactParticleId = null;
        projectileLightId = null;
        projectileLight = null;
    }
}
