package hr.brajnovic.td.enemy;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.math.Vector2;
import hr.brajnovic.td.ecs.Mappers;
import hr.brajnovic.td.ecs.PositionComponent;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Live view of every enemy currently steering, for Separation's RadiusProximity neighbor queries.
 * Wraps Ashley's own ImmutableArray (kept in sync automatically as enemies spawn/die) rather than
 * maintaining a separate list, so a single instance can be shared by every enemy's RadiusProximity.
 */
public class EnemySteerables implements Iterable<Steerable<Vector2>> {

    private static final Family FAMILY = Family.all(EnemyComponent.class, PositionComponent.class, SteeringComponent.class).get();

    private final ImmutableArray<Entity> entities;

    public EnemySteerables(PooledEngine engine) {
        this.entities = engine.getEntitiesFor(FAMILY);
    }

    @Override
    public Iterator<Steerable<Vector2>> iterator() {
        return new Iterator<Steerable<Vector2>>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < entities.size();
            }

            @Override
            public Steerable<Vector2> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return Mappers.STEERING.get(entities.get(index++));
            }
        };
    }
}
