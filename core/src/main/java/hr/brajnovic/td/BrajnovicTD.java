package hr.brajnovic.td;

import com.badlogic.gdx.Game;
import hr.brajnovic.td.i18n.Localization;
import hr.brajnovic.td.screen.GameScreen;
import hr.brajnovic.td.screen.MenuScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class BrajnovicTD extends Game {

    private GameScreen pausedGame;

    @Override
    public void create() {
        Localization.init();
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
