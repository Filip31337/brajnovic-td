package hr.brajnovic.td.tower;

/** Data-driven tower definition, loaded from assets/data/towers.json. */
public class TowerDefinition {

    public String id;
    public String name;
    public int cost;
    public float rangeTiles;
    public float damage;
    public float fireRatePerSecond;
    public float projectileSpeedTilesPerSec;
    public float turretIdleRevolutionSeconds;
    public float turretTrackingDegPerSec;
    public float fireAngleThresholdDeg;

    /** Prepared for V2 elemental towers; unused while damage is flat. */
    public String damageType;
    /** Prepared for future SoundManager wiring; no-op until real audio lands. */
    public String shootSoundId;

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
