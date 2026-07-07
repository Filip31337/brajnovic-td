package hr.brajnovic.td;

import com.badlogic.gdx.Game;
import hr.brajnovic.td.i18n.Localization;
import hr.brajnovic.td.screen.MenuScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class BrajnovicTD extends Game {

    @Override
    public void create() {
        Localization.init();
        setScreen(new MenuScreen(this));
    }
}
