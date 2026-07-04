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
}
