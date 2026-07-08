package hr.brajnovic.td.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import hr.brajnovic.td.map.GridMap;

/**
 * Live view of a {@link GridMap} as an 8-directional gdx-ai graph. Connections are computed on
 * demand (not cached) so that placing/removing a tower is immediately reflected in the next search,
 * same corner-cutting rule as before: a diagonal move is rejected if either adjacent orthogonal
 * tile is not walkable.
 *
 * <p>{@link IndexedAStarPathFinder} compares nodes by reference identity (not {@code equals()}) to
 * detect the goal, so every tile must be represented by the exact same {@link GridPoint2} instance
 * everywhere it appears (both here and as the start/goal passed to the search) — hence the
 * pre-built {@code nodesByIndex} cache instead of allocating a fresh node per neighbor.
 */
final class GridIndexedGraph implements IndexedGraph<GridPoint2> {

    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DY = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final float ORTHOGONAL_COST = 1f;
    private static final float DIAGONAL_COST = (float) Math.sqrt(2);

    private final GridMap gridMap;
    private final GridPoint2[] nodesByIndex;

    GridIndexedGraph(GridMap gridMap) {
        this.gridMap = gridMap;
        int width = gridMap.getWidthInTiles();
        int height = gridMap.getHeightInTiles();
        this.nodesByIndex = new GridPoint2[width * height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                nodesByIndex[x + y * width] = new GridPoint2(x, y);
            }
        }
    }

    /** The canonical node instance for a tile; callers must search using these, not ad-hoc GridPoint2s. */
    GridPoint2 nodeAt(int x, int y) {
        return nodesByIndex[x + y * gridMap.getWidthInTiles()];
    }

    @Override
    public int getIndex(GridPoint2 node) {
        return node.x + node.y * gridMap.getWidthInTiles();
    }

    @Override
    public int getNodeCount() {
        return nodesByIndex.length;
    }

    @Override
    public Array<Connection<GridPoint2>> getConnections(GridPoint2 fromNode) {
        Array<Connection<GridPoint2>> connections = new Array<>(8);
        for (int direction = 0; direction < 8; direction++) {
            int dx = DX[direction];
            int dy = DY[direction];
            int neighborX = fromNode.x + dx;
            int neighborY = fromNode.y + dy;

            if (!gridMap.isWalkable(neighborX, neighborY)) {
                continue;
            }
            boolean diagonal = dx != 0 && dy != 0;
            if (diagonal && (!gridMap.isWalkable(fromNode.x + dx, fromNode.y)
                || !gridMap.isWalkable(fromNode.x, fromNode.y + dy))) {
                continue;
            }

            float cost = diagonal ? DIAGONAL_COST : ORTHOGONAL_COST;
            connections.add(new GridConnection(fromNode, nodeAt(neighborX, neighborY), cost));
        }
        return connections;
    }

    private static final class GridConnection implements Connection<GridPoint2> {
        private final GridPoint2 fromNode;
        private final GridPoint2 toNode;
        private final float cost;

        GridConnection(GridPoint2 fromNode, GridPoint2 toNode, float cost) {
            this.fromNode = fromNode;
            this.toNode = toNode;
            this.cost = cost;
        }

        @Override
        public float getCost() {
            return cost;
        }

        @Override
        public GridPoint2 getFromNode() {
            return fromNode;
        }

        @Override
        public GridPoint2 getToNode() {
            return toNode;
        }
    }
}
