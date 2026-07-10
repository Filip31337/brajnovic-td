package hr.brajnovic.td.tower;

/** Upgrade/sell formulas shared by targeting (effective stats) and the tower info UI. */
public final class TowerUpgrade {

    public static final int MAX_LEVEL = 15;

    /** Milestone levels grant a one-time +20% bonus to a specific stat, on top of the regular per-level growth. */
    private static final int DAMAGE_MILESTONE_LEVEL = 5;
    private static final int RANGE_MILESTONE_LEVEL = 10;
    private static final int FIRE_RATE_MILESTONE_LEVEL = 15;

    private static final float PER_LEVEL_DAMAGE_GROWTH = 0.10f;
    private static final float MILESTONE_BONUS_RATIO = 0.20f;
    private static final float SELL_REFUND_RATIO = 0.7f;

    public enum MilestoneStat {
        DAMAGE, RANGE, FIRE_RATE
    }

    private TowerUpgrade() {
    }

    public static boolean canUpgrade(int currentLevel) {
        return currentLevel < MAX_LEVEL;
    }

    /** Gold cost of any single upgrade level-up (flat, same as base tower cost). */
    public static int upgradeCost(TowerDefinition definition) {
        return definition.cost;
    }

    public static MilestoneStat milestoneAtLevel(int level) {
        if (level == DAMAGE_MILESTONE_LEVEL) {
            return MilestoneStat.DAMAGE;
        }
        if (level == RANGE_MILESTONE_LEVEL) {
            return MilestoneStat.RANGE;
        }
        if (level == FIRE_RATE_MILESTONE_LEVEL) {
            return MilestoneStat.FIRE_RATE;
        }
        return null;
    }

    public static float damageForLevel(TowerDefinition definition, int level) {
        float growth = 1f + PER_LEVEL_DAMAGE_GROWTH * (level - 1);
        float milestoneBonus = level >= DAMAGE_MILESTONE_LEVEL ? 1f + MILESTONE_BONUS_RATIO : 1f;
        return definition.damage * growth * milestoneBonus;
    }

    public static float rangeForLevel(TowerDefinition definition, int level) {
        float milestoneBonus = level >= RANGE_MILESTONE_LEVEL ? 1f + MILESTONE_BONUS_RATIO : 1f;
        return definition.rangeTiles * milestoneBonus;
    }

    public static float fireRateForLevel(TowerDefinition definition, int level) {
        float milestoneBonus = level >= FIRE_RATE_MILESTONE_LEVEL ? 1f + MILESTONE_BONUS_RATIO : 1f;
        return definition.fireRatePerSecond * milestoneBonus;
    }

    public static int sellRefund(int totalInvested) {
        return Math.round(totalInvested * SELL_REFUND_RATIO);
    }
}
