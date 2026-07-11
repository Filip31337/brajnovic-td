package hr.brajnovic.td.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ObjectMap;

/** Static, app-lifetime audio playback, mirroring {@link hr.brajnovic.td.i18n.Localization}'s static-utility shape.
 * Every .ogg file under assets/sounds/ is eagerly loaded and keyed by its filename (no extension) as sound ID -
 * data (towers.json/enemies.json shootSoundId etc.) just references that ID, no code change needed for new sounds. */
public final class SoundManager {

    private static final String PREFS_NAME = "brajnovic-td-settings";
    private static final String PREF_KEY_VOLUME = "soundVolume";
    private static final String PREF_KEY_MUTED = "soundMuted";
    private static final float DEFAULT_VOLUME = 0.8f;

    private static final ObjectMap<String, Sound> sounds = new ObjectMap<>();
    private static float volume = DEFAULT_VOLUME;
    private static boolean muted = false;

    private SoundManager() {
    }

    public static void init() {
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
        sounds.clear();
        for (FileHandle file : Gdx.files.internal("sounds").list(".ogg")) {
            sounds.put(file.nameWithoutExtension(), Gdx.audio.newSound(file));
        }
        volume = getPreferences().getFloat(PREF_KEY_VOLUME, DEFAULT_VOLUME);
        muted = getPreferences().getBoolean(PREF_KEY_MUTED, false);
    }

    /** No-op if soundId is null or unknown, so data-driven callers never need a null/existence check. */
    public static void play(String soundId) {
        if (soundId == null || muted || volume <= 0f) {
            return;
        }
        Sound sound = sounds.get(soundId);
        if (sound != null) {
            sound.play(volume);
        }
    }

    public static float getVolume() {
        return volume;
    }

    public static void setVolume(float newVolume) {
        volume = Math.max(0f, Math.min(1f, newVolume));
        getPreferences().putFloat(PREF_KEY_VOLUME, volume).flush();
    }

    public static boolean isMuted() {
        return muted;
    }

    public static void setMuted(boolean newMuted) {
        muted = newMuted;
        getPreferences().putBoolean(PREF_KEY_MUTED, muted).flush();
    }

    private static Preferences getPreferences() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }
}
