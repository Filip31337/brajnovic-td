package hr.brajnovic.td.tower;

import com.badlogic.gdx.math.Vector2;
import hr.brajnovic.td.enemy.Enemy;

/** A projectile in flight toward a predicted intercept point; applies damage on impact. */
public class Projectile {

    private final Vector2 startPosition;
    private final Vector2 targetPosition;
    private final Enemy target;
    private final float damage;
    private final float totalTime;
    private float timeToImpact;

    public Projectile(Vector2 startPosition, Vector2 targetPosition, Enemy target, float damage, float speedTilesPerSec) {
        this.startPosition = new Vector2(startPosition);
        this.targetPosition = new Vector2(targetPosition);
        this.target = target;
        this.damage = damage;
        this.totalTime = Math.max(0.01f, this.startPosition.dst(this.targetPosition) / speedTilesPerSec);
        this.timeToImpact = totalTime;
    }

    /** Returns false once the projectile has impacted (and should be removed). */
    public boolean update(float delta) {
        timeToImpact -= delta;
        if (timeToImpact <= 0f) {
            if (target.isAlive()) {
                target.applyDamage(damage);
            }
            return false;
        }
        return true;
    }

    public Vector2 getCurrentPosition(Vector2 out) {
        float t = 1f - Math.max(0f, timeToImpact) / totalTime;
        return out.set(startPosition).lerp(targetPosition, t);
    }
}
