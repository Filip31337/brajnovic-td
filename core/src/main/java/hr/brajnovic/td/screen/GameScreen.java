package hr.brajnovic.td.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import hr.brajnovic.td.BrajnovicTD;
import hr.brajnovic.td.GameConstants;
import hr.brajnovic.td.economy.Economy;
import hr.brajnovic.td.enemy.Enemy;
import hr.brajnovic.td.enemy.EnemyRegistry;
import hr.brajnovic.td.map.GridMap;
import hr.brajnovic.td.map.LevelDefinition;
import hr.brajnovic.td.map.LevelLoader;
import hr.brajnovic.td.tower.Projectile;
import hr.brajnovic.td.tower.Tower;
import hr.brajnovic.td.tower.TowerDefinition;
import hr.brajnovic.td.tower.TowerPlacementValidator;
import hr.brajnovic.td.tower.TowerRegistry;
import hr.brajnovic.td.wave.GamePhase;
import hr.brajnovic.td.wave.WaveController;

import java.util.ArrayList;
import java.util.List;

public class GameScreen implements Screen {

    private static final int SCALE = GameConstants.SCALED_TILE_SIZE_PX;

    private final BrajnovicTD game;

    private final TowerRegistry towerRegistry;
    private final EnemyRegistry enemyRegistry;
    private final LevelDefinition level;

    private final TiledMap tiledMap;
    private final GridMap gridMap;
    private final OrthogonalTiledMapRenderer mapRenderer;

    private final Economy economy;
    private final WaveController waveController;
    private final List<Tower> towers = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();

    private final OrthographicCamera mapCamera;
    private final Viewport mapViewport;
    private final ShapeRenderer shapeRenderer;

    private final Stage hudStage;
    private final Stage overlayStage;
    private final Skin skin;
    private final InputMultiplexer inputMultiplexer;

    private Label goldLabel;
    private Label livesLabel;
    private Label waveLabel;
    private Label nextWaveTimerLabel;
    private TextButton startWaveButton;
    private TextButton arrowTowerButton;

    private Window overlayWindow;
    private Label overlayMessageLabel;
    private boolean gameEnded = false;

    private String selectedTowerId = null;
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
        waveController = new WaveController(level, enemyRegistry, gridMap, economy);

        mapCamera = new OrthographicCamera();
        mapViewport = new ExtendViewport(
            GameConstants.MAP_VIEWPORT_WIDTH_PX,
            GameConstants.MAP_VIEWPORT_HEIGHT_PX,
            mapCamera
        );
        shapeRenderer = new ShapeRenderer();

        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        hudStage = new Stage(new ScreenViewport());
        overlayStage = new Stage(new ScreenViewport());
        buildHud();
        buildOverlay();

        inputMultiplexer = new InputMultiplexer(overlayStage, hudStage, mapInputProcessor);

