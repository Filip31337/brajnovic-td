package hr.brajnovic.td.wave;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import hr.brajnovic.td.economy.Economy;
import hr.brajnovic.td.enemy.Enemy;
import hr.brajnovic.td.enemy.EnemyDefinition;
import hr.brajnovic.td.enemy.EnemyRegistry;
import hr.brajnovic.td.map.GridMap;
import hr.brajnovic.td.map.LevelDefinition;
import hr.brajnovic.td.pathfinding.AStarPathfinder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Build/Wave state machine: placement is only allowed while phase == BUILD. */
public class WaveController {

    private static final float SPAWN_INTERVAL_SECONDS = 0.6f;
    private static final float BUILD_PHASE_DURATION_SECONDS = 60f;

    private final LevelDefinition level;
    private final EnemyRegistry enemyRegistry;
    private final GridMap gridMap;
    private final Economy economy;
    private final List<Enemy> activeEnemies = new ArrayList<>();

    private GamePhase phase = GamePhase.BUILD;
    private int currentWaveNumber = 0;
    private int enemiesRemainingToSpawn = 0;
    private float spawnTimer = 0f;
    private float buildPhaseTimer = BUILD_PHASE_DURATION_SECONDS;

    public WaveController(LevelDefinition level, EnemyRegistry enemyRegistry, GridMap gridMap, Economy economy) {
        this.level = level;
        this.enemyRegistry = enemyRegistry;
        this.gridMap = gridMap;
        this.economy = economy;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public int getCurrentWaveNumber() {
        return currentWaveNumber;
    }

    public int getTotalWaveCount() {
        return level.waveCount;
    }

    public List<Enemy> getActiveEnemies() {
        return activeEnemies;
    }

    public boolean canStartNextWave() {
        return phase == GamePhase.BUILD && currentWaveNumber < level.waveCount;
    }

    public int getBuildPhaseSecondsRemaining() {
        return Math.max(0, MathUtils.ceil(buildPhaseTimer));
    }

    /** The very first build phase of a level has no auto-start timer, giving the player unlimited setup time. */
    public boolean isBuildPhaseTimerActive() {
        return canStartNextWave() && currentWaveNumber > 0;
    }

    public boolean allWavesCleared() {
        return phase == GamePhase.BUILD && currentWaveNumber >= level.waveCount;
    }

    private boolean isBossWave(int waveNumber) {
        return level.bossWaveInterval > 0 && waveNumber % level.bossWaveInterval == 0;
    }

    public void startNextWave() {
        if (!canStartNextWave()) {
            return;
        }
        currentWaveNumber++;
        enemiesRemainingToSpawn = isBossWave(currentWaveNumber)
            ? 1
            : Math.round(level.baseEnemyCountPerWave * (float) Math.pow(level.countScalePerWave, currentWaveNumber - 1));
        spawnTimer = 0f;
        phase = GamePhase.WAVE;
    }

    public void update(float delta) {
        if (phase == GamePhase.BUILD) {
            updateBuildPhase(delta);
            return;
        }

        updateSpawning(delta);
        updateEnemies(delta);

        if (enemiesRemainingToSpawn <= 0 && activeEnemies.isEmpty()) {
            completeWave();
        }
    }

    private void updateBuildPhase(float delta) {
        if (!isBuildPhaseTimerActive()) {
            return;
        }
        buildPhaseTimer -= delta;
        if (buildPhaseTimer <= 0f) {
            startNextWave();
        }
    }

    private void updateSpawning(float delta) {
        if (enemiesRemainingToSpawn <= 0) {
            return;
        }
        spawnTimer -= delta;
        if (spawnTimer <= 0f) {
            spawnEnemy();
            enemiesRemainingToSpawn--;
            spawnTimer = SPAWN_INTERVAL_SECONDS;
        }
    }

    private void spawnEnemy() {
        boolean bossWave = isBossWave(currentWaveNumber);
        String enemyId = bossWave ? level.bossEnemyId : level.baseEnemyId;
        EnemyDefinition definition = enemyRegistry.get(enemyId);

        float hpMultiplier = (float) Math.pow(level.hpScalePerWave, currentWaveNumber - 1);
        if (bossWave) {
            hpMultiplier *= level.bossHpMultiplier;
        }

        GridPoint2 spawnPoint = gridMap.getSpawns().get(0);
        GridPoint2 goalPoint = pickReachableGoal(spawnPoint);
        List<GridPoint2> path = AStarPathfinder.findPath(gridMap, spawnPoint, goalPoint);
        if (path == null) {
            return;
        }

        activeEnemies.add(new Enemy(definition, path, hpMultiplier));
    }

    private GridPoint2 pickReachableGoal(GridPoint2 spawn) {
        for (GridPoint2 goal : gridMap.getGoals()) {
            if (AStarPathfinder.isReachable(gridMap, spawn, goal)) {
                return goal;
            }
        }
        throw new IllegalStateException("No reachable goal from spawn " + spawn);
    }

    private void updateEnemies(float delta) {
        Iterator<Enemy> iterator = activeEnemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            if (enemy.isDead()) {
                economy.addGold(enemy.getDefinition().goldReward);
                iterator.remove();
                continue;
            }
            enemy.update(delta);
            if (enemy.hasReachedGoal()) {
                economy.loseLives(enemy.getDefinition().livesDamage);
                iterator.remove();
            }
        }
    }

    private void completeWave() {
        int bonus = Math.round(level.waveClearGoldBonusBase * (float) Math.pow(level.waveClearGoldBonusScalePerWave, currentWaveNumber - 1));
        economy.addGold(bonus);
        phase = GamePhase.BUILD;
        buildPhaseTimer = BUILD_PHASE_DURATION_SECONDS;
    }
}
