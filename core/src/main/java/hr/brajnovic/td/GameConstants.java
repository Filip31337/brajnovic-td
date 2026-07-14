package hr.brajnovic.td;

public final class GameConstants {

    public static final int TILE_SIZE_PX = 32;
    public static final int TILE_SCALE = 2;
    public static final int SCALED_TILE_SIZE_PX = TILE_SIZE_PX * TILE_SCALE;

    public static final int GRID_WIDTH_TILES = 20;
    public static final int GRID_HEIGHT_TILES = 12;

    public static final int MAP_VIEWPORT_WIDTH_PX = GRID_WIDTH_TILES * SCALED_TILE_SIZE_PX;
    public static final int MAP_VIEWPORT_HEIGHT_PX = GRID_HEIGHT_TILES * SCALED_TILE_SIZE_PX;

    public static final int HUD_WIDTH_PX = 260;

    public static final int WINDOW_WIDTH_PX = MAP_VIEWPORT_WIDTH_PX + HUD_WIDTH_PX;
    public static final int WINDOW_HEIGHT_PX = MAP_VIEWPORT_HEIGHT_PX;

    public static final float ENEMY_SPEED_TILES_PER_SEC = 80f / SCALED_TILE_SIZE_PX;
    public static final float ENEMY_HIT_FLASH_DURATION_SECONDS = 0.12f;

    /** gdx-ai Steering tuning (FollowPath + Separation blend, see EnemyLifecycleSystem/SteeringComponent). */
    public static final float ENEMY_BOUNDING_RADIUS_TILES = 0.4f;
    public static final float ENEMY_STEERING_MAX_ACCELERATION_TILES_PER_SEC2 = 20f;
    public static final float ENEMY_STEERING_PATH_OFFSET_TILES = 0.5f;
    public static final float ENEMY_SEPARATION_RADIUS_TILES = 1.2f;
    public static final float ENEMY_SEPARATION_DECAY_COEFFICIENT = 4f;
    public static final float ENEMY_SEPARATION_WEIGHT = 0.6f;
    public static final float ENEMY_MAX_PATH_DEVIATION_TILES = 0.3f;
    public static final float ENEMY_GOAL_ARRIVAL_TOLERANCE_TILES = 0.05f;

    public static final String FIRST_LEVEL_ID = "level_01";

    private GameConstants() {
    }
}
