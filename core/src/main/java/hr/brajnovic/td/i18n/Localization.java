package hr.brajnovic.td.i18n;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.I18NBundle;

import java.util.Locale;

public final class Localization {

    public static final String LANGUAGE_CROATIAN = "hr";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String DEFAULT_LANGUAGE = LANGUAGE_CROATIAN;

    private static final String PREFS_NAME = "brajnovic-td-settings";
    private static final String PREF_KEY_LANGUAGE = "language";

    private static I18NBundle bundle;

    private Localization() {
    }

    public static void init() {
        setLanguage(getPreferences().getString(PREF_KEY_LANGUAGE, DEFAULT_LANGUAGE));
    }

    public static void setLanguage(String languageCode) {
        bundle = I18NBundle.createBundle(Gdx.files.internal("i18n/strings"), Locale.forLanguageTag(languageCode));
        getPreferences().putString(PREF_KEY_LANGUAGE, languageCode).flush();
    }

    public static String getLanguage() {
        return getPreferences().getString(PREF_KEY_LANGUAGE, DEFAULT_LANGUAGE);
    }

    public static String get(String key) {
        return bundle.get(key);
    }

    public static String format(String key, Object... args) {
        return bundle.format(key, args);
    }

    private static Preferences getPreferences() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }
}
