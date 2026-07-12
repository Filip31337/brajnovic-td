package hr.brajnovic.td.fx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool.PooledEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.IndependentScaledNumericValue;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/** One-shot burst particle effects (impact hits, enemy death, tower upgrade), built entirely through the
 * {@link ParticleEmitter} API rather than loaded from ".p" effect files: hand-authoring that file format
 * without the Flame Particle Editor GUI is fragile (positional text, several ParticleValue subtypes with
 * different active/timeline semantics), while every property used here has a public setter. Effect ids are
 * looked up dynamically by {@link #spawn(String, float, float)} so towers.json/enemies.json can reference
 * them (impactParticleId/deathParticleId) without any code change for new tower/enemy types. */
public class ParticleEffectManager implements Disposable {

    private static final int POOL_INITIAL_CAPACITY = 2;
    private static final int POOL_MAX_CAPACITY = 12;

    private record BurstSpec(
        int particleCount, float durationMs, float lifeMinMs, float lifeMaxMs,
        float velocityMin, float velocityMax, float startSizePx, float endSizePx,
        float gravity, boolean additive, Color color
    ) {
    }

    private static final ObjectMap<String, BurstSpec> SPECS = new ObjectMap<>();

    static {
        // Physical sparks: tight, fast, additive glow.
        SPECS.put("impact_arrow", new BurstSpec(10, 150f, 150f, 300f, 80f, 160f, 10f, 2f,
            0f, true, new Color(1f, 0.95f, 0.6f, 1f)));
        SPECS.put("impact_cannon", new BurstSpec(16, 200f, 250f, 450f, 60f, 140f, 16f, 4f,
            0f, true, new Color(1f, 0.55f, 0.15f, 1f)));
        SPECS.put("impact_ice", new BurstSpec(12, 180f, 200f, 380f, 70f, 150f, 8f, 2f,
            0f, true, new Color(0.65f, 0.85f, 1f, 1f)));
        // Poison bubbles drift upward slowly instead of blowing out with additive glow.
        SPECS.put("impact_poison", new BurstSpec(10, 200f, 300f, 550f, 30f, 70f, 10f, 4f,
            25f, false, new Color(0.45f, 0.9f, 0.35f, 1f)));
        // Enemy death: dark burst that falls with gravity, solid (non-additive) so it doesn't blow out white.
        SPECS.put("enemy_death", new BurstSpec(14, 180f, 250f, 450f, 50f, 130f, 10f, 3f,
            -60f, false, new Color(0.55f, 0.12f, 0.08f, 1f)));
        // Tower upgrade: gold sparkle rising slightly, additive glow.
        SPECS.put("tower_upgrade", new BurstSpec(14, 250f, 350f, 600f, 40f, 90f, 8f, 2f,
            35f, true, new Color(1f, 0.85f, 0.25f, 1f)));
    }

    private final Texture dotTexture;
    private final Sprite dotSprite;
    private final ObjectMap<String, ParticleEffectPool> pools = new ObjectMap<>();
    private final Array<PooledEffect> activeEffects = new Array<>();

    public ParticleEffectManager() {
        dotTexture = new Texture(Gdx.files.internal("particles/particle_dot.png"));
        dotTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        dotSprite = new Sprite(dotTexture);

        for (ObjectMap.Entry<String, BurstSpec> entry : SPECS) {
            ParticleEffect template = new ParticleEffect();
            template.getEmitters().add(buildEmitter(entry.value));
            pools.put(entry.key, new ParticleEffectPool(template, POOL_INITIAL_CAPACITY, POOL_MAX_CAPACITY));
        }
    }

    private ParticleEmitter buildEmitter(BurstSpec spec) {
        ParticleEmitter emitter = new ParticleEmitter();
        emitter.setMaxParticleCount(spec.particleCount());
        emitter.setMinParticleCount(spec.particleCount());
        emitter.setSprites(Array.with(dotSprite));
        emitter.setAdditive(spec.additive());

        emitter.getDuration().setLow(spec.durationMs());

        emitter.getLife().setLow(spec.lifeMinMs(), spec.lifeMaxMs());
        emitter.getLife().setHigh(spec.lifeMinMs(), spec.lifeMaxMs());
        // getLife() is typed ScaledNumericValue but the underlying field is IndependentScaledNumericValue.
        ((IndependentScaledNumericValue) emitter.getLife()).setIndependent(true);

        emitter.getAngle().setLow(0f, 360f);
        emitter.getAngle().setHigh(0f, 360f);

        emitter.getVelocity().setActive(true);
        emitter.getVelocity().setLow(spec.velocityMin(), spec.velocityMax());
        emitter.getVelocity().setHigh(spec.velocityMin(), spec.velocityMax());

        emitter.getXScale().setLow(spec.startSizePx());
        emitter.getXScale().setHigh(spec.endSizePx());
        emitter.getXScale().setTimeline(new float[]{0f, 1f});
        emitter.getXScale().setScaling(new float[]{0f, 1f});

        emitter.getTransparency().setLow(1f);
        emitter.getTransparency().setHigh(0f);
        emitter.getTransparency().setTimeline(new float[]{0f, 1f});
        emitter.getTransparency().setScaling(new float[]{0f, 1f});

        if (spec.gravity() != 0f) {
            emitter.getGravity().setActive(true);
            emitter.getGravity().setLow(spec.gravity());
            emitter.getGravity().setHigh(spec.gravity());
        }

        Color color = spec.color();
        emitter.getTint().setColors(new float[]{color.r, color.g, color.b});

        return emitter;
    }

    /** No-op if effectId is null or unknown, so data-driven callers (towers.json/enemies.json ids) never
     * need a null/existence check. x/y are in the same pixel space as the map camera (world tile units * SCALE). */
    public void spawn(String effectId, float x, float y) {
        if (effectId == null) {
            return;
        }
        ParticleEffectPool pool = pools.get(effectId);
        if (pool == null) {
            return;
        }
        PooledEffect effect = pool.obtain();
        effect.setPosition(x, y);
        activeEffects.add(effect);
    }

    /** Draws and ages every active effect, freeing completed ones back to their pool. Must be called once per
     * frame inside the same Batch.begin()/end() block used for the rest of world-space rendering. */
    public void renderAndPrune(Batch batch, float delta) {
        for (int i = activeEffects.size - 1; i >= 0; i--) {
            PooledEffect effect = activeEffects.get(i);
            effect.draw(batch, delta);
            if (effect.isComplete()) {
                activeEffects.removeIndex(i);
                effect.free();
            }
        }
    }

    @Override
    public void dispose() {
        dotTexture.dispose();
    }
}
