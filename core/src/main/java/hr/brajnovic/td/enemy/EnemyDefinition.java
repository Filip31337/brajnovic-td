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

    /** Prepared for V2 elemental towers: damageType -> multiplier. Empty while damage is flat. */
    public ObjectMap<String, Float> resistances;
}
