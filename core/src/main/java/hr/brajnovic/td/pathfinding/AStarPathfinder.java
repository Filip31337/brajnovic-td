package hr.brajnovic.td.pathfinding;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.math.GridPoint2;
import hr.brajnovic.td.map.GridMap;

import java.util.ArrayList;
import java.util.List;

/**
 * 8-directional A* over a {@link GridMap}, backed by gdx-ai's {@link IndexedAStarPathFinder} via
 * {@link GridIndexedGraph}. Diagonal moves are disallowed if either adjacent orthogonal tile is not
 * walkable, to prevent cutting through a blocked corner.
 */
public final class AStarPathfinder {

    private static final float ORTHOGONAL_COST = 1f;
    private static final float DIAGONAL_COST = (float) Math.sqrt(2);

    private static final Heuristic<GridPoint2> HEURISTIC = (node, endNode) -> {
        int dx = Math.abs(node.x - endNode.x);
        int dy = Math.abs(node.y - endNode.y);
        int diagonalSteps = Math.min(dx, dy);
        int straightSteps = Math.abs(dx - dy);
        return diagonalSteps * DIAGONAL_COST + straightSteps * ORTHOGONAL_COST;
    };

    private AStarPathfinder() {
    }

    /** Returns the waypoint list (tile centers, start to goal inclusive) or null if unreachable. */
    public static List<GridPoint2> findPath(GridMap gridMap, GridPoint2 start, GridPoint2 goal) {
        if (!gridMap.isWalkable(start.x, start.y) || !gridMap.isWalkable(goal.x, goal.y)) {
            return null;
        }

        GridIndexedGraph graph = new GridIndexedGraph(gridMap);
        IndexedAStarPathFinder<GridPoint2> pathFinder = new IndexedAStarPathFinder<>(graph);
        DefaultGraphPath<GridPoint2> outPath = new DefaultGraphPath<>();
        // IndexedAStarPathFinder compares nodes by reference identity, so the search must run on the
        // graph's own canonical node instances, not the caller's start/goal objects (see GridIndexedGraph).
        boolean found = pathFinder.searchNodePath(graph.nodeAt(start.x, start.y), graph.nodeAt(goal.x, goal.y), HEURISTIC, outPath);
        if (!found) {
            return null;
        }

        List<GridPoint2> path = new ArrayList<>(outPath.getCount());
        for (GridPoint2 node : outPath) {
            path.add(node);
        }
        return path;
    }

    public static boolean isReachable(GridMap gridMap, GridPoint2 start, GridPoint2 goal) {
        return findPath(gridMap, start, goal) != null;
    }

    /** True if every spawn can still reach at least one goal on the current grid state. */
    public static boolean allSpawnsCanReachAGoal(GridMap gridMap, List<GridPoint2> spawns, List<GridPoint2> goals) {
        for (GridPoint2 spawn : spawns) {
            boolean reachable = false;
            for (GridPoint2 goal : goals) {
                if (isReachable(gridMap, spawn, goal)) {
                    reachable = true;
                    break;
                }
            }
            if (!reachable) {
                return false;
            }
        }
        return true;
    }
}
