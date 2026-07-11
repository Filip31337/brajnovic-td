package hr.brajnovic.td.map;

/** Data-driven level definition, loaded from assets/data/levels/level_XX.json. */
public class LevelDefinition {

    public String id;
    /** Path to the Tiled map, relative to assets/, e.g. "maps/level_01.tmx". */
    public String tmxPath;

    public int startingGold;
    public int startingLives;

    public int waveCount;
    public String baseEnemyId;
    public int baseEnemyCountPerWave;
    public float hpScalePerWave;
    public float countScalePerWave;

    /** Every 5th wave is a solo boss wave using this enemy id (may equal baseEnemyId). */
    public int bossWaveInterval = 5;
    public String bossEnemyId;
    public float bossHpMultiplier;

    public int waveClearGoldBonusBase;
    public float waveClearGoldBonusScalePerWave;

    /** Id of the level to play next (e.g. "level_02"), or null if this is the final level in the chain. */
    public String nextLevelId;
}
