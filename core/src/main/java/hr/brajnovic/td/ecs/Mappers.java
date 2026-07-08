package hr.brajnovic.td.ecs;

import com.badlogic.ashley.core.ComponentMapper;
import hr.brajnovic.td.enemy.EnemyComponent;
import hr.brajnovic.td.tower.ProjectileComponent;
import hr.brajnovic.td.tower.TowerComponent;

/** Central {@link ComponentMapper} registry, the standard Ashley idiom for fast component lookup. */
public final class Mappers {

    public static final ComponentMapper<PositionComponent> POSITION = ComponentMapper.getFor(PositionComponent.class);
    public static final ComponentMapper<TowerComponent> TOWER = ComponentMapper.getFor(TowerComponent.class);
    public static final ComponentMapper<EnemyComponent> ENEMY = ComponentMapper.getFor(EnemyComponent.class);
    public static final ComponentMapper<ProjectileComponent> PROJECTILE = ComponentMapper.getFor(ProjectileComponent.class);

    private Mappers() {
    }
}