        resize(GameConstants.WINDOW_WIDTH_PX, GameConstants.WINDOW_HEIGHT_PX);
    }

    private final InputAdapter mapInputProcessor = new InputAdapter() {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (button == Input.Buttons.RIGHT) {
                cancelPlacement();
                return true;
            }
            if (button == Input.Buttons.LEFT && selectedTowerId != null) {
                updateHoverTile(screenX, screenY);
                tryPlaceSelectedTower();
                return true;
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
        towers.add(new Tower(definition, hoverTileX, hoverTileY));
    }

    private void buildHud() {
        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(16);
        hudStage.addActor(root);

        root.add(new Label("Brajnovic TD", skin, "window")).left().row();

        TextButton menuButton = new TextButton("Menu", skin);
        menuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
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

        TowerDefinition arrowTower = towerRegistry.get("arrow_tower");
        arrowTowerButton = new TextButton(arrowTower.name + " (" + arrowTower.cost + "g)", skin);
        arrowTowerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if ("arrow_tower".equals(selectedTowerId)) {
                    cancelPlacement();
                } else {
                    selectedTowerId = "arrow_tower";
                }
            }
        });
        root.add(arrowTowerButton).padTop(24).width(200).left().row();

        root.add().expandY().row();

        nextWaveTimerLabel = new Label("", skin);
        root.add(nextWaveTimerLabel).padBottom(8).left().row();

        startWaveButton = new TextButton("START WAVE", skin);
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

        TextButton retryButton = new TextButton("Retry", skin);
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
        goldLabel.setText("Gold: " + economy.getGold());
        livesLabel.setText("Lives: " + economy.getLives());
        waveLabel.setText("Wave: " + waveController.getCurrentWaveNumber() + " / " + waveController.getTotalWaveCount());

        boolean buildPhase = waveController.getPhase() == GamePhase.BUILD;
        startWaveButton.setDisabled(!waveController.canStartNextWave());
        arrowTowerButton.setDisabled(!buildPhase);
        if (!buildPhase) {
            selectedTowerId = null;
        }

        nextWaveTimerLabel.setText(
            waveController.isBuildPhaseTimerActive()
                ? "Next wave in: " + waveController.getBuildPhaseSecondsRemaining() + "s"
                : ""
        );
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public void render(float delta) {
        if (!gameEnded) {
            waveController.update(delta);
            for (Tower tower : towers) {
                tower.update(delta, waveController.getActiveEnemies(), projectiles);
            }
            projectiles.removeIf(projectile -> !projectile.update(delta));

            if (economy.isGameOver()) {
                gameEnded = true;
                showOverlay("Game Over", "Ostao si bez zivota.");
            } else if (waveController.allWavesCleared()) {
                gameEnded = true;
                showOverlay("Pobjeda!", "Ocistio si sve valove.");
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

        overlayStage.act(delta);
        overlayStage.getViewport().apply();
        overlayStage.draw();
    }

    private void renderEntities() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(mapCamera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Tower tower : towers) {
            drawTower(tower);
        }
        for (Enemy enemy : waveController.getActiveEnemies()) {
            drawEnemy(enemy);
        }
        Vector2 projectilePosition = new Vector2();
        for (Projectile projectile : projectiles) {
            projectile.getCurrentPosition(projectilePosition);
            shapeRenderer.setColor(Color.YELLOW);
            shapeRenderer.circle(projectilePosition.x * SCALE, projectilePosition.y * SCALE, 3f);
        }
        if (selectedTowerId != null && gridMap.isInBounds(hoverTileX, hoverTileY)) {
            drawGhost();
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        if (selectedTowerId != null && gridMap.isInBounds(hoverTileX, hoverTileY)) {
            drawRangeCircle(hoverTileX + 0.5f, hoverTileY + 0.5f, towerRegistry.get(selectedTowerId).rangeTiles);
        } else {
            Tower hovered = findTowerAt(hoverTileX, hoverTileY);
            if (hovered != null) {
                drawRangeCircle(hovered.getPosition().x, hovered.getPosition().y, hovered.getDefinition().rangeTiles);
            }
        }
        shapeRenderer.end();
    }

    private void drawTower(Tower tower) {
        float px = tower.getPosition().x * SCALE;
        float py = tower.getPosition().y * SCALE;

        shapeRenderer.setColor(0.25f, 0.4f, 0.85f, 1f);
        shapeRenderer.circle(px, py, SCALE * 0.35f);

        float barrelLength = SCALE * 0.4f;
        float endX = px + MathUtils.cosDeg(tower.getTurretAngleDeg()) * barrelLength;
        float endY = py + MathUtils.sinDeg(tower.getTurretAngleDeg()) * barrelLength;
        shapeRenderer.setColor(Color.NAVY);
        shapeRenderer.rectLine(px, py, endX, endY, 3f);
    }

    private void drawEnemy(Enemy enemy) {
        float px = enemy.getPosition().x * SCALE;
        float py = enemy.getPosition().y * SCALE;

        shapeRenderer.setColor(Color.FIREBRICK);
        shapeRenderer.circle(px, py, SCALE * 0.28f);

        float hpRatio = Math.max(0f, enemy.getHp() / enemy.getMaxHp());
        float barWidth = SCALE * 0.6f;
        float barY = py + SCALE * 0.4f;
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(px - barWidth / 2f, barY, barWidth, 4f);
        shapeRenderer.setColor(Color.LIME);
        shapeRenderer.rect(px - barWidth / 2f, barY, barWidth * hpRatio, 4f);
    }

    private void drawGhost() {
        TowerDefinition definition = towerRegistry.get(selectedTowerId);
        boolean valid = TowerPlacementValidator.canPlaceTowerAt(gridMap, hoverTileX, hoverTileY)
            && economy.getGold() >= definition.cost;

        shapeRenderer.setColor(valid ? new Color(0f, 1f, 0f, 0.45f) : new Color(1f, 0f, 0f, 0.45f));
        float px = (hoverTileX + 0.5f) * SCALE;
        float py = (hoverTileY + 0.5f) * SCALE;
        shapeRenderer.circle(px, py, SCALE * 0.35f);
    }

    private void drawRangeCircle(float centerTileX, float centerTileY, float radiusTiles) {
        shapeRenderer.circle(centerTileX * SCALE, centerTileY * SCALE, radiusTiles * SCALE, 64);
    }

    private Tower findTowerAt(int gridX, int gridY) {
        for (Tower tower : towers) {
            if (tower.getGridX() == gridX && tower.getGridY() == gridY) {
                return tower;
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
        mapRenderer.dispose();
        tiledMap.dispose();
        hudStage.dispose();
        overlayStage.dispose();
        skin.dispose();
    }
}
