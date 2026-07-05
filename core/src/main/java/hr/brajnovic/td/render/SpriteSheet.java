package hr.brajnovic.td.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Loads a hand-authored sprites-src sheet: a PNG plus a sidecar JSON listing named
 * animations, each a list of {x, y, w, h} frame rects (see assets/sprites-src/*.json).
 * This is a direct loader (no TexturePacker/atlas step yet); revisit before V1 asset lock-in.
 */
public class SpriteSheet {

    private final Texture texture;
    private final ObjectMap<String, TextureRegion[]> animations = new ObjectMap<>();

    private SpriteSheet(Texture texture) {
        this.texture = texture;
    }

    public static SpriteSheet loadFromInternal(String basePath) {
        Texture texture = new Texture(Gdx.files.internal(basePath + ".png"));
        texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        SpriteSheet sheet = new SpriteSheet(texture);
        JsonValue root = new JsonReader().parse(Gdx.files.internal(basePath + ".json"));
        JsonValue animationsJson = root.get("animations");
        for (JsonValue animation = animationsJson.child; animation != null; animation = animation.next) {
            TextureRegion[] frames = new TextureRegion[animation.size];
            int i = 0;
            for (JsonValue frame = animation.child; frame != null; frame = frame.next) {
                frames[i++] = new TextureRegion(texture, frame.getInt("x"), frame.getInt("y"), frame.getInt("w"), frame.getInt("h"));
            }
            sheet.animations.put(animation.name, frames);
        }
        return sheet;
    }

    public TextureRegion[] getAnimation(String name) {
        TextureRegion[] frames = animations.get(name);
        if (frames == null) {
            throw new IllegalArgumentException("Unknown animation: " + name);
        }
        return frames;
    }

    public void dispose() {
        texture.dispose();
    }
}
