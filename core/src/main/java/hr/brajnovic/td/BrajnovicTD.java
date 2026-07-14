package hr.brajnovic.td;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.physics.box2d.Box2D;
import hr.brajnovic.td.i18n.Localization;
import hr.brajnovic.td.input.InputSettings;
import hr.brajnovic.td.screen.GameScreen;
import hr.brajnovic.td.screen.MenuScreen;
import hr.brajnovic.td.sound.SoundManager;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class BrajnovicTD extends Game {

    private GameScreen pausedGame;

    @Override
    public void create() {
        Box2D.init();
        Localization.init();
        SoundManager.init();
        InputSettings.init();
        setScreen(new MenuScreen(this));
    }

    /** Stashes an in-progress game (navigated away from via its Menu button) so it can be resumed later. */
    public void pauseGame(GameScreen screen) {
        pausedGame = screen;
    }

    public boolean hasPausedGame() {
        return pausedGame != null;
    }

    /** Hands back the stashed game and clears the stash; the caller is expected to switch to it. */
    public GameScreen resumePausedGame() {
        GameScreen screen = pausedGame;
        pausedGame = null;
        return screen;
    }

    /** Disposes and clears any stashed game, e.g. because the player started a fresh one instead. */
    public void discardPausedGame() {
        if (pausedGame != null) {
            pausedGame.dispose();
            pausedGame = null;
        }
    }
}
