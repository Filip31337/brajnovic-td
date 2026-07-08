package hr.brajnovic.td.ecs;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool.Poolable;

/** Continuous world position (tile units), shared by tower/enemy/projectile entities. */
public class PositionComponent implements Component, Poolable {

    public final Vector2 value = new Vector2();

    @Override
    public void reset() {
        value.setZero();
    }
}
