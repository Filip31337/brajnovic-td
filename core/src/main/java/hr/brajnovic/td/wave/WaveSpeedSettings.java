package hr.brajnovic.td.wave;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/** Static, app-lifetime "remember wave speed" preference, mirroring {@link hr.brajnovic.td.i18n.Localization}'s
 * static-utility shape. When remembered, the player's last chosen 1x/2x wave speed is re-applied automatically
 * at the start of every subsequent wave (and level, and app session) instead of resetting to 1x each time. */
public final class WaveSpeedSettings {

    private static final String PREFS_NAME = "brajnovic-td-settings";
    // Renamed (v2 suffix) to force a clean 1x/unchecked default: earlier testing had already persisted
    // remembered=true/2x under the old keys, which isn't a code bug -- it's the feature working as
    // designed on stale saved state, but 1x/unchecked is the correct out-of-the-box default.
    private static final String PREF_KEY_REMEMBER = "rememberWaveSpeed_v2";
    private static final String PREF_KEY_TIME_SCALE = "preferredWaveTimeScale_v2";
    private static final float DEFAULT_TIME_SCALE = 1f;

    private static boolean remembered = false;
    private static float preferredTimeScale = DEFAULT_TIME_SCALE;

    private WaveSpeedSettings() {
    }

    public static void init() {
        remembered = getPreferences().getBoolean(PREF_KEY_REMEMBER, false);
        preferredTimeScale = getPreferences().getFloat(PREF_KEY_TIME_SCALE, DEFAULT_TIME_SCALE);
    }

    public static boolean isRemembered() {
        return remembered;
    }

    public static void setRemembered(boolean value) {
        remembered = value;
        getPreferences().putBoolean(PREF_KEY_REMEMBER, value).flush();
    }

    public static float getPreferredTimeScale() {
        return preferredTimeScale;
    }

    /** No-op for 0f (pause) -- only 1x/2x are ever worth remembering as "the" wave speed. */
    public static void setPreferredTimeScale(float timeScale) {
        if (timeScale <= 0f) {
            return;
        }
        preferredTimeScale = timeScale;
        getPreferences().putFloat(PREF_KEY_TIME_SCALE, timeScale).flush();
    }

    private static Preferences getPreferences() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }
}
