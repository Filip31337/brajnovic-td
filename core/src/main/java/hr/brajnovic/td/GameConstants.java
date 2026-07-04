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

    private GameConstants() {
    }
}
