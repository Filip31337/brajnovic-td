package hr.brajnovic.td.map;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import hr.brajnovic.td.GameConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Logical grid data (tile types, spawn/goal points, tower occupancy) extracted from a Tiled map. y=0 is the bottom row. */
public class GridMap {

    private final int widthInTiles;
    private final int heightInTiles;
    private final TileType[][] tileTypes;
    private final boolean[][] towerOccupied;
    private final List<GridPoint2> spawns;
    private final List<GridPoint2> goals;

    private GridMap(int widthInTiles, int heightInTiles, TileType[][] tileTypes,
                     List<GridPoint2> spawns, List<GridPoint2> goals) {
        this.widthInTiles = widthInTiles;
        this.heightInTiles = heightInTiles;
        this.tileTypes = tileTypes;
        this.towerOccupied = new boolean[widthInTiles][heightInTiles];
        this.spawns = spawns;
        this.goals = goals;
    }

    public static GridMap fromTiledMap(TiledMap tiledMap) {
        TiledMapTileLayer groundLayer = (TiledMapTileLayer) tiledMap.getLayers().get("ground");
        int width = groundLayer.getWidth();
        int height = groundLayer.getHeight();

        TileType[][] tileTypes = new TileType[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                TiledMapTileLayer.Cell cell = groundLayer.getCell(x, y);
                String type = cell.getTile().getProperties().get("type", String.class);
                tileTypes[x][y] = TileType.fromTiledProperty(type);
            }
        }

        List<GridPoint2> spawns = new ArrayList<>();
        List<GridPoint2> goals = new ArrayList<>();
        for (MapObject object : tiledMap.getLayers().get("markers").getObjects()) {
            if (!(object instanceof RectangleMapObject)) {
                continue;
            }
            Rectangle rectangle = ((RectangleMapObject) object).getRectangle();
            int tileX = (int) ((rectangle.x + rectangle.width / 2f) / GameConstants.TILE_SIZE_PX);
            int tileY = (int) ((rectangle.y + rectangle.height / 2f) / GameConstants.TILE_SIZE_PX);
            String objectType = object.getProperties().get("type", String.class);
            if ("spawn".equals(objectType)) {
                spawns.add(new GridPoint2(tileX, tileY));
            } else if ("goal".equals(objectType)) {
                goals.add(new GridPoint2(tileX, tileY));
            }
        }

        return new GridMap(width, height, tileTypes, spawns, goals);
    }

    public int getWidthInTiles() {
        return widthInTiles;
    }

    public int getHeightInTiles() {
        return heightInTiles;
    }

    public List<GridPoint2> getSpawns() {
        return Collections.unmodifiableList(spawns);
    }

    public List<GridPoint2> getGoals() {
        return Collections.unmodifiableList(goals);
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < widthInTiles && y >= 0 && y < heightInTiles;
    }

    public boolean isWalkable(int x, int y) {
        if (!isInBounds(x, y)) {
            return false;
        }
        TileType type = tileTypes[x][y];
        return type == TileType.PATH || (type == TileType.BUILDABLE && !towerOccupied[x][y]);
    }

    public boolean isBuildable(int x, int y) {
        return isInBounds(x, y) && tileTypes[x][y] == TileType.BUILDABLE && !towerOccupied[x][y];
    }

    public void placeTower(int x, int y) {
        if (!isBuildable(x, y)) {
            throw new IllegalStateException("Tile not buildable: (" + x + ", " + y + ")");
        }
        towerOccupied[x][y] = true;
    }

    public void removeTower(int x, int y) {
        towerOccupied[x][y] = false;
    }
}
