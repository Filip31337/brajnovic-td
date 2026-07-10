package hr.brajnovic.td.screen;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import hr.brajnovic.td.BrajnovicTD;
import hr.brajnovic.td.GameConstants;
import hr.brajnovic.td.ecs.Mappers;
import hr.brajnovic.td.ecs.PositionComponent;
import hr.brajnovic.td.economy.Economy;
import hr.brajnovic.td.enemy.EnemyComponent;
import hr.brajnovic.td.enemy.EnemyLifecycleSystem;
import hr.brajnovic.td.enemy.EnemyRegistry;
import hr.brajnovic.td.enemy.EnemyState;
import hr.brajnovic.td.enemy.EnemyStatusEffectSystem;
import hr.brajnovic.td.i18n.Localization;
import hr.brajnovic.td.map.GridMap;
import hr.brajnovic.td.map.LevelDefinition;
import hr.brajnovic.td.map.LevelLoader;
import hr.brajnovic.td.render.SpriteSheet;
import hr.brajnovic.td.tower.ProjectileComponent;
import hr.brajnovic.td.tower.ProjectileSystem;
import hr.brajnovic.td.tower.TowerComponent;
import hr.brajnovic.td.tower.TowerDefinition;
import hr.brajnovic.td.tower.TowerPlacementValidator;
import hr.brajnovic.td.tower.TowerRegistry;
import hr.brajnovic.td.tower.TowerState;
import hr.brajnovic.td.tower.TowerTargetingSystem;
import hr.brajnovic.td.tower.TowerUpgrade;
import hr.brajnovic.td.ui.SkinFactory;
import hr.brajnovic.td.wave.GamePhase;
import hr.brajnovic.td.wave.WaveController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class GameScreen implements Screen {

    private static final int SCALE = GameConstants.SCALED_TILE_SIZE_PX;
    private static final float ENEMY_FRAME_DURATION_SECONDS = 0.12f;
    private static final float TOWER_IDLE_FRAME_DURATION_SECONDS = 0.5f;
    private static final float PROJECTILE_FRAME_DURATION_SECONDS = 0.08f;

    private static final Family TOWER_FAMILY = Family.all(TowerComponent.class, PositionComponent.class).get();
    private static final Family ENEMY_FAMILY = Family.all(EnemyComponent.class, PositionComponent.class).get();
    private static final Family PROJECTILE_FAMILY = Family.all(ProjectileComponent.class, PositionComponent.class).get();

    private final BrajnovicTD game;

    private final TowerRegistry towerRegistry;
    private final EnemyRegistry enemyRegistry;
    private final LevelDefinition level;

    private final TiledMap tiledMap;
    private final GridMap gridMap;
    private final OrthogonalTiledMapRenderer mapRenderer;

    private final Economy economy;
    private final PooledEngine engine;
    private final WaveController waveController;

    private final OrthographicCamera mapCamera;
    private final Viewport mapViewport;
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch spriteBatch;
    private final Map<String, SpriteSheet> enemySpriteSheetsById = new HashMap<>();
    private final Map<String, SpriteSheet> towerSpriteSheetsById = new HashMap<>();

    private final Stage hudStage;
    private final Stage overlayStage;
    private final Skin skin;
    private final InputMultiplexer inputMultiplexer;

    private Label goldLabel;
    private Label livesLabel;
    private Label waveLabel;
    private Label nextWaveTimerLabel;
    private TextButton startWaveButton;
    private final Map<String, TextButton> towerButtonsById = new LinkedHashMap<>();
    private TextButton pauseButton;
    private TextButton normalSpeedButton;
    private TextButton fastSpeedButton;
    private float timeScale = 1f;

    private Window overlayWindow;
    private Label overlayMessageLabel;
    private boolean gameEnded = false;

    private Window towerInfoWindow;
    private Image towerInfoIconBase;
    private Image towerInfoIconTurret;
    private Label towerInfoLevelLabel;
    private Label towerInfoDamageLabel;
    private Label towerInfoRangeLabel;
    private Label towerInfoFireRateLabel;
    private Label towerInfoNextLevelLabel;
    private TextButton towerUpgradeButton;
    private TextButton towerSellButton;

    private String selectedTowerId = null;
    private Entity selectedTowerEntity = null;
    private int hoverTileX = Integer.MIN_VALUE;
    private int hoverTileY = Integer.MIN_VALUE;

    public GameScreen(BrajnovicTD game) {
        this.game = game;

        towerRegistry = TowerRegistry.loadFromInternal("data/towers.json");
        enemyRegistry = EnemyRegistry.loadFromInternal("data/enemies.json");
        level = LevelLoader.loadFromInternal("data/levels/level_01.json");

        tiledMap = new TmxMapLoader().load(level.tmxPath);
        gridMap = GridMap.fromTiledMap(tiledMap);
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, GameConstants.TILE_SCALE);

        economy = new Economy(level.startingGold, level.startingLives);
        engine = new PooledEngine();
        engine.addSystem(new EnemyStatusEffectSystem());
        engine.addSystem(new EnemyLifecycleSystem(economy));
        engine.addSystem(new TowerTargetingSystem());
        engine.addSystem(new ProjectileSystem());
        waveController = new WaveController(level, enemyRegistry, gridMap, economy, engine);

        mapCamera = new OrthographicCamera();
        mapViewport = new ExtendViewport(
            GameConstants.MAP_VIEWPORT_WIDTH_PX,
            GameConstants.MAP_VIEWPORT_HEIGHT_PX,
            mapCamera
        );
        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();
        for (var definition : enemyRegistry.all()) {
            if (definition.spriteSheetId != null) {
                enemySpriteSheetsById.computeIfAbsent(definition.spriteSheetId,
                    id -> SpriteSheet.loadFromInternal("sprites-src/" + id));
            }
        }
        for (var definition : towerRegistry.all()) {
            if (definition.spriteSheetId != null) {
                towerSpriteSheetsById.computeIfAbsent(definition.spriteSheetId,
                    id -> SpriteSheet.loadFromInternal("sprites-src/" + id));
            }
        }

        skin = SkinFactory.createSkin();
        hudStage = new Stage(new ScreenViewport());
        overlayStage = new Stage(new ScreenViewport());
        buildHud();
        buildOverlay();
        buildTowerInfoWindow();

        inputMultiplexer = new InputMultiplexer(overlayStage, hudStage, mapInputProcessor);

        resize(GameConstants.WINDOW_WIDTH_PX, GameConstants.WINDOW_HEIGHT_PX);
    }

    private final InputAdapter mapInputProcessor = new InputAdapter() {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (button == Input.Buttons.RIGHT) {
                cancelPlacement();
                cancelSelection();
                return true;
            }
            if (button == Input.Buttons.LEFT) {
                updateHoverTile(screenX, screenY);
                if (selectedTowerId != null) {
                    tryPlaceSelectedTower();
                    return true;
                }
                if (waveController.getPhase() == GamePhase.BUILD) {
                    return trySelectTowerAt(hoverTileX, hoverTileY);
                }
            }
            return false;
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            updateHoverTile(screenX, screenY);
            return false;
        }

        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
                cancelPlacement();
                cancelSelection();
                return true;
            }
            return false;
        }
    };

    private void updateHoverTile(int screenX, int screenY) {
        Vector2 point = new Vector2(screenX, screenY);
        mapViewport.unproject(point);
        hoverTileX = (int) Math.floor(point.x / SCALE);
        hoverTileY = (int) Math.floor(point.y / SCALE);
    }

    private void cancelPlacement() {
        selectedTowerId = null;
    }

    private void cancelSelection() {
        selectedTowerEntity = null;
    }

    private boolean trySelectTowerAt(int gridX, int gridY) {
        Entity tower = findTowerAt(gridX, gridY);
        if (tower == null) {
            if (selectedTowerEntity == null) {
                return false;
            }
            cancelSelection();
            return true;
        }
        selectedTowerEntity = tower == selectedTowerEntity ? null : tower;
        return true;
    }

    private void tryPlaceSelectedTower() {
        if (waveController.getPhase() != GamePhase.BUILD || !gridMap.isInBounds(hoverTileX, hoverTileY)) {
            return;
        }
        TowerDefinition definition = towerRegistry.get(selectedTowerId);
        if (!TowerPlacementValidator.canPlaceTowerAt(gridMap, hoverTileX, hoverTileY)) {
            return;
        }
        if (!economy.trySpend(definition.cost)) {
            return;
        }
        gridMap.placeTower(hoverTileX, hoverTileY);

        TowerComponent towerComponent = engine.createComponent(TowerComponent.class);
        towerComponent.definition = definition;
        towerComponent.gridX = hoverTileX;
        towerComponent.gridY = hoverTileY;
        towerComponent.totalInvested = definition.cost;

        PositionComponent positionComponent = engine.createComponent(PositionComponent.class);
        positionComponent.value.set(hoverTileX + 0.5f, hoverTileY + 0.5f);

        Entity entity = engine.createEntity();
        towerComponent.stateMachine = new DefaultStateMachine<>(entity, TowerState.IDLE);
        entity.add(towerComponent);
        entity.add(positionComponent);
        engine.addEntity(entity);
    }

    private void buildHud() {
        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(16);
        hudStage.addActor(root);

        root.add(new Label(Localization.get("app.title"), skin, "window")).left().row();

        TextButton menuButton = new TextButton(Localization.get("hud.menu"), skin);
        menuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.pauseGame(GameScreen.this);
                game.setScreen(new MenuScreen(game));
            }
        });
        root.add(menuButton).padTop(12).width(160).left().row();

        goldLabel = new Label("", skin);
        livesLabel = new Label("", skin);
        waveLabel = new Label("", skin);
        root.add(goldLabel).padTop(24).left().row();
        root.add(livesLabel).padTop(4).left().row();
        root.add(waveLabel).padTop(4).left().row();

        boolean firstTowerButton = true;
        for (TowerDefinition towerDefinition : towerRegistry.all()) {
            String towerId = towerDefinition.id;
            TextButton towerButton = new TextButton(Localization.get(towerDefinition.name) + " (" + towerDefinition.cost + "g)", skin);
            towerButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (towerId.equals(selectedTowerId)) {
                        cancelPlacement();
                    } else {
                        selectedTowerId = towerId;
                    }
                }
            });
            Table towerRow = new Table();
            towerRow.add(buildTowerIconStack(towerDefinition)).size(28f).padRight(6f);
            towerRow.add(towerButton).width(200);
            root.add(towerRow).padTop(firstTowerButton ? 24 : 8).left().row();
            towerButtonsById.put(towerId, towerButton);
            firstTowerButton = false;
        }

        root.add().expandY().row();

        nextWaveTimerLabel = new Label("", skin);
        root.add(nextWaveTimerLabel).padBottom(8).left().row();

        Table speedTable = new Table();
        pauseButton = new TextButton(Localization.get("hud.pause"), skin);
        pauseButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                timeScale = 0f;
            }
        });
        normalSpeedButton = new TextButton("1x", skin);
        normalSpeedButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                timeScale = 1f;
            }
        });
        fastSpeedButton = new TextButton("2x", skin);
        fastSpeedButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                timeScale = 2f;
            }
        });
        speedTable.add(pauseButton).width(60).padRight(4);
        speedTable.add(normalSpeedButton).width(60).padRight(4);
        speedTable.add(fastSpeedButton).width(60);
        root.add(speedTable).padBottom(8).left().row();

        startWaveButton = new TextButton(Localization.get("hud.startWave"), skin);
        startWaveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cancelPlacement();
                waveController.startNextWave();
            }
        });
        root.add(startWaveButton).width(200).left();
    }

    private void buildOverlay() {
        overlayWindow = new Window("", skin);
        overlayWindow.setMovable(false);

        overlayMessageLabel = new Label("", skin);
        overlayWindow.add(overlayMessageLabel).pad(20).row();

        TextButton retryButton = new TextButton(Localization.get("overlay.retry"), skin);
        retryButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game));
            }
        });
        overlayWindow.add(retryButton).width(160).padBottom(16);

        overlayWindow.pack();
        overlayWindow.setVisible(false);
        overlayWindow.setTouchable(Touchable.disabled);
        overlayStage.addActor(overlayWindow);
    }

    private Stack buildTowerIconStack(TowerDefinition definition) {
        Image base = new Image();
        Image turret = new Image();
        SpriteSheet sheet = towerSpriteSheetsById.get(definition.spriteSheetId);
        if (sheet != null) {
            base.setDrawable(new TextureRegionDrawable(sheet.getAnimation("base")[0]));
            turret.setDrawable(new TextureRegionDrawable(sheet.getAnimation("turret_idle")[0]));
        }
        return new Stack(base, turret);
    }

    private void buildTowerInfoWindow() {
        towerInfoWindow = new Window("", skin);
        towerInfoWindow.setMovable(false);
        // Window sizes its title bar (and thus the background behind it) to getPadTop(), which by
        // default is only tall enough for the title font - widen it so the 28px icon fits inside.
        towerInfoWindow.padTop(40f);

        towerInfoIconBase = new Image();
        towerInfoIconTurret = new Image();
        Stack towerIconStack = new Stack(towerInfoIconBase, towerInfoIconTurret);
        Table titleTable = towerInfoWindow.getTitleTable();
        titleTable.clearChildren();
        titleTable.add(towerIconStack).size(28f).padRight(6f);
        titleTable.add(towerInfoWindow.getTitleLabel()).growX().minWidth(0);

        towerInfoLevelLabel = new Label("", skin);
        towerInfoDamageLabel = new Label("", skin);
        towerInfoRangeLabel = new Label("", skin);
        towerInfoFireRateLabel = new Label("", skin);
        towerInfoNextLevelLabel = new Label("", skin);
        towerInfoWindow.add(towerInfoLevelLabel).left().pad(4).row();
        towerInfoWindow.add(towerInfoDamageLabel).left().pad(4).row();
        towerInfoWindow.add(towerInfoRangeLabel).left().pad(4).row();
        towerInfoWindow.add(towerInfoFireRateLabel).left().pad(4).row();
        towerInfoWindow.add(towerInfoNextLevelLabel).left().pad(4).row();

        towerUpgradeButton = new TextButton("", skin);
        towerUpgradeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                upgradeSelectedTower();
            }
        });
        towerInfoWindow.add(towerUpgradeButton).width(180).pad(4).row();

        towerSellButton = new TextButton("", skin);
        towerSellButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sellSelectedTower();
            }
        });
        towerInfoWindow.add(towerSellButton).width(180).pad(4);

        towerInfoWindow.pack();
        towerInfoWindow.setVisible(false);
        towerInfoWindow.setTouchable(Touchable.disabled);
        overlayStage.addActor(towerInfoWindow);
    }

    private void upgradeSelectedTower() {
        TowerComponent tower = Mappers.TOWER.get(selectedTowerEntity);
        if (!TowerUpgrade.canUpgrade(tower.level)) {
            return;
        }
        int cost = TowerUpgrade.upgradeCost(tower.definition);
        if (economy.trySpend(cost)) {
            tower.level++;
            tower.totalInvested += cost;
        }
    }

    private void sellSelectedTower() {
        TowerComponent tower = Mappers.TOWER.get(selectedTowerEntity);
        economy.addGold(TowerUpgrade.sellRefund(tower.totalInvested));
        gridMap.removeTower(tower.gridX, tower.gridY);
        engine.removeEntity(selectedTowerEntity);
        cancelSelection();
    }

    private void showOverlay(String title, String message) {
        overlayWindow.getTitleLabel().setText(title);
        overlayMessageLabel.setText(message);
        overlayWindow.pack();
        overlayWindow.setPosition(
            (overlayStage.getViewport().getWorldWidth() - overlayWindow.getWidth()) / 2f,
            (overlayStage.getViewport().getWorldHeight() - overlayWindow.getHeight()) / 2f
        );
        overlayWindow.setVisible(true);
        overlayWindow.setTouchable(Touchable.enabled);
    }

    private void updateHud() {
        goldLabel.setText(Localization.format("hud.gold", economy.getGold()));
        livesLabel.setText(Localization.format("hud.lives", economy.getLives()));
        waveLabel.setText(Localization.format("hud.wave", waveController.getCurrentWaveNumber(), waveController.getTotalWaveCount()));

        boolean buildPhase = waveController.getPhase() == GamePhase.BUILD;
        startWaveButton.setDisabled(!waveController.canStartNextWave());
        for (TextButton towerButton : towerButtonsById.values()) {
            towerButton.setDisabled(!buildPhase);
        }
        if (!buildPhase) {
            selectedTowerId = null;
            selectedTowerEntity = null;
        } else {
            timeScale = 1f;
        }

        pauseButton.setDisabled(buildPhase || timeScale == 0f);
        normalSpeedButton.setDisabled(buildPhase || timeScale == 1f);
        fastSpeedButton.setDisabled(buildPhase || timeScale == 2f);

        nextWaveTimerLabel.setText(
            waveController.isBuildPhaseTimerActive()
                ? Localization.format("hud.nextWaveIn", waveController.getBuildPhaseSecondsRemaining())
                : ""
        );
    }

    private void updateTowerInfoWindow() {
        if (selectedTowerEntity == null) {
            towerInfoWindow.setVisible(false);
            towerInfoWindow.setTouchable(Touchable.disabled);
            return;
        }

        TowerComponent tower = Mappers.TOWER.get(selectedTowerEntity);
        TowerDefinition definition = tower.definition;

        SpriteSheet iconSheet = towerSpriteSheetsById.get(definition.spriteSheetId);
        if (iconSheet != null) {
            towerInfoIconBase.setDrawable(new TextureRegionDrawable(iconSheet.getAnimation("base")[0]));
            towerInfoIconTurret.setDrawable(new TextureRegionDrawable(iconSheet.getAnimation("turret_idle")[0]));
        }

        towerInfoWindow.getTitleLabel().setText(Localization.get(definition.name));
        towerInfoLevelLabel.setText(Localization.format("tower.info.level", tower.level, TowerUpgrade.MAX_LEVEL));
        towerInfoDamageLabel.setText(Localization.format("tower.info.damage",
            (int) TowerUpgrade.damageForLevel(definition, tower.level)));
        towerInfoRangeLabel.setText(Localization.format("tower.info.range",
            TowerUpgrade.rangeForLevel(definition, tower.level)));
        towerInfoFireRateLabel.setText(Localization.format("tower.info.fireRate",
            TowerUpgrade.fireRateForLevel(definition, tower.level)));

        if (TowerUpgrade.canUpgrade(tower.level)) {
            int upgradeCost = TowerUpgrade.upgradeCost(definition);
            towerUpgradeButton.setText(Localization.format("tower.info.upgrade", upgradeCost));
            towerUpgradeButton.setDisabled(economy.getGold() < upgradeCost);
            towerInfoNextLevelLabel.setText(Localization.get(nextLevelPreviewKey(tower.level + 1)));
        } else {
            towerUpgradeButton.setText(Localization.get("tower.info.upgrade.maxed"));
            towerUpgradeButton.setDisabled(true);
            towerInfoNextLevelLabel.setText("");
        }
        towerSellButton.setText(Localization.format("tower.info.sell", TowerUpgrade.sellRefund(tower.totalInvested)));

        towerInfoWindow.pack();
        positionTowerInfoWindow();
        towerInfoWindow.setVisible(true);
        towerInfoWindow.setTouchable(Touchable.enabled);
    }

    private String nextLevelPreviewKey(int nextLevel) {
        TowerUpgrade.MilestoneStat milestone = TowerUpgrade.milestoneAtLevel(nextLevel);
        if (milestone == null) {
            return "tower.info.nextLevel.normal";
        }
        return switch (milestone) {
            case DAMAGE -> "tower.info.nextLevel.damage";
            case RANGE -> "tower.info.nextLevel.range";
            case FIRE_RATE -> "tower.info.nextLevel.fireRate";
        };
    }

    private void positionTowerInfoWindow() {
        Vector2 worldPoint = Mappers.POSITION.get(selectedTowerEntity).value.cpy();
        // PositionComponent is in tile units, but mapCamera/mapViewport operate in pixels (see
        // renderEntities' px = position.value.x * SCALE) - convert before projecting.
        worldPoint.scl(SCALE);
        worldPoint.x += SCALE * 0.7f;
        Vector2 screenPoint = mapViewport.project(worldPoint);
        // Viewport#project returns GL-style coords (origin bottom-left, y-up); screenToStageCoordinates
        // expects touch/screen coords (origin top-left, y-down), so flip y before converting.
        screenPoint.y = Gdx.graphics.getHeight() - screenPoint.y;
        Vector2 stagePoint = overlayStage.screenToStageCoordinates(screenPoint);

        float x = MathUtils.clamp(stagePoint.x,
            0f, overlayStage.getWidth() - towerInfoWindow.getWidth());
        float y = MathUtils.clamp(stagePoint.y - towerInfoWindow.getHeight() / 2f,
            0f, overlayStage.getHeight() - towerInfoWindow.getHeight());
        towerInfoWindow.setPosition(x, y);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public void render(float delta) {
        if (!gameEnded) {
            float simDelta = waveController.getPhase() == GamePhase.WAVE ? delta * timeScale : delta;
            waveController.update(simDelta);
            engine.update(simDelta);

            if (economy.isGameOver()) {
                gameEnded = true;
                showOverlay(Localization.get("overlay.gameOver.title"), Localization.get("overlay.gameOver.message"));
            } else if (waveController.allWavesCleared()) {
                gameEnded = true;
                showOverlay(Localization.get("overlay.win.title"), Localization.get("overlay.win.message"));
            }
        }

        ScreenUtils.clear(0.05f, 0.05f, 0.07f, 1f);

        mapViewport.apply();
        mapCamera.update();
        mapRenderer.setView(mapCamera);
        mapRenderer.render();
        renderEntities();

        updateHud();
        hudStage.act(delta);
        hudStage.getViewport().apply();
        hudStage.draw();

        updateTowerInfoWindow();
        overlayStage.act(delta);
        overlayStage.getViewport().apply();
        overlayStage.draw();
    }

    private void renderEntities() {
        Gdx.gl.glEnable(GL20.GL_BLEND);

        ImmutableArray<Entity> towerEntities = engine.getEntitiesFor(TOWER_FAMILY);
        ImmutableArray<Entity> enemyEntities = engine.getEntitiesFor(ENEMY_FAMILY);
        ImmutableArray<Entity> projectileEntities = engine.getEntitiesFor(PROJECTILE_FAMILY);

        spriteBatch.setProjectionMatrix(mapCamera.combined);
        spriteBatch.begin();
        for (Entity entity : towerEntities) {
            drawTowerSprite(Mappers.TOWER.get(entity), Mappers.POSITION.get(entity));
        }
        for (Entity entity : enemyEntities) {
            drawEnemySprite(Mappers.ENEMY.get(entity), Mappers.POSITION.get(entity));
        }
        for (Entity entity : projectileEntities) {
            drawProjectileSprite(Mappers.PROJECTILE.get(entity), Mappers.POSITION.get(entity));
        }
        if (selectedTowerId != null && gridMap.isInBounds(hoverTileX, hoverTileY)) {
            drawGhostSprite();
        }
        spriteBatch.end();

        shapeRenderer.setProjectionMatrix(mapCamera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Entity entity : enemyEntities) {
            drawEnemyHpBar(Mappers.ENEMY.get(entity), Mappers.POSITION.get(entity));
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        if (selectedTowerId != null && gridMap.isInBounds(hoverTileX, hoverTileY)) {
            drawRangeCircle(hoverTileX + 0.5f, hoverTileY + 0.5f, towerRegistry.get(selectedTowerId).rangeTiles);
        } else if (selectedTowerEntity != null) {
            Vector2 selectedPosition = Mappers.POSITION.get(selectedTowerEntity).value;
            TowerComponent selectedTower = Mappers.TOWER.get(selectedTowerEntity);
            drawRangeCircle(selectedPosition.x, selectedPosition.y,
                TowerUpgrade.rangeForLevel(selectedTower.definition, selectedTower.level));
        } else {
            Entity hovered = findTowerAt(hoverTileX, hoverTileY);
            if (hovered != null) {
                Vector2 hoveredPosition = Mappers.POSITION.get(hovered).value;
                TowerComponent hoveredTower = Mappers.TOWER.get(hovered);
                drawRangeCircle(hoveredPosition.x, hoveredPosition.y,
                    TowerUpgrade.rangeForLevel(hoveredTower.definition, hoveredTower.level));
            }
        }
        shapeRenderer.end();
    }

    private void drawTowerSprite(TowerComponent tower, PositionComponent position) {
        SpriteSheet sheet = towerSpriteSheetsById.get(tower.definition.spriteSheetId);
        if (sheet == null) {
            return;
        }
        float px = position.value.x * SCALE;
        float py = position.value.y * SCALE;

        TextureRegion base = sheet.getAnimation("base")[0];
        spriteBatch.draw(base, px - SCALE / 2f, py - SCALE / 2f, SCALE, SCALE);

        TextureRegion[] turretFrames;
        int turretFrameIndex;
        float rotation = tower.turretAngleDeg - 90f + tower.definition.spriteRotationOffsetDeg;
        TowerState state = tower.stateMachine.getCurrentState();
        if (state == TowerState.FIRING) {
            turretFrames = sheet.getAnimation("turret_shoot");
            float shootProgress = MathUtils.clamp(
                tower.timeSinceLastShot / tower.definition.shootAnimationDurationSeconds, 0f, 1f);
            turretFrameIndex = MathUtils.clamp((int) (shootProgress * turretFrames.length), 0, turretFrames.length - 1);
        } else {
            turretFrames = sheet.getAnimation("turret_idle");
            // Only cycle the idle sway animation during true idle spin; while actively tracking a
            // target between shots, hold a static pose so tracking rotation reads as one continuous motion.
            turretFrameIndex = state == TowerState.TRACKING
                ? 0
                : (int) (tower.timeSinceLastShot / TOWER_IDLE_FRAME_DURATION_SECONDS) % turretFrames.length;
        }
        float originX = sheet.getPivotX() * GameConstants.TILE_SCALE;
        float originY = sheet.getPivotY() * GameConstants.TILE_SCALE;
        float turretPy = py + tower.definition.turretVerticalOffsetTiles * SCALE;
        spriteBatch.draw(turretFrames[turretFrameIndex], px - originX, turretPy - originY, originX, originY, SCALE, SCALE, 1f, 1f, rotation);
    }

    private void drawEnemySprite(EnemyComponent enemy, PositionComponent position) {
        SpriteSheet sheet = enemySpriteSheetsById.get(enemy.definition.spriteSheetId);
        if (sheet == null) {
            return;
        }
        boolean dying = enemy.stateMachine.getCurrentState() == EnemyState.DYING;
        TextureRegion[] frames = dying ? sheet.getAnimation("death") : sheet.getAnimation("walk_" + enemy.facingDirection);
        int frameIndex = dying
            ? MathUtils.clamp((int) (enemy.deathTimer / ENEMY_FRAME_DURATION_SECONDS), 0, frames.length - 1)
            : (int) (enemy.animationTime / ENEMY_FRAME_DURATION_SECONDS) % frames.length;
        TextureRegion frame = frames[frameIndex];

        float px = position.value.x * SCALE;
        float py = position.value.y * SCALE;
        spriteBatch.draw(frame, px - SCALE / 2f, py - SCALE / 2f, SCALE, SCALE);
    }

    private void drawEnemyHpBar(EnemyComponent enemy, PositionComponent position) {
        if (enemy.stateMachine.getCurrentState() == EnemyState.DYING) {
            return;
        }
        float px = position.value.x * SCALE;
        float py = position.value.y * SCALE;

        float hpRatio = Math.max(0f, enemy.hp / enemy.maxHp);
        float barWidth = SCALE * 0.6f;
        float barY = py + SCALE * 0.4f;
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(px - barWidth / 2f, barY, barWidth, 4f);
        shapeRenderer.setColor(Color.LIME);
        shapeRenderer.rect(px - barWidth / 2f, barY, barWidth * hpRatio, 4f);
    }

    private void drawProjectileSprite(ProjectileComponent projectile, PositionComponent position) {
        SpriteSheet sheet = towerSpriteSheetsById.get(projectile.spriteSheetId);
        if (sheet == null) {
            return;
        }
        float px = position.value.x * SCALE;
        float py = position.value.y * SCALE;

        boolean isImpacting = projectile.impactTimer >= 0f;
        if (isImpacting) {
            TextureRegion[] frames = sheet.getAnimation("projectile_impact");
            float impactProgress = MathUtils.clamp(projectile.impactTimer / projectile.impactAnimationDuration, 0f, 1f);
            int frameIndex = MathUtils.clamp((int) (impactProgress * frames.length), 0, frames.length - 1);
            spriteBatch.draw(frames[frameIndex], px - SCALE / 2f, py - SCALE / 2f, SCALE, SCALE);
        } else {
            TextureRegion[] frames = sheet.getAnimation("projectile_fly");
            float flightElapsedSeconds = projectile.totalTime - Math.max(0f, projectile.timeToImpact);
            int frameIndex = (int) (flightElapsedSeconds / PROJECTILE_FRAME_DURATION_SECONDS) % frames.length;
            float originX = SCALE / 2f;
            float originY = SCALE / 2f;
            spriteBatch.draw(frames[frameIndex], px - originX, py - originY, originX, originY, SCALE, SCALE, 1f, 1f, projectile.travelAngleDeg);
        }
    }

    private void drawGhostSprite() {
        TowerDefinition definition = towerRegistry.get(selectedTowerId);
        SpriteSheet sheet = towerSpriteSheetsById.get(definition.spriteSheetId);
        if (sheet == null) {
            return;
        }
        boolean valid = TowerPlacementValidator.canPlaceTowerAt(gridMap, hoverTileX, hoverTileY)
            && economy.getGold() >= definition.cost;

        float px = (hoverTileX + 0.5f) * SCALE;
        float py = (hoverTileY + 0.5f) * SCALE;

        spriteBatch.setColor(valid ? 0f : 1f, valid ? 1f : 0f, 0f, 0.55f);
        TextureRegion base = sheet.getAnimation("base")[0];
        spriteBatch.draw(base, px - SCALE / 2f, py - SCALE / 2f, SCALE, SCALE);

        TextureRegion turret = sheet.getAnimation("turret_idle")[0];
        float originX = sheet.getPivotX() * GameConstants.TILE_SCALE;
        float originY = sheet.getPivotY() * GameConstants.TILE_SCALE;
        float turretPy = py + definition.turretVerticalOffsetTiles * SCALE;
        spriteBatch.draw(turret, px - originX, turretPy - originY, originX, originY, SCALE, SCALE, 1f, 1f, definition.spriteRotationOffsetDeg);
        spriteBatch.setColor(Color.WHITE);
    }

    private void drawRangeCircle(float centerTileX, float centerTileY, float radiusTiles) {
        shapeRenderer.circle(centerTileX * SCALE, centerTileY * SCALE, radiusTiles * SCALE, 64);
    }

    private Entity findTowerAt(int gridX, int gridY) {
        for (Entity entity : engine.getEntitiesFor(TOWER_FAMILY)) {
            TowerComponent tower = Mappers.TOWER.get(entity);
            if (tower.gridX == gridX && tower.gridY == gridY) {
                return entity;
            }
        }
        return null;
    }

    @Override
    public void resize(int width, int height) {
        // Viewport#update() resets screenBounds to (0, 0, w, h) internally, so setScreenBounds()
        // must run AFTER update() for docked/offset viewports, not before.
        int mapAreaWidth = Math.max(1, width - GameConstants.HUD_WIDTH_PX);

        mapViewport.update(mapAreaWidth, height, true);
        mapViewport.setScreenBounds(0, 0, mapAreaWidth, height);
        mapCamera.position.set(
            GameConstants.MAP_VIEWPORT_WIDTH_PX / 2f,
            GameConstants.MAP_VIEWPORT_HEIGHT_PX / 2f,
            0
        );

        hudStage.getViewport().update(GameConstants.HUD_WIDTH_PX, height, true);
        hudStage.getViewport().setScreenBounds(mapAreaWidth, 0, GameConstants.HUD_WIDTH_PX, height);

        overlayStage.getViewport().update(width, height, true);
        overlayStage.getViewport().setScreenBounds(0, 0, width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        spriteBatch.dispose();
        for (SpriteSheet sheet : enemySpriteSheetsById.values()) {
            sheet.dispose();
        }
        for (SpriteSheet sheet : towerSpriteSheetsById.values()) {
            sheet.dispose();
        }
        mapRenderer.dispose();
        tiledMap.dispose();
        hudStage.dispose();
        overlayStage.dispose();
        skin.dispose();
    }
}
