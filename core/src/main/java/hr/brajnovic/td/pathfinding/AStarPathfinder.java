package hr.brajnovic.td.pathfinding;

import com.badlogic.gdx.math.GridPoint2;
import hr.brajnovic.td.map.GridMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * 8-directional A* over a {@link GridMap}. Diagonal moves are disallowed if either adjacent
 * orthogonal tile is not walkable, to prevent cutting through a blocked corner.
 */
public final class AStarPathfinder {

    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DY = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final float ORTHOGONAL_COST = 1f;
    private static final float DIAGONAL_COST = (float) Math.sqrt(2);

    private AStarPathfinder() {
    }

    /** Returns the waypoint list (tile centers, start to goal inclusive) or null if unreachable. */
    public static List<GridPoint2> findPath(GridMap gridMap, GridPoint2 start, GridPoint2 goal) {
        if (!gridMap.isWalkable(start.x, start.y) || !gridMap.isWalkable(goal.x, goal.y)) {
            return null;
        }

        Map<Long, Float> gScore = new HashMap<>();
        Map<Long, GridPoint2> cameFrom = new HashMap<>();
        PriorityQueue<Node> openQueue = new PriorityQueue<>();

        long startKey = key(start.x, start.y);
        gScore.put(startKey, 0f);
        openQueue.add(new Node(start.x, start.y, heuristic(start, goal)));

        while (!openQueue.isEmpty()) {
            Node current = openQueue.poll();
            long currentKey = key(current.x, current.y);

            if (current.x == goal.x && current.y == goal.y) {
                return reconstructPath(cameFrom, new GridPoint2(current.x, current.y));
            }

            float currentG = gScore.getOrDefault(currentKey, Float.MAX_VALUE);
            if (current.fScore > currentG + heuristic(new GridPoint2(current.x, current.y), goal) + 1e-4f) {
                continue;
            }

            for (int direction = 0; direction < 8; direction++) {
                int dx = DX[direction];
                int dy = DY[direction];
                int neighborX = current.x + dx;
                int neighborY = current.y + dy;

                if (!gridMap.isWalkable(neighborX, neighborY)) {
                    continue;
                }
                boolean diagonal = dx != 0 && dy != 0;
                if (diagonal && (!gridMap.isWalkable(current.x + dx, current.y)
                    || !gridMap.isWalkable(current.x, current.y + dy))) {
                    continue;
                }

                float moveCost = diagonal ? DIAGONAL_COST : ORTHOGONAL_COST;
                float tentativeG = currentG + moveCost;
                long neighborKey = key(neighborX, neighborY);

                if (tentativeG < gScore.getOrDefault(neighborKey, Float.MAX_VALUE)) {
                    cameFrom.put(neighborKey, new GridPoint2(current.x, current.y));
                    gScore.put(neighborKey, tentativeG);
                    float f = tentativeG + heuristic(new GridPoint2(neighborX, neighborY), goal);
                    openQueue.add(new Node(neighborX, neighborY, f));
                }
            }
        }

        return null;
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

    private static List<GridPoint2> reconstructPath(Map<Long, GridPoint2> cameFrom, GridPoint2 current) {
        List<GridPoint2> path = new ArrayList<>();
        path.add(current);
        GridPoint2 point = current;
        while (cameFrom.containsKey(key(point.x, point.y))) {
            point = cameFrom.get(key(point.x, point.y));
            path.add(point);
        }
        Collections.reverse(path);
        return path;
    }

    private static float heuristic(GridPoint2 a, GridPoint2 b) {
        int dx = Math.abs(a.x - b.x);
        int dy = Math.abs(a.y - b.y);
        int diagonalSteps = Math.min(dx, dy);
        int straightSteps = Math.abs(dx - dy);
        return diagonalSteps * DIAGONAL_COST + straightSteps * ORTHOGONAL_COST;
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    private static final class Node implements Comparable<Node> {
        final int x;
        final int y;
        final float fScore;

        Node(int x, int y, float fScore) {
            this.x = x;
            this.y = y;
            this.fScore = fScore;
        }

        @Override
        public int compareTo(Node other) {
            return Float.compare(fScore, other.fScore);
        }
    }
}
