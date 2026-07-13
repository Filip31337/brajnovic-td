package hr.brajnovic.td.wave;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import hr.brajnovic.td.ecs.PositionComponent;
import hr.brajnovic.td.economy.Economy;
import hr.brajnovic.td.enemy.BossBehaviorTreeFactory;
import hr.brajnovic.td.enemy.BossComponent;
import hr.brajnovic.td.enemy.EnemyComponent;
import hr.brajnovic.td.enemy.EnemyDefinition;
import hr.brajnovic.td.enemy.EnemyRegistry;
import hr.brajnovic.td.enemy.EnemyState;
import hr.brajnovic.td.fx.ParticleEffectManager;
import hr.brajnovic.td.map.GridMap;
import hr.brajnovic.td.map.LevelDefinition;
import hr.brajnovic.td.pathfinding.AStarPathfinder;
import hr.brajnovic.td.sound.SoundManager;

import java.util.List;

/** Build/Wave state machine: placement is only allowed while phase == BUILD. */
public class WaveController {

    private static final float SPAWN_INTERVAL_SECONDS = 0.6f;
    private static final float BUILD_PHASE_DURATION_SECONDS = 60f;
    private static final Family ENEMY_FAMILY = Family.all(EnemyComponent.class).get();

    private final LevelDefinition level;
    private final EnemyRegistry enemyRegistry;
    private final GridMap gridMap;
    private final Economy economy;
    private final PooledEngine engine;
    private final ParticleEffectManager particleEffectManager;

    private GamePhase phase = GamePhase.BUILD;
    private int currentWaveNumber = 0;
    private int enemiesRemainingToSpawn = 0;
    private float spawnTimer = 0f;
    private float buildPhaseTimer = BUILD_PHASE_DURATION_SECONDS;
    private int nextEnemySpawnId = 0;

    public WaveController(LevelDefinition level, EnemyRegistry enemyRegistry, GridMap gridMap, Economy economy,
                           PooledEngine engine, ParticleEffectManager particleEffectManager) {
        this.level = level;
        this.enemyRegistry = enemyRegistry;
        this.gridMap = gridMap;
        this.economy = economy;
        this.engine = engine;
        this.particleEffectManager = particleEffectManager;
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
        SoundManager.play("drums_wave_starting");
    }

    public void update(float delta) {
        if (phase == GamePhase.BUILD) {
            updateBuildPhase(delta);
            return;
        }

        updateSpawning(delta);

        if (enemiesRemainingToSpawn <= 0 && countActiveEnemies() == 0) {
            completeWave();
        }
    }

    private int countActiveEnemies() {
        return engine.getEntitiesFor(ENEMY_FAMILY).size();
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

        EnemyComponent enemyComponent = engine.createComponent(EnemyComponent.class);
        enemyComponent.spawnId = nextEnemySpawnId++;
        enemyComponent.definition = definition;
        enemyComponent.path = path;
        enemyComponent.maxHp = definition.maxHp * hpMultiplier;
        enemyComponent.hp = enemyComponent.maxHp;
        if (path.size() == 1) {
            enemyComponent.reachedGoal = true;
        }

        PositionComponent positionComponent = engine.createComponent(PositionComponent.class);
        GridPoint2 start = path.get(0);
        positionComponent.value.set(start.x + 0.5f, start.y + 0.5f);

        Entity entity = engine.createEntity();
        enemyComponent.stateMachine = new DefaultStateMachine<>(entity, EnemyState.WALKING);
        entity.add(enemyComponent);
        entity.add(positionComponent);

        if (definition.healIntervalSeconds > 0f) {
            BossComponent bossComponent = engine.createComponent(BossComponent.class);
            bossComponent.healCooldownTimer = definition.healIntervalSeconds;
            bossComponent.behaviorTree = BossBehaviorTreeFactory.build(entity, particleEffectManager);
            entity.add(bossComponent);
        }

        engine.addEntity(entity);
    }

    private GridPoint2 pickReachableGoal(GridPoint2 spawn) {
        for (GridPoint2 goal : gridMap.getGoals()) {
            if (AStarPathfinder.isReachable(gridMap, spawn, goal)) {
                return goal;
            }
        }
        throw new IllegalStateException("No reachable goal from spawn " + spawn);
    }

    private void completeWave() {
        int bonus = Math.round(level.waveClearGoldBonusBase * (float) Math.pow(level.waveClearGoldBonusScalePerWave, currentWaveNumber - 1));
        economy.addGold(bonus);
        phase = GamePhase.BUILD;
        buildPhaseTimer = BUILD_PHASE_DURATION_SECONDS;
        SoundManager.play("drums_wave_completed");
    }
}
