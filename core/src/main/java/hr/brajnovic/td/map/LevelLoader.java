package hr.brajnovic.td.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;

/** Loads a {@link LevelDefinition} from assets/data/levels/level_XX.json. */
public final class LevelLoader {

    private LevelLoader() {
    }

    public static LevelDefinition loadFromInternal(String path) {
        return new Json().fromJson(LevelDefinition.class, Gdx.files.internal(path));
    }
}
