package hr.brajnovic.td.enemy;

/** A single stack of a time-limited effect (e.g. one ice tower hit's slow). Multiple stacks of the
 * same type coexist independently, each on its own expiry timer; see EnemyStatusEffectSystem. */
public class ActiveEffect {

    public final EffectType type;
    /** Effect-specific strength: for SLOW the speed multiplier this stack contributes (e.g. 0.9 = -10%);
     * for POISON the damage/sec this stack contributes. */
    public final float magnitude;
    public float remainingSeconds;

    public ActiveEffect(EffectType type, float magnitude, float remainingSeconds) {
        this.type = type;
        this.magnitude = magnitude;
        this.remainingSeconds = remainingSeconds;
    }
}
