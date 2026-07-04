package hr.brajnovic.td.enemy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;

import java.util.LinkedHashMap;
import java.util.Map;

/** Loads and looks up {@link EnemyDefinition}s from assets/data/enemies.json. */
public class EnemyRegistry {

    private final Map<String, EnemyDefinition> definitionsById = new LinkedHashMap<>();

    public static EnemyRegistry loadFromInternal(String path) {
        EnemyDefinition[] definitions = new Json().fromJson(EnemyDefinition[].class, Gdx.files.internal(path));
        EnemyRegistry registry = new EnemyRegistry();
        for (EnemyDefinition definition : definitions) {
            registry.definitionsById.put(definition.id, definition);
        }
        return registry;
    }

    public EnemyDefinition get(String id) {
        EnemyDefinition definition = definitionsById.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown enemy id: " + id);
        }
        return definition;
    }

    public Iterable<EnemyDefinition> all() {
        return definitionsById.values();
    }
}
