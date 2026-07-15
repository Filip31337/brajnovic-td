package hr.brajnovic.td.input;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/** Static, app-lifetime input mode setting, mirroring {@link hr.brajnovic.td.i18n.Localization}'s static-utility shape. */
public final class InputSettings {

    private static final String PREFS_NAME = "brajnovic-td-settings";
    private static final String PREF_KEY_MODE = "inputMode";

    private static InputMode mode = InputMode.MOUSE;

    private InputSettings() {
    }

    /**
     * First-launch default only — Android gets Touch (no hover events on a real touchscreen), every
     * other platform gets Mouse. A previously stored preference (e.g. a user who explicitly switched
     * modes) always wins over this, regardless of platform.
     */
    public static void init() {
        InputMode platformDefault = Gdx.app.getType() == ApplicationType.Android ? InputMode.TOUCH : InputMode.MOUSE;
        String stored = getPreferences().getString(PREF_KEY_MODE, platformDefault.name());
        try {
            mode = InputMode.valueOf(stored);
        } catch (IllegalArgumentException e) {
            mode = platformDefault;
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
