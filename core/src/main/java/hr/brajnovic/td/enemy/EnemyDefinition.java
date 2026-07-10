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

    /** Prepared for V2 elemental towers: damageType -> multiplier. Empty while damage is flat. */
    public ObjectMap<String, Float> resistances;
}
