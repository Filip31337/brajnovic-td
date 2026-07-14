package hr.brajnovic.td.fx;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import hr.brajnovic.td.GameConstants;

/** Decorative Box2DLights point-light glow (muzzle flash + travelling projectile light), no shadow casting --
 * the owned {@link World} never has any body added to it, it exists only because {@link RayHandler}'s
 * constructor requires one. Light ids are looked up dynamically the same way {@link ParticleEffectManager}
 * looks up particle ids, so towers.json can reference them (muzzleFlashLightId/projectileLightId) without
 * any code change for new tower types. */
public class LightEffectManager implements Disposable {

    private static final int RAYS = 32;
    private static final float DISTANCE_PX = 150f;

    private static final ObjectMap<String, Color> SPEC_COLORS = new ObjectMap<>();

    static {
        SPEC_COLORS.put("cannon_glow", new Color(1f, 0.55f, 0.15f, 0.55f));
        SPEC_COLORS.put("ice_glow", new Color(0.4f, 0.8f, 1f, 0.55f));
        SPEC_COLORS.put("poison_glow", new Color(0.4f, 1f, 0.4f, 0.55f));
    }

    private static final class ActiveFlash {
        String specId;
        PointLight light;
        float remaining;
    }

    private final World world;
    private final RayHandler rayHandler;
    private final ObjectMap<String, Array<PointLight>> freeLightsBySpec = new ObjectMap<>();
    private final Array<ActiveFlash> activeFlashes = new Array<>();

    public LightEffectManager() {
        world = new World(Vector2.Zero, true);
        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(GameConstants.LIGHT_AMBIENT_INTENSITY);
        rayHandler.setShadows(false);
    }

    /** Fire-and-forget flash that self-expires after duration seconds. No-op if lightId is null/unknown. */
    public void spawnFlash(String lightId, float x, float y, float duration) {
        if (lightId == null) {
            return;
        }
        Color color = SPEC_COLORS.get(lightId);
        if (color == null) {
            return;
        }
        ActiveFlash flash = new ActiveFlash();
        flash.specId = lightId;
        flash.light = obtain(lightId, color, x, y);
        flash.remaining = duration;
        activeFlashes.add(flash);
    }

    /** Obtains a light for the caller to reposition every frame and eventually {@link #release}.
     * Returns null if lightId is null/unknown, so callers never need to guard the call site. */
    public PointLight attachTraveling(String lightId, float x, float y) {
        if (lightId == null) {
            return null;
        }
        Color color = SPEC_COLORS.get(lightId);
        if (color == null) {
            return null;
        }
        return obtain(lightId, color, x, y);
    }

    public void release(String lightId, PointLight light) {
        light.setActive(false);
        freeLightsBySpec.get(lightId).add(light);
    }

    private PointLight obtain(String lightId, Color color, float x, float y) {
        Array<PointLight> free = freeLightsBySpec.get(lightId);
        if (free == null) {
            free = new Array<>();
            freeLightsBySpec.put(lightId, free);
        }
        PointLight light;
        if (free.size > 0) {
            light = free.pop();
            light.setActive(true);
        } else {
            light = new PointLight(rayHandler, RAYS, color, DISTANCE_PX, x, y);
        }
        light.setPosition(x, y);
        return light;
    }

    /** Ages active muzzle flashes, releasing expired ones back to their pool. */
    public void update(float delta) {
        for (int i = activeFlashes.size - 1; i >= 0; i--) {
            ActiveFlash flash = activeFlashes.get(i);
            flash.remaining -= delta;
            if (flash.remaining <= 0f) {
                release(flash.specId, flash.light);
                activeFlashes.removeIndex(i);
            }
        }
    }

    /** Must be called once per frame, after all world-space sprites/shapes are drawn (RayHandler renders
     * its own additive-blend pass, it isn't a Batch draw so it can't be nested in a SpriteBatch begin/end). */
    public void render(OrthographicCamera camera) {
        rayHandler.setCombinedMatrix(camera);
        rayHandler.updateAndRender();
    }

    /** Clips the light layer to the map viewport area only, matching mapViewport's own screen bounds
     * (not the full window -- the docked HUD panel occupies the right edge, see CLAUDE.md). */
    public void resize(int width, int height) {
        rayHandler.useCustomViewport(0, 0, width, height);
    }

    @Override
    public void dispose() {
        rayHandler.dispose();
        world.dispose();
    }
}
