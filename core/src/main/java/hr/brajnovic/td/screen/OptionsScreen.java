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
import hr.brajnovic.td.GameConstants;
import hr.brajnovic.td.i18n.Localization;
import hr.brajnovic.td.input.InputMode;
import hr.brajnovic.td.input.InputSettings;
import hr.brajnovic.td.sound.SoundManager;
import hr.brajnovic.td.ui.SkinFactory;

public class OptionsScreen implements Screen {

    private final BrajnovicTD game;
    private final Stage stage;
    private final Skin skin;

    public OptionsScreen(BrajnovicTD game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        ((ScreenViewport) stage.getViewport()).setUnitsPerPixel(1f / GameConstants.MENU_UI_SCALE);
        this.skin = SkinFactory.createSkin(GameConstants.MENU_UI_SCALE);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label(Localization.get("options.title"), skin, "window");
        root.add(title).colspan(2).padBottom(40).row();

        // Android's 4x MENU_UI_SCALE makes every row much taller, and this screen has no ScrollPane, so
        // on top of the desktop layout the content overflowed the screen and hid the back button. Folding
        // each section's label into its control row (instead of its own row) reclaims vertical space --
        // desktop keeps the original label-then-controls layout since it isn't cramped.
        boolean android = GameConstants.isAndroid();

        Label languageLabel = new Label(Localization.get("options.language"), skin);

        TextButton croatianButton = new TextButton("Hrvatski", skin);
        croatianButton.setDisabled(Localization.LANGUAGE_CROATIAN.equals(Localization.getLanguage()));
        croatianButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Localization.setLanguage(Localization.LANGUAGE_CROATIAN);
                game.setScreen(new OptionsScreen(game));
            }
        });

        TextButton englishButton = new TextButton("English", skin);
        englishButton.setDisabled(Localization.LANGUAGE_ENGLISH.equals(Localization.getLanguage()));
        englishButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Localization.setLanguage(Localization.LANGUAGE_ENGLISH);
                game.setScreen(new OptionsScreen(game));
            }
        });

        if (android) {
            Table languageRow = new Table();
            languageRow.add(languageLabel).padRight(12);
            languageRow.add(croatianButton).width(160).padRight(12);
            languageRow.add(englishButton).width(160);
            root.add(languageRow).colspan(2).padBottom(30).row();
        } else {
            root.add(languageLabel).colspan(2).padBottom(12).row();
            root.add(croatianButton).width(160).padRight(12).padBottom(30);
            root.add(englishButton).width(160).padBottom(30).row();
        }

        Label volumeLabel = new Label(Localization.get("options.volume"), skin);

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

        CheckBox muteCheckBox = new CheckBox(" " + Localization.get("options.mute"), skin);
        muteCheckBox.setChecked(SoundManager.isMuted());
        muteCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SoundManager.setMuted(muteCheckBox.isChecked());
            }
        });

        if (android) {
            volumeRow.add(volumeLabel).padRight(12);
            volumeRow.add(volumeSlider).width(200).padRight(12);
            volumeRow.add(volumePercentLabel).width(44).padRight(12).left();
            volumeRow.add(muteCheckBox).left();
            root.add(volumeRow).colspan(2).padBottom(30).row();
        } else {
            root.add(volumeLabel).colspan(2).padBottom(12).row();
            volumeRow.add(volumeSlider).width(200).padRight(12);
            volumeRow.add(volumePercentLabel).width(44).left();
            root.add(volumeRow).colspan(2).padBottom(20).row();
            root.add(muteCheckBox).colspan(2).padBottom(30).row();
        }

        Label inputModeLabel = new Label(Localization.get("options.inputMode"), skin);

        TextButton mouseButton = new TextButton(Localization.get("options.inputMode.mouse"), skin);
        mouseButton.setDisabled(InputMode.MOUSE.equals(InputSettings.getMode()));
        mouseButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                InputSettings.setMode(InputMode.MOUSE);
                game.setScreen(new OptionsScreen(game));
            }
        });

        TextButton touchButton = new TextButton(Localization.get("options.inputMode.touch"), skin);
        touchButton.setDisabled(InputMode.TOUCH.equals(InputSettings.getMode()));
        touchButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                InputSettings.setMode(InputMode.TOUCH);
                game.setScreen(new OptionsScreen(game));
            }
        });

        if (android) {
            Table inputModeRow = new Table();
            inputModeRow.add(inputModeLabel).padRight(12);
            inputModeRow.add(mouseButton).width(160).padRight(12);
            inputModeRow.add(touchButton).width(160);
            root.add(inputModeRow).colspan(2).padBottom(30).row();
        } else {
            root.add(inputModeLabel).colspan(2).padBottom(12).row();
            root.add(mouseButton).width(160).padRight(12).padBottom(30);
            root.add(touchButton).width(160).padBottom(30).row();
        }

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
