package hr.brajnovic.td.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import hr.brajnovic.td.BrajnovicTD;
import hr.brajnovic.td.i18n.Localization;
import hr.brajnovic.td.ui.SkinFactory;

public class MenuScreen implements Screen {

    private final BrajnovicTD game;
    private final Stage stage;
    private final Skin skin;

    public MenuScreen(BrajnovicTD game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.skin = SkinFactory.createSkin();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label(Localization.get("app.title"), skin, "window");
        root.add(title).padBottom(40).row();

        if (game.hasPausedGame()) {
            TextButton resumeButton = new TextButton(Localization.get("menu.resume"), skin);
            resumeButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(game.resumePausedGame());
                }
            });
            root.add(resumeButton).width(200).padBottom(20).row();
        }

        TextButton playButton = new TextButton(Localization.get("menu.play"), skin);
        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.discardPausedGame();
                game.setScreen(new GameScreen(game));
            }
        });
        root.add(playButton).width(200).padBottom(20).row();

        TextButton optionsButton = new TextButton(Localization.get("menu.options"), skin);
        optionsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new OptionsScreen(game));
            }
        });
        root.add(optionsButton).width(200).padBottom(20).row();

        TextButton quitButton = new TextButton(Localization.get("menu.quit"), skin);
        quitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
        root.add(quitButton).width(200);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.1f, 0.1f, 0.12f, 1f);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
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
        stage.dispose();
        skin.dispose();
    }
}
