package hr.brajnovic.td.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/** Static, app-lifetime input mode setting, mirroring {@link hr.brajnovic.td.i18n.Localization}'s static-utility shape. */
public final class InputSettings {

    private static final String PREFS_NAME = "brajnovic-td-settings";
    private static final String PREF_KEY_MODE = "inputMode";
    private static final InputMode DEFAULT_MODE = InputMode.MOUSE;

    private static InputMode mode = DEFAULT_MODE;

    private InputSettings() {
    }

    public static void init() {
        String stored = getPreferences().getString(PREF_KEY_MODE, DEFAULT_MODE.name());
        try {
            mode = InputMode.valueOf(stored);
        } catch (IllegalArgumentException e) {
            mode = DEFAULT_MODE;
        }
    }

    public static InputMode getMode() {
        return mode;
    }

    public static void setMode(InputMode newMode) {
        mode = newMode;
        getPreferences().putString(PREF_KEY_MODE, newMode.name()).flush();
    }

    private static Preferences getPreferences() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }
}
