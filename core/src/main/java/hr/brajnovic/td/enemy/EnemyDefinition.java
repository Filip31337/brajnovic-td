package hr.brajnovic.td.enemy;

import com.badlogic.gdx.utils.ObjectMap;

/** Data-driven enemy definition, loaded from assets/data/enemies.json. */
public class EnemyDefinition {

    public String id;
    public String name;
    public float maxHp;
    public float speedTilesPerSec;
    public int goldReward;
    public int livesDamage;
    public float deathAnimationDurationSeconds = 0.5f;

    /** Base name of the sprites-src sheet (PNG + JSON) under assets/sprites-src/, e.g. "orc_atlas". */
    public String spriteSheetId;
    /** Sound ID (matches a filename under assets/sounds/, no extension) played by {@link hr.brajnovic.td.sound.SoundManager} on death. */
    public String deathSoundId;
    /** Particle effect ID (matches an id built by {@link hr.brajnovic.td.fx.ParticleEffectManager}) spawned on death. */
    public String deathParticleId;

    /** Prepared for V2 elemental towers: damageType -> multiplier. Empty while damage is flat. */
    public ObjectMap<String, Float> resistances;
}
