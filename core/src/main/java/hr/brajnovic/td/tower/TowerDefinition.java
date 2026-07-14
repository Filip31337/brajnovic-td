package hr.brajnovic.td.tower;

/** Data-driven tower definition, loaded from assets/data/towers.json. */
public class TowerDefinition {

    public String id;
    /** Localization key (e.g. "tower.arrow_tower.name") resolved via {@link hr.brajnovic.td.i18n.Localization}. */
    public String name;
    public int cost;
    public float rangeTiles;
    public float damage;
    public float fireRatePerSecond;
    public float projectileSpeedTilesPerSec;
    /** Splash radius in tiles applied around the impact point; 0 = single-target hit only (e.g. arrow_tower). */
    public float aoeRadiusTiles = 0f;
    /** Fraction (0..1) each hit slows a target by, as a fresh stacking speed-multiplier stack; 0 = no slow (e.g. arrow/cannon). */
    public float slowRatio = 0f;
    /** How long (seconds) one slow stack lasts before expiring; only meaningful when slowRatio > 0. */
    public float slowDurationSeconds = 0f;
    /** Damage/sec applied by each hit as a fresh stacking poison stack; 0 = no poison (e.g. arrow/cannon/ice). */
    public float poisonDamagePerSecond = 0f;
    /** How long (seconds) one poison stack lasts before expiring; only meaningful when poisonDamagePerSecond > 0. */
    public float poisonDurationSeconds = 0f;
    public float turretIdleRevolutionSeconds;
    public float turretTrackingDegPerSec;
    public float fireAngleThresholdDeg;

    /** Prepared for V2 elemental towers; unused while damage is flat. */
    public String damageType;
    /** Sound ID (matches a filename under assets/sounds/, no extension) played by {@link hr.brajnovic.td.sound.SoundManager} on fire. */
    public String shootSoundId;
    /** Sound ID played when this tower's projectile lands a hit. */
    public String impactSoundId;
    /** Particle effect ID (matches an id built by {@link hr.brajnovic.td.fx.ParticleEffectManager}) spawned on impact. */
    public String impactParticleId;
    /** Light ID (matches an id in {@link hr.brajnovic.td.fx.LightEffectManager}) flashed briefly when this tower fires; null = no muzzle flash. */
    public String muzzleFlashLightId;
    /** Light ID that travels with this tower's projectile until impact; null = no projectile light. */
    public String projectileLightId;

    /** Base name of the sprites-src sheet (PNG + JSON) under assets/sprites-src/, e.g. "tower_atlas". */
    public String spriteSheetId;
    /** Degrees added on top of (turretAngleDeg - 90) to correct for the art's drawn-facing direction. */
    public float spriteRotationOffsetDeg = 0f;
    /** Lifts the turret sprite up off the base sprite, in tile units (e.g. 0.15 = 15% of a tile up). */
    public float turretVerticalOffsetTiles = 0f;
    /** Degrees added to the projectile's travel angle to correct for the art's drawn-facing direction. */
    public float projectileSpriteRotationOffsetDeg = 0f;
    public float shootAnimationDurationSeconds = 0.3f;
    public float projectileImpactDurationSeconds = 0.2f;
}
