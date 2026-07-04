package hr.brajnovic.td.tower;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;

import java.util.LinkedHashMap;
import java.util.Map;

/** Loads and looks up {@link TowerDefinition}s from assets/data/towers.json. */
public class TowerRegistry {

    private final Map<String, TowerDefinition> definitionsById = new LinkedHashMap<>();

    public static TowerRegistry loadFromInternal(String path) {
        TowerDefinition[] definitions = new Json().fromJson(TowerDefinition[].class, Gdx.files.internal(path));
        TowerRegistry registry = new TowerRegistry();
        for (TowerDefinition definition : definitions) {
            registry.definitionsById.put(definition.id, definition);
        }
        return registry;
    }

    public TowerDefinition get(String id) {
        TowerDefinition definition = definitionsById.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown tower id: " + id);
        }
        return definition;
    }

    public Iterable<TowerDefinition> all() {
        return definitionsById.values();
    }
}
