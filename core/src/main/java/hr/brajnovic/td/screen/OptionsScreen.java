package hr.brajnovic.td.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import hr.brajnovic.td.BrajnovicTD;
import hr.brajnovic.td.i18n.Localization;
import hr.brajnovic.td.sound.SoundManager;
import hr.brajnovic.td.ui.SkinFactory;

public class OptionsScreen implements Screen {

    private final BrajnovicTD game;
    private final Stage stage;
    private final Skin skin;

    public OptionsScreen(BrajnovicTD game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.skin = SkinFactory.createSkin();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label(Localization.get("options.title"), skin, "window");
        root.add(title).colspan(2).padBottom(40).row();

        Label languageLabel = new Label(Localization.get("options.language"), skin);
        root.add(languageLabel).colspan(2).padBottom(12).row();

        TextButton croatianButton = new TextButton("Hrvatski", skin);
        croatianButton.setDisabled(Localization.LANGUAGE_CROATIAN.equals(Localization.getLanguage()));
        croatianButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Localization.setLanguage(Localization.LANGUAGE_CROATIAN);
                game.setScreen(new OptionsScreen(game));
            }
        });
        root.add(croatianButton).width(160).padRight(12).padBottom(30);

        TextButton englishButton = new TextButton("English", skin);
        englishButton.setDisabled(Localization.LANGUAGE_ENGLISH.equals(Localization.getLanguage()));
        englishButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Localization.setLanguage(Localization.LANGUAGE_ENGLISH);
                game.setScreen(new OptionsScreen(game));
            }
        });
        root.add(englishButton).width(160).padBottom(30).row();

        Label volumeLabel = new Label(Localization.get("options.volume"), skin);
        root.add(volumeLabel).colspan(2).padBottom(12).row();

        Table volumeRow = new Table();
        Slider volumeSlider = new Slider(0f, 1f, 0.05f, false, skin);
        volumeSlider.setValue(SoundManager.getVolume());
        Label volumePercentLabel = new Label(volumePercentText(volumeSlider.getValue()), skin);
        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SoundManager.setVolume(volumeSlider.getValue());
                volumePercentLabel.setText(volumePercentText(volumeSlider.getValue()));
            }
        });
        volumeRow.add(volumeSlider).width(200).padRight(12);
        volumeRow.add(volumePercentLabel).width(44).left();
        root.add(volumeRow).colspan(2).padBottom(20).row();

        CheckBox muteCheckBox = new CheckBox(" " + Localization.get("options.mute"), skin);
        muteCheckBox.setChecked(SoundManager.isMuted());
        muteCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SoundManager.setMuted(muteCheckBox.isChecked());
            }
        });
        root.add(muteCheckBox).colspan(2).padBottom(30).row();

        TextButton backButton = new TextButton(Localization.get("options.back"), skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MenuScreen(game));
            }
        });
        root.add(backButton).colspan(2).width(200);
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

    private static String volumePercentText(float value) {
        return Math.round(value * 100) + "%";
    }
}
