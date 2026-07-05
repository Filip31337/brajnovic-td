package hr.brajnovic.td.tower;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import hr.brajnovic.td.enemy.Enemy;

/** A projectile in flight toward a predicted intercept point; applies damage on impact, then
 * lingers briefly at the impact point to let an impact animation play before being removed. */
public class Projectile {

    private final Vector2 startPosition;
    private final Vector2 targetPosition;
    private final Enemy target;
    private final float damage;
    private final float totalTime;
    private final float travelAngleDeg;
    private final float impactAnimationDuration;
    private final String spriteSheetId;
    private float timeToImpact;
    private float impactTimer = -1f;

    public Projectile(Vector2 startPosition, Vector2 targetPosition, Enemy target, float damage,
                       float speedTilesPerSec, float impactAnimationDuration, float spriteRotationOffsetDeg,
                       String spriteSheetId) {
        this.startPosition = new Vector2(startPosition);
        this.targetPosition = new Vector2(targetPosition);
        this.target = target;
        this.damage = damage;
        this.totalTime = Math.max(0.01f, this.startPosition.dst(this.targetPosition) / speedTilesPerSec);
        this.timeToImpact = totalTime;
        this.travelAngleDeg = MathUtils.atan2(
            this.targetPosition.y - this.startPosition.y,
            this.targetPosition.x - this.startPosition.x
        ) * MathUtils.radiansToDegrees + spriteRotationOffsetDeg;
        this.impactAnimationDuration = impactAnimationDuration;
        this.spriteSheetId = spriteSheetId;
    }

    /** Returns false once the impact animation has finished (and the projectile should be removed). */
    public boolean update(float delta) {
        if (impactTimer >= 0f) {
            impactTimer += delta;
            return impactTimer < impactAnimationDuration;
        }

        timeToImpact -= delta;
        if (timeToImpact <= 0f) {
            if (target.isAlive()) {
                target.applyDamage(damage);
            }
            impactTimer = 0f;
        }
        return true;
    }

    public Vector2 getCurrentPosition(Vector2 out) {
        float t = 1f - Math.max(0f, timeToImpact) / totalTime;
        return out.set(startPosition).lerp(targetPosition, t);
    }

    public boolean isImpacting() {
        return impactTimer >= 0f;
    }

    public float getImpactProgress() {
        return MathUtils.clamp(impactTimer / impactAnimationDuration, 0f, 1f);
    }

    public float getFlightElapsedSeconds() {
        return totalTime - Math.max(0f, timeToImpact);
    }

    public float getTravelAngleDeg() {
        return travelAngleDeg;
    }

    public String getSpriteSheetId() {
        return spriteSheetId;
    }
}
