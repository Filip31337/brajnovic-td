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
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import hr.brajnovic.td.BrajnovicTD;
import hr.brajnovic.td.GameConstants;
import hr.brajnovic.td.ecs.Mappers;
import hr.brajnovic.td.ecs.PositionComponent;
import hr.brajnovic.td.economy.Economy;
import hr.brajnovic.td.enemy.ActiveEffect;
import hr.brajnovic.td.enemy.BossBehaviorSystem;
import hr.brajnovic.td.enemy.EffectType;
import hr.brajnovic.td.enemy.EnemyComponent;
import hr.brajnovic.td.enemy.EnemyLifecycleSystem;
import hr.brajnovic.td.enemy.EnemyRegistry;
import hr.brajnovic.td.enemy.EnemyState;
import hr.brajnovic.td.enemy.EnemyStatusEffectSystem;
import hr.brajnovic.td.fx.ParticleEffectManager;
import hr.brajnovic.td.i18n.Localization;
import hr.brajnovic.td.map.GridMap;
import hr.brajnovic.td.map.LevelDefinition;
import hr.brajnovic.td.map.LevelLoader;
import hr.brajnovic.td.input.InputMode;
import hr.brajnovic.td.input.InputSettings;
import hr.brajnovic.td.render.SpriteSheet;
import hr.brajnovic.td.sound.SoundManager;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameScreen implements Screen {

    private static final int SCALE = GameConstants.SCALED_TILE_SIZE_PX;
    private static final float ENEMY_FRAME_DURATION_SECONDS = 0.12f;
    private static final float TOWER_IDLE_FRAME_DURATION_SECONDS = 0.5f;
    private static final float PROJECTILE_FRAME_DURATION_SECONDS = 0.08f;

    // rgb = tint target color for enemy_status_tint shader; a is unused here (blend strength is set per-enemy).
    private static final Color ICE_TINT_COLOR = new Color(0.6f, 0.85f, 1f, 1f);
    private static final Color POISON_TINT_COLOR = new Color(0.45f, 0.9f, 0.35f, 1f);
    private static final Color BOSS_TINT_COLOR = new Color(0.9f, 0.25f, 0.2f, 1f);
    private static final float STATUS_TINT_STRENGTH = 0.35f;
    private static final float COMBINED_STATUS_TINT_STRENGTH = 0.45f;
    private static final Color NO_STATUS_TINT = new Color(1f, 1f, 1f, 0f);
    private static final float HIT_FLASH_PEAK_STRENGTH = 0.375f;

    // No boss art yet - reused orc_atlas rendered bigger + permanently red-tinted (see computeStatusTintColor).
    private static final float BOSS_VISUAL_SCALE = 1.6f;

    private static final float OVERLAY_FADE_IN_SECONDS = 0.4f;
    private static final float OVERLAY_SCALE_IN_START = 0.85f;
    private static final float PANEL_FADE_IN_SECONDS = 0.2f;
    private static final float HUD_PUNCH_SCALE = 1.35f;
    private static final float HUD_PUNCH_DURATION_SECONDS = 0.25f;
    private static final float WAVE_BANNER_FADE_IN_SECONDS = 0.3f;
    private static final float WAVE_BANNER_HOLD_SECONDS = 1f;
    private static final float WAVE_BANNER_FADE_OUT_SECONDS = 0.5f;

    // Real edge-detect outline shader for the selected/hovered tower (see tower_outline.frag): a single
    // shader pass reads the sprite's own silhouette, so it stays a clean ring even for a rotated turret -
    // the earlier multi-draw offset trick fanned rotated art out into visible ghost copies instead.
    private static final Color TOWER_OUTLINE_COLOR = new Color(1f, 1f, 1f, 1f);
    // Matches the shader's max sample radius (2 texels, see tower_outline.frag) - each outline frame
    // texture gets a real transparent border this wide so the shader never needs to sample beyond it.
    private static final int OUTLINE_FRAME_PADDING_PX = 2;

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
    private final ShaderProgram enemyStatusTintShader;
    private final ShaderProgram towerOutlineShader;
    private final ParticleEffectManager particleEffectManager;
    private final Texture confirmIconTexture;
    private final Texture cancelIconTexture;
    private final Texture placementPanelTexture;
    private final Color statusTintScratch = new Color();
    private final Map<String, SpriteSheet> enemySpriteSheetsById = new HashMap<>();
    private final Map<String, SpriteSheet> towerSpriteSheetsById = new HashMap<>();
    private final Map<String, TextureRegion> outlineBaseFrameById = new HashMap<>();
    private final Map<String, TextureRegion[]> outlineTurretIdleFramesById = new HashMap<>();
    private final Map<String, TextureRegion[]> outlineTurretShootFramesById = new HashMap<>();
    private final List<Texture> outlineFrameTextures = new ArrayList<>();

    private final Stage hudStage;
    private final Stage overlayStage;
    private final Skin skin;
    private final InputMultiplexer inputMultiplexer;

    private Label goldLabel;
    private Label livesLabel;
    private int lastDisplayedGold = Integer.MIN_VALUE;
    private int lastDisplayedLives = Integer.MIN_VALUE;
    private Label waveLabel;
    private Label nextWaveTimerLabel;
    private TextButton startWaveButton;
    private Window placementConfirmWindow;
    private Label waveBannerLabel;
    private GamePhase lastPhase = GamePhase.BUILD;
    private final Map<String, TextButton> towerButtonsById = new LinkedHashMap<>();
    private TextButton pauseButton;
    private TextButton normalSpeedButton;
    private TextButton fastSpeedButton;
    private float timeScale = 1f;

    private Window overlayWindow;
    private Label overlayMessageLabel;
    private TextButton overlayRetryButton;
    private TextButton overlayNextLevelButton;
    private Table overlayButtonRow;
    private boolean gameEnded = false;

    private Window towerInfoWindow;
    private Image towerInfoIconBase;
    private Image towerInfoIconTurret;
    private Label towerInfoLevelLabel;
    private Label towerInfoDamageLabel;
    private Label towerInfoRangeLabel;
    private Label towerInfoFireRateLabel;
    private Label towerInfoPoisonLabel;
    private Label towerInfoSlowLabel;
    private Table towerInfoEffectsTable;
    private Label towerInfoNextLevelLabel;
    private TextButton towerUpgradeButton;
    private TextButton towerSellButton;

    private String selectedTowerId = null;
    private Entity selectedTowerEntity = null;
    private int hoverTileX = Integer.MIN_VALUE;
    private int hoverTileY = Integer.MIN_VALUE;

    public GameScreen(BrajnovicTD game, String levelId) {
        this.game = game;

        towerRegistry = TowerRegistry.loadFromInternal("data/towers.json");
        enemyRegistry = EnemyRegistry.loadFromInternal("data/enemies.json");
        level = LevelLoader.loadFromInternal("data/levels/" + levelId + ".json");

        tiledMap = new TmxMapLoader().load(level.tmxPath);
        gridMap = GridMap.fromTiledMap(tiledMap);
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, GameConstants.TILE_SCALE);

        economy = new Economy(level.startingGold, level.startingLives);
        particleEffectManager = new ParticleEffectManager();
        engine = new PooledEngine();
        engine.addSystem(new BossBehaviorSystem());
        engine.addSystem(new EnemyStatusEffectSystem());
        engine.addSystem(new EnemyLifecycleSystem(economy, particleEffectManager));
        engine.addSystem(new TowerTargetingSystem());
        engine.addSystem(new ProjectileSystem(particleEffectManager));
        waveController = new WaveController(level, enemyRegistry, gridMap, economy, engine, particleEffectManager);

        mapCamera = new OrthographicCamera();
        mapViewport = new ExtendViewport(
            GameConstants.MAP_VIEWPORT_WIDTH_PX,
            GameConstants.MAP_VIEWPORT_HEIGHT_PX,
            mapCamera
        );
        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();
        confirmIconTexture = new Texture(Gdx.files.internal("ui/icon_confirm.png"));
        cancelIconTexture = new Texture(Gdx.files.internal("ui/icon_cancel.png"));
        placementPanelTexture = new Texture(Gdx.files.internal("ui/panel_rounded.png"));
        placementPanelTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        enemyStatusTintShader = new ShaderProgram(
            Gdx.files.internal("shaders/enemy_status_tint.vert"),
            Gdx.files.internal("shaders/enemy_status_tint.frag")
        );
        if (!enemyStatusTintShader.isCompiled()) {
            throw new IllegalStateException("enemy_status_tint shader failed to compile: " + enemyStatusTintShader.getLog());
        }
        towerOutlineShader = new ShaderProgram(
            Gdx.files.internal("shaders/enemy_status_tint.vert"),
            Gdx.files.internal("shaders/tower_outline.frag")
        );
        if (!towerOutlineShader.isCompiled()) {
            throw new IllegalStateException("tower_outline shader failed to compile: " + towerOutlineShader.getLog());
        }
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
        buildTowerOutlineFrames();

        skin = SkinFactory.createSkin();
        hudStage = new Stage(new ScreenViewport());
        overlayStage = new Stage(new ScreenViewport());
        buildHud();
        buildOverlay();
        buildTowerInfoWindow();
        buildPlacementConfirmWindow();
        buildWaveBanner();

        inputMultiplexer = new InputMultiplexer(overlayStage, hudStage, mapInputProcessor);

        resize(GameConstants.WINDOW_WIDTH_PX, GameConstants.WINDOW_HEIGHT_PX);
    }

    private final InputAdapter mapInputProcessor = new InputAdapter() {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (InputSettings.getMode() == InputMode.MOUSE && button == Input.Buttons.RIGHT) {
                cancelPlacement();
                cancelSelection();
                return true;
            }
            if (button != Input.Buttons.LEFT) {
                return false;
            }
            updateHoverTile(screenX, screenY);
            if (selectedTowerId != null) {
                // TOUCH confirms via the placementConfirmWindow's checkmark/X instead of this tap
                // (no hover means a tap can only move the ghost preview, not commit to placing it).
                if (InputSettings.getMode() == InputMode.MOUSE) {
                    tryPlaceSelectedTower();
                }
                return true;
            }
            if (waveController.getPhase() == GamePhase.BUILD) {
                return trySelectTowerAt(hoverTileX, hoverTileY);
            }
            return false;
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            if (InputSettings.getMode() == InputMode.MOUSE) {
                updateHoverTile(screenX, screenY);
            }
            return false;
        }

        @Override
        public boolean keyDown(int keycode) {
            if (InputSettings.getMode() == InputMode.MOUSE && keycode == Input.Keys.ESCAPE) {
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
        hoverTileX = Integer.MIN_VALUE;
        hoverTileY = Integer.MIN_VALUE;
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

        // Clear the ghost/confirm-panel target so it doesn't linger on the tile that was just filled;
        // MOUSE mode repopulates it instantly on the next mouseMoved, TOUCH mode waits for the next tap.
        hoverTileX = Integer.MIN_VALUE;
        hoverTileY = Integer.MIN_VALUE;

        if (InputSettings.getMode() == InputMode.TOUCH) {
            // MOUSE keeps selectedTowerId so repeated clicks buy more of the same tower. In TOUCH that
            // same "still selected" state made re-tapping that tower's icon look like a deselect toggle
            // (see the buildHud() tower button listener), so a second placement silently did nothing
            // until the icon was tapped a third time. Deselecting here makes every icon tap unambiguous.
            selectedTowerId = null;
        }
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

        overlayRetryButton = new TextButton(Localization.get("overlay.retry"), skin);
        overlayRetryButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game, level.id));
            }
        });

        overlayNextLevelButton = new TextButton(Localization.get("overlay.nextLevel"), skin);
        overlayNextLevelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game, level.nextLevelId));
            }
        });

        // A dedicated row (rebuilt per showOverlay call) instead of adding both buttons up front - an
        // invisible Cell's actor still reserves its layout space, which made the Win overlay pack() as
        // wide as both buttons side by side even though only one was ever shown.
        overlayButtonRow = new Table();
        overlayWindow.add(overlayButtonRow).padBottom(16).row();

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
        towerInfoPoisonLabel = new Label("", skin);
        towerInfoSlowLabel = new Label("", skin);
        towerInfoEffectsTable = new Table();
        towerInfoNextLevelLabel = new Label("", skin);
        towerInfoWindow.add(towerInfoLevelLabel).left().pad(4).row();
        towerInfoWindow.add(towerInfoDamageLabel).left().pad(4).row();
        towerInfoWindow.add(towerInfoRangeLabel).left().pad(4).row();
        towerInfoWindow.add(towerInfoFireRateLabel).left().pad(4).row();
        // Rebuilt per updateTowerInfoWindow() call instead of adding both labels up front and toggling
        // setVisible() - an invisible Cell still reserves its layout space (same lesson as overlayButtonRow),
        // which would leave a blank gap for towers without a poison/slow mechanic.
        towerInfoWindow.add(towerInfoEffectsTable).left().row();
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

    /** TOUCH-mode-only confirm/cancel panel shown next to the ghost preview tile (replaces the mouse-only
     * right-click/Escape cancel, since touch has neither): green check confirms the placement in-place,
     * red X cancels placement mode without spending gold. */
    private void buildPlacementConfirmWindow() {
        Window.WindowStyle roundedStyle = new Window.WindowStyle(skin.get(Window.WindowStyle.class));
        NinePatch panelPatch = new NinePatch(placementPanelTexture, 14, 14, 14, 14);
        roundedStyle.background = new NinePatchDrawable(panelPatch);

        placementConfirmWindow = new Window("", roundedStyle);
        placementConfirmWindow.setMovable(false);

        ImageButton.ImageButtonStyle confirmStyle = new ImageButton.ImageButtonStyle();
        confirmStyle.imageUp = new TextureRegionDrawable(new TextureRegion(confirmIconTexture));
        ImageButton confirmButton = new ImageButton(confirmStyle);
        confirmButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                tryPlaceSelectedTower();
            }
        });
        placementConfirmWindow.add(confirmButton).size(48f).pad(6f);

        ImageButton.ImageButtonStyle cancelStyle = new ImageButton.ImageButtonStyle();
        cancelStyle.imageUp = new TextureRegionDrawable(new TextureRegion(cancelIconTexture));
        ImageButton cancelIconButton = new ImageButton(cancelStyle);
        cancelIconButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cancelPlacement();
            }
        });
        placementConfirmWindow.add(cancelIconButton).size(48f).pad(6f);

        placementConfirmWindow.pack();
        placementConfirmWindow.setVisible(false);
        placementConfirmWindow.setTouchable(Touchable.disabled);
        overlayStage.addActor(placementConfirmWindow);
    }

    /** Transient wave start/clear announcement. Vertically centered on the full window, but horizontally
     * centered over the map viewport only (MAP_VIEWPORT_WIDTH_PX, not the full window) so it sits in the
     * middle of the play area instead of drifting right into the docked HUD panel. See showWaveBanner()
     * for the fade-in/hold/fade-out sequence and render() for the BUILD<->WAVE transition detection. */
    private void buildWaveBanner() {
        waveBannerLabel = new Label("", skin, "window");
        waveBannerLabel.setAlignment(Align.center);
        waveBannerLabel.setVisible(false);
        waveBannerLabel.setTouchable(Touchable.disabled);
        overlayStage.addActor(waveBannerLabel);
    }

    private void showWaveBanner(String text) {
        waveBannerLabel.setText(text);
        waveBannerLabel.pack();
        waveBannerLabel.setPosition(
            (GameConstants.MAP_VIEWPORT_WIDTH_PX - waveBannerLabel.getWidth()) / 2f,
            (overlayStage.getViewport().getWorldHeight() - waveBannerLabel.getHeight()) / 2f
        );
        waveBannerLabel.setOrigin(Align.center);
        waveBannerLabel.clearActions();
        waveBannerLabel.setColor(1f, 1f, 1f, 0f);
        waveBannerLabel.setVisible(true);
        waveBannerLabel.addAction(Actions.sequence(
            Actions.fadeIn(WAVE_BANNER_FADE_IN_SECONDS, Interpolation.fade),
            Actions.delay(WAVE_BANNER_HOLD_SECONDS),
            Actions.fadeOut(WAVE_BANNER_FADE_OUT_SECONDS, Interpolation.fade),
            Actions.visible(false)
        ));
    }

    private void updatePlacementConfirmWindow() {
        boolean show = InputSettings.getMode() == InputMode.TOUCH
            && selectedTowerId != null
            && gridMap.isInBounds(hoverTileX, hoverTileY);
        boolean wasVisible = placementConfirmWindow.isVisible();
        placementConfirmWindow.setVisible(show);
        placementConfirmWindow.setTouchable(show ? Touchable.enabled : Touchable.disabled);
        if (show) {
            positionWindowNearTile(placementConfirmWindow, hoverTileX + 0.5f, hoverTileY + 0.5f);
            if (!wasVisible) {
                fadeInPanel(placementConfirmWindow);
            }
        }
    }

    /** Fades a just-shown floating panel in from transparent - only called on the invisible-to-visible
     * edge (see the isVisible() check at each call site), never every frame while already shown. */
    private void fadeInPanel(Actor panel) {
        panel.clearActions();
        panel.setColor(1f, 1f, 1f, 0f);
        panel.addAction(Actions.fadeIn(PANEL_FADE_IN_SECONDS));
    }

    /** Origin at left-middle (not center) so the punch doesn't depend on the label's current width,
     * which can be stale by one frame right after setText() changes digit count - height is stable. */
    private void punchLabel(Label label) {
        label.setOrigin(0f, label.getHeight() / 2f);
        label.clearActions();
        label.setScale(1f);
        label.addAction(Actions.sequence(
            Actions.scaleTo(HUD_PUNCH_SCALE, HUD_PUNCH_SCALE, HUD_PUNCH_DURATION_SECONDS / 2f, Interpolation.pow2Out),
            Actions.scaleTo(1f, 1f, HUD_PUNCH_DURATION_SECONDS / 2f, Interpolation.pow2In)
        ));
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
            Vector2 position = Mappers.POSITION.get(selectedTowerEntity).value;
            particleEffectManager.spawn("tower_upgrade", position.x * SCALE, position.y * SCALE);
        }
    }

    private void sellSelectedTower() {
        TowerComponent tower = Mappers.TOWER.get(selectedTowerEntity);
        economy.addGold(TowerUpgrade.sellRefund(tower.totalInvested));
        gridMap.removeTower(tower.gridX, tower.gridY);
        engine.removeEntity(selectedTowerEntity);
        cancelSelection();
    }

    private void showOverlay(String title, String message, boolean nextLevel) {
        overlayWindow.getTitleLabel().setText(title);
        overlayMessageLabel.setText(message);
        overlayButtonRow.clearChildren();
        overlayButtonRow.add(nextLevel ? overlayNextLevelButton : overlayRetryButton).width(160);
        overlayWindow.pack();
        overlayWindow.setPosition(
            (GameConstants.MAP_VIEWPORT_WIDTH_PX - overlayWindow.getWidth()) / 2f,
            (overlayStage.getViewport().getWorldHeight() - overlayWindow.getHeight()) / 2f
        );
        overlayWindow.setOrigin(Align.center);
        overlayWindow.setVisible(true);
        overlayWindow.setTouchable(Touchable.enabled);
        overlayWindow.setColor(1f, 1f, 1f, 0f);
        overlayWindow.setScale(OVERLAY_SCALE_IN_START);
        overlayWindow.addAction(Actions.parallel(
            Actions.fadeIn(OVERLAY_FADE_IN_SECONDS, Interpolation.fade),
            Actions.scaleTo(1f, 1f, OVERLAY_FADE_IN_SECONDS, Interpolation.swingOut)
        ));
    }

    private void updateHud() {
        int currentGold = economy.getGold();
        int currentLives = economy.getLives();
        goldLabel.setText(Localization.format("hud.gold", currentGold));
        livesLabel.setText(Localization.format("hud.lives", currentLives));
        waveLabel.setText(Localization.format("hud.wave", waveController.getCurrentWaveNumber(), waveController.getTotalWaveCount()));

        if (lastDisplayedGold != Integer.MIN_VALUE && currentGold != lastDisplayedGold) {
            punchLabel(goldLabel);
        }
        if (lastDisplayedLives != Integer.MIN_VALUE && currentLives != lastDisplayedLives) {
            punchLabel(livesLabel);
        }
        lastDisplayedGold = currentGold;
        lastDisplayedLives = currentLives;

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
        boolean wasVisible = towerInfoWindow.isVisible();

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
            TowerUpgrade.damageForLevel(definition, tower.level)));
        towerInfoRangeLabel.setText(Localization.format("tower.info.range",
            TowerUpgrade.rangeForLevel(definition, tower.level)));
        towerInfoFireRateLabel.setText(Localization.format("tower.info.fireRate",
            TowerUpgrade.fireRateForLevel(definition, tower.level)));

        towerInfoEffectsTable.clearChildren();
        if (definition.poisonDamagePerSecond > 0f) {
            towerInfoPoisonLabel.setText(Localization.format("tower.info.poison",
                TowerUpgrade.poisonDamagePerSecondForLevel(definition, tower.level)));
            towerInfoEffectsTable.add(towerInfoPoisonLabel).left().pad(4).row();
        }
        if (definition.slowRatio > 0f) {
            towerInfoSlowLabel.setText(Localization.format("tower.info.slow",
                Math.round(TowerUpgrade.slowRatioForLevel(definition, tower.level) * 100)));
            towerInfoEffectsTable.add(towerInfoSlowLabel).left().pad(4).row();
        }

        if (TowerUpgrade.canUpgrade(tower.level)) {
            int upgradeCost = TowerUpgrade.upgradeCost(definition);
            towerUpgradeButton.setText(Localization.format("tower.info.upgrade", upgradeCost));
            towerUpgradeButton.setDisabled(economy.getGold() < upgradeCost);
            String nextLevelPreview = Localization.get(nextLevelPreviewKey(tower.level + 1));
            if (definition.poisonDamagePerSecond > 0f) {
                nextLevelPreview += Localization.get("tower.info.nextLevel.poisonSuffix");
            }
            if (definition.slowRatio > 0f) {
                nextLevelPreview += Localization.get("tower.info.nextLevel.slowSuffix");
            }
            towerInfoNextLevelLabel.setText(nextLevelPreview);
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
        if (!wasVisible) {
            fadeInPanel(towerInfoWindow);
        }
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
        Vector2 tilePosition = Mappers.POSITION.get(selectedTowerEntity).value;
        positionWindowNearTile(towerInfoWindow, tilePosition.x, tilePosition.y);
    }

    /** Positions a floating overlay window next to a tile (world tile-center coordinates), matching the
     * tower info panel's placement: to the right of the tile, vertically centered, clamped to stage bounds. */
    private void positionWindowNearTile(Window window, float tileX, float tileY) {
        Vector2 worldPoint = new Vector2(tileX, tileY);
        // PositionComponent/tile coords are in tile units, but mapCamera/mapViewport operate in pixels
        // (see renderEntities' px = position.value.x * SCALE) - convert before projecting.
        worldPoint.scl(SCALE);
        worldPoint.x += SCALE * 0.7f;
        Vector2 screenPoint = mapViewport.project(worldPoint);
        // Viewport#project returns GL-style coords (origin bottom-left, y-up); screenToStageCoordinates
        // expects touch/screen coords (origin top-left, y-down), so flip y before converting.
        screenPoint.y = Gdx.graphics.getHeight() - screenPoint.y;
        Vector2 stagePoint = overlayStage.screenToStageCoordinates(screenPoint);

        float x = MathUtils.clamp(stagePoint.x,
            0f, overlayStage.getWidth() - window.getWidth());
        float y = MathUtils.clamp(stagePoint.y - window.getHeight() / 2f,
            0f, overlayStage.getHeight() - window.getHeight());
        window.setPosition(x, y);
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

            GamePhase currentPhase = waveController.getPhase();
            if (lastPhase == GamePhase.BUILD && currentPhase == GamePhase.WAVE) {
                showWaveBanner(Localization.format("hud.waveBanner.start", waveController.getCurrentWaveNumber()));
            } else if (lastPhase == GamePhase.WAVE && currentPhase == GamePhase.BUILD && !waveController.allWavesCleared()) {
                showWaveBanner(Localization.format("hud.waveBanner.clear", waveController.getCurrentWaveNumber()));
            }
            lastPhase = currentPhase;

            if (economy.isGameOver()) {
                gameEnded = true;
                showOverlay(Localization.get("overlay.gameOver.title"), Localization.get("overlay.gameOver.message"), false);
            } else if (waveController.allWavesCleared()) {
                gameEnded = true;
                SoundManager.play("game_over");
                if (level.nextLevelId != null) {
                    showOverlay(Localization.get("overlay.levelComplete.title"), Localization.get("overlay.levelComplete.message"), true);
                } else {
                    showOverlay(Localization.get("overlay.win.title"), Localization.get("overlay.win.message"), false);
                }
            }
        }

        ScreenUtils.clear(0.05f, 0.05f, 0.07f, 1f);

        mapViewport.apply();
        mapCamera.update();
        mapRenderer.setView(mapCamera);
        mapRenderer.render();
        renderEntities(delta);

        updateHud();
        hudStage.act(delta);
        hudStage.getViewport().apply();
        hudStage.draw();

        updateTowerInfoWindow();
        updatePlacementConfirmWindow();
        overlayStage.act(delta);
        overlayStage.getViewport().apply();
        overlayStage.draw();
    }

    private void renderEntities(float delta) {
        Gdx.gl.glEnable(GL20.GL_BLEND);

        ImmutableArray<Entity> towerEntities = engine.getEntitiesFor(TOWER_FAMILY);
        ImmutableArray<Entity> enemyEntities = engine.getEntitiesFor(ENEMY_FAMILY);
        ImmutableArray<Entity> projectileEntities = engine.getEntitiesFor(PROJECTILE_FAMILY);

        spriteBatch.setProjectionMatrix(mapCamera.combined);
        spriteBatch.begin();
        Entity highlightedTower = resolveHighlightedTower();
        if (highlightedTower != null) {
            drawTowerOutline(Mappers.TOWER.get(highlightedTower), Mappers.POSITION.get(highlightedTower));
        }
        for (Entity entity : towerEntities) {
            drawTowerSprite(Mappers.TOWER.get(entity), Mappers.POSITION.get(entity));
        }
        spriteBatch.setShader(enemyStatusTintShader);
        for (Entity entity : enemyEntities) {
            EnemyComponent enemy = Mappers.ENEMY.get(entity);
            boolean boss = Mappers.BOSS.has(entity);
            spriteBatch.setColor(computeStatusTintColor(enemy, boss));
            drawEnemySprite(enemy, Mappers.POSITION.get(entity), boss);
        }
        spriteBatch.setColor(Color.WHITE);
        spriteBatch.setShader(null);
        for (Entity entity : projectileEntities) {
            drawProjectileSprite(Mappers.PROJECTILE.get(entity), Mappers.POSITION.get(entity));
        }
        if (selectedTowerId != null && gridMap.isInBounds(hoverTileX, hoverTileY)) {
            drawGhostSprite();
        }
        particleEffectManager.renderAndPrune(spriteBatch, delta);
        spriteBatch.end();

        shapeRenderer.setProjectionMatrix(mapCamera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Entity entity : enemyEntities) {
            drawEnemyHpBar(Mappers.ENEMY.get(entity), Mappers.POSITION.get(entity), Mappers.BOSS.has(entity));
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

    private record TowerFrames(TextureRegion base, TextureRegion turret, float rotation, float originX, float originY) {
    }

    private record TurretFrameSelection(String animationName, int frameIndex, float rotation) {
    }

    /** Picks the turret animation/frame/rotation for the current tower state - shared by drawTowerSprite
     * (reads frames straight off the shared atlas SpriteSheet) and drawTowerOutline (reads the same
     * animation/index off the padded per-frame outline textures instead), so the two never drift apart. */
    private TurretFrameSelection selectTurretFrame(TowerComponent tower, SpriteSheet sheet) {
        float rotation = tower.turretAngleDeg - 90f + tower.definition.spriteRotationOffsetDeg;
        TowerState state = tower.stateMachine.getCurrentState();
        if (state == TowerState.FIRING) {
            TextureRegion[] turretFrames = sheet.getAnimation("turret_shoot");
            float shootProgress = MathUtils.clamp(
                tower.timeSinceLastShot / tower.definition.shootAnimationDurationSeconds, 0f, 1f);
            int frameIndex = MathUtils.clamp((int) (shootProgress * turretFrames.length), 0, turretFrames.length - 1);
            return new TurretFrameSelection("turret_shoot", frameIndex, rotation);
        }
        TextureRegion[] idleFrames = sheet.getAnimation("turret_idle");
        // Only cycle the idle sway animation during true idle spin; while actively tracking a
        // target between shots, hold a static pose so tracking rotation reads as one continuous motion.
        int frameIndex = state == TowerState.TRACKING
            ? 0
            : (int) (tower.timeSinceLastShot / TOWER_IDLE_FRAME_DURATION_SECONDS) % idleFrames.length;
        return new TurretFrameSelection("turret_idle", frameIndex, rotation);
    }

    private TowerFrames resolveTowerFrames(TowerComponent tower, SpriteSheet sheet) {
        TextureRegion base = sheet.getAnimation("base")[0];
        TurretFrameSelection selection = selectTurretFrame(tower, sheet);
        TextureRegion turret = sheet.getAnimation(selection.animationName())[selection.frameIndex()];
        float originX = sheet.getPivotX() * GameConstants.TILE_SCALE;
        float originY = sheet.getPivotY() * GameConstants.TILE_SCALE;
        return new TowerFrames(base, turret, selection.rotation(), originX, originY);
    }

    private void drawTowerSprite(TowerComponent tower, PositionComponent position) {
        SpriteSheet sheet = towerSpriteSheetsById.get(tower.definition.spriteSheetId);
        if (sheet == null) {
            return;
        }
        TowerFrames frames = resolveTowerFrames(tower, sheet);
        float px = position.value.x * SCALE;
        float py = position.value.y * SCALE;
        float turretPy = py + tower.definition.turretVerticalOffsetTiles * SCALE;

        spriteBatch.draw(frames.base(), px - SCALE / 2f, py - SCALE / 2f, SCALE, SCALE);
        spriteBatch.draw(frames.turret(), px - frames.originX(), turretPy - frames.originY(),
            frames.originX(), frames.originY(), SCALE, SCALE, 1f, 1f, frames.rotation());
    }

    /** Edge-detect outline pass for the selected/hovered tower's base+turret frames (see tower_outline.frag).
     * Reads from {@link #outlineBaseFrameById}/{@link #outlineTurretIdleFramesById}/{@link #outlineTurretShootFramesById}
     * - standalone padded-per-frame textures built once in {@link #buildTowerOutlineFrames()} - instead of the
     * shared atlas: sprites-src frames are packed edge-to-edge with zero gap (see tower_atlas.json), so sampling
     * a couple of texels past a frame's own rect on the shared atlas always bleeds into whatever's packed next
     * to it there; a real 2px transparent border baked into an isolated texture per frame has no such neighbor. */
    private void drawTowerOutline(TowerComponent tower, PositionComponent position) {
        String sheetId = tower.definition.spriteSheetId;
        SpriteSheet sheet = towerSpriteSheetsById.get(sheetId);
        TextureRegion outlineBase = outlineBaseFrameById.get(sheetId);
        if (sheet == null || outlineBase == null) {
            return;
        }
        TurretFrameSelection selection = selectTurretFrame(tower, sheet);
        TextureRegion[] outlineTurretFrames = "turret_shoot".equals(selection.animationName())
            ? outlineTurretShootFramesById.get(sheetId)
            : outlineTurretIdleFramesById.get(sheetId);
        TextureRegion outlineTurret = outlineTurretFrames[selection.frameIndex()];

        float originX = sheet.getPivotX() * GameConstants.TILE_SCALE;
        float originY = sheet.getPivotY() * GameConstants.TILE_SCALE;
        float px = position.value.x * SCALE;
        float py = position.value.y * SCALE;
        float turretPy = py + tower.definition.turretVerticalOffsetTiles * SCALE;
        float padScaled = OUTLINE_FRAME_PADDING_PX * GameConstants.TILE_SCALE;
        float paddedSize = SCALE + 2f * padScaled;

        spriteBatch.setShader(towerOutlineShader);
        towerOutlineShader.setUniformf("u_outlineColor", TOWER_OUTLINE_COLOR);
        // Each outline texture is its own isolated Texture (no neighboring frame packed onto it), so
        // clamping to its full [0,1] extent is always safe - no per-frame UV math needed here anymore.
        towerOutlineShader.setUniformf("u_frameBounds", 0f, 0f, 1f, 1f);

        towerOutlineShader.setUniformf("u_texelSize", 1f / outlineBase.getTexture().getWidth(), 1f / outlineBase.getTexture().getHeight());
        spriteBatch.draw(outlineBase, px - SCALE / 2f - padScaled, py - SCALE / 2f - padScaled, paddedSize, paddedSize);
        // Flush now, while the base frame's texelSize uniform is still bound - SpriteBatch defers the
        // actual GL draw call until flush()/shader-swap, so without this the turret's texelSize (set right
        // below) would silently apply to this still-queued base quad too.
        spriteBatch.flush();

        towerOutlineShader.setUniformf("u_texelSize", 1f / outlineTurret.getTexture().getWidth(), 1f / outlineTurret.getTexture().getHeight());
        float originXPadded = originX + padScaled;
        float originYPadded = originY + padScaled;
        spriteBatch.draw(outlineTurret, px - originXPadded, turretPy - originYPadded,
            originXPadded, originYPadded, paddedSize, paddedSize, 1f, 1f, selection.rotation());
        spriteBatch.flush();

        spriteBatch.setShader(null);
    }

    /** Builds a standalone, individually-padded Texture per base/turret_idle/turret_shoot frame of every
     * registered tower type, for {@link #drawTowerOutline(TowerComponent, PositionComponent)} to sample
     * without risking bleed from a tightly-packed neighboring frame on the real atlas. One-time cost at
     * screen construction; the textures are disposed alongside everything else in {@link #dispose()}. */
    private void buildTowerOutlineFrames() {
        for (TowerDefinition definition : towerRegistry.all()) {
            SpriteSheet sheet = towerSpriteSheetsById.get(definition.spriteSheetId);
            if (sheet == null || outlineBaseFrameById.containsKey(definition.spriteSheetId)) {
                continue;
            }
            Pixmap sourcePixmap = new Pixmap(Gdx.files.internal("sprites-src/" + definition.spriteSheetId + ".png"));
            outlineBaseFrameById.put(definition.spriteSheetId, buildPaddedOutlineFrame(sourcePixmap, sheet.getAnimation("base")[0]));
            outlineTurretIdleFramesById.put(definition.spriteSheetId, buildPaddedOutlineFrames(sourcePixmap, sheet.getAnimation("turret_idle")));
            outlineTurretShootFramesById.put(definition.spriteSheetId, buildPaddedOutlineFrames(sourcePixmap, sheet.getAnimation("turret_shoot")));
            sourcePixmap.dispose();
        }
    }

    private TextureRegion[] buildPaddedOutlineFrames(Pixmap sourcePixmap, TextureRegion[] frames) {
        TextureRegion[] padded = new TextureRegion[frames.length];
        for (int i = 0; i < frames.length; i++) {
            padded[i] = buildPaddedOutlineFrame(sourcePixmap, frames[i]);
        }
        return padded;
    }

    private TextureRegion buildPaddedOutlineFrame(Pixmap sourcePixmap, TextureRegion source) {
        int width = source.getRegionWidth();
        int height = source.getRegionHeight();
        int pad = OUTLINE_FRAME_PADDING_PX;
        Pixmap padded = new Pixmap(width + pad * 2, height + pad * 2, Pixmap.Format.RGBA8888);
        padded.setBlending(Pixmap.Blending.None);
        padded.drawPixmap(sourcePixmap, source.getRegionX(), source.getRegionY(), width, height, pad, pad, width, height);
        Texture texture = new Texture(padded);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        padded.dispose();
        outlineFrameTextures.add(texture);
        return new TextureRegion(texture);
    }

    /** The existing placed tower that should show a selection/hover outline: selectedTowerEntity takes
     * priority, otherwise whatever's under the cursor - null during ghost placement mode (selectedTowerId
     * != null), since the ghost preview already has its own green/red tint feedback. */
    private Entity resolveHighlightedTower() {
        if (selectedTowerId != null) {
            return null;
        }
        if (selectedTowerEntity != null) {
            return selectedTowerEntity;
        }
        return findTowerAt(hoverTileX, hoverTileY);
    }

    private void drawEnemySprite(EnemyComponent enemy, PositionComponent position, boolean boss) {
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
        float size = boss ? SCALE * BOSS_VISUAL_SCALE : SCALE;
        spriteBatch.draw(frame, px - size / 2f, py - size / 2f, size, size);
    }

    /** rgb/a fed to enemy_status_tint's v_color: rgb is the tint target, a is blend strength (0 = untinted).
     * Slow/poison/boss are averaged together when more than one is active at once, same as the original
     * slow+poison combo - this generalizes to a third contributor instead of hand-listing every combination. */
    private Color computeStatusTintColor(EnemyComponent enemy, boolean boss) {
        if (enemy.hitFlashTimer > 0f) {
            float strength = enemy.hitFlashTimer / GameConstants.ENEMY_HIT_FLASH_DURATION_SECONDS * HIT_FLASH_PEAK_STRENGTH;
            return statusTintScratch.set(1f, 1f, 1f, strength);
        }
        boolean slowed = false;
        boolean poisoned = false;
        for (ActiveEffect effect : enemy.activeEffects) {
            if (effect.type == EffectType.SLOW) {
                slowed = true;
            } else if (effect.type == EffectType.POISON) {
                poisoned = true;
            }
        }
        int activeCount = (slowed ? 1 : 0) + (poisoned ? 1 : 0) + (boss ? 1 : 0);
        if (activeCount == 0) {
            return NO_STATUS_TINT;
        }
        float r = 0f, g = 0f, b = 0f;
        if (slowed) {
            r += ICE_TINT_COLOR.r; g += ICE_TINT_COLOR.g; b += ICE_TINT_COLOR.b;
        }
        if (poisoned) {
            r += POISON_TINT_COLOR.r; g += POISON_TINT_COLOR.g; b += POISON_TINT_COLOR.b;
        }
        if (boss) {
            r += BOSS_TINT_COLOR.r; g += BOSS_TINT_COLOR.g; b += BOSS_TINT_COLOR.b;
        }
        float strength = activeCount >= 2 ? COMBINED_STATUS_TINT_STRENGTH : STATUS_TINT_STRENGTH;
        return statusTintScratch.set(r / activeCount, g / activeCount, b / activeCount, strength);
    }

    private void drawEnemyHpBar(EnemyComponent enemy, PositionComponent position, boolean boss) {
        if (enemy.stateMachine.getCurrentState() == EnemyState.DYING) {
            return;
        }
        float px = position.value.x * SCALE;
        float py = position.value.y * SCALE;
        float visualScale = boss ? BOSS_VISUAL_SCALE : 1f;

        float hpRatio = Math.max(0f, enemy.hp / enemy.maxHp);
        float barWidth = SCALE * 0.6f * visualScale;
        float barY = py + SCALE * 0.4f * visualScale;
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
        enemyStatusTintShader.dispose();
        towerOutlineShader.dispose();
        for (Texture texture : outlineFrameTextures) {
            texture.dispose();
        }
        particleEffectManager.dispose();
        confirmIconTexture.dispose();
        cancelIconTexture.dispose();
        placementPanelTexture.dispose();
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
