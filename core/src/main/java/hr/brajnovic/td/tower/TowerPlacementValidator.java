package hr.brajnovic.td.tower;

import hr.brajnovic.td.map.GridMap;
import hr.brajnovic.td.pathfinding.AStarPathfinder;

/** Gates tower placement so it can never fully block every spawn from reaching a goal. */
public final class TowerPlacementValidator {

    private TowerPlacementValidator() {
    }

    public static boolean canPlaceTowerAt(GridMap gridMap, int x, int y) {
        if (!gridMap.isBuildable(x, y)) {
            return false;
        }

        gridMap.placeTower(x, y);
        boolean stillSolvable = AStarPathfinder.allSpawnsCanReachAGoal(gridMap, gridMap.getSpawns(), gridMap.getGoals());
        gridMap.removeTower(x, y);

        return stillSolvable;
    }
}
