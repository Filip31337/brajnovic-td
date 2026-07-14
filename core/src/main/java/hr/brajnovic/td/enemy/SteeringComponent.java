package hr.brajnovic.td.enemy;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.behaviors.BlendedSteering;
import com.badlogic.gdx.ai.steer.behaviors.FollowPath;
import com.badlogic.gdx.ai.steer.behaviors.Separation;
import com.badlogic.gdx.ai.steer.proximities.RadiusProximity;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool.Poolable;
import hr.brajnovic.td.GameConstants;
import hr.brajnovic.td.ecs.PositionComponent;

import java.util.List;

/**
 * gdx-ai Steerable adapter for an enemy: wraps FollowPath (the A* waypoint path) blended with
 * Separation (crowd spreading) into one acceleration-based movement source for EnemyLifecycleSystem.
 * getPosition() returns the sibling PositionComponent's Vector2 directly (same object the renderer reads),
 * so there is a single source of truth for world position.
 */
public class SteeringComponent implements Component, Poolable, Steerable<Vector2> {

    public PositionComponent position;
    public final Vector2 linearVelocity = new Vector2();
    private float orientation;
    private boolean tagged;

    private float zeroLinearSpeedThreshold = 0.01f;
    public float maxLinearSpeed;
    public float maxLinearAcceleration;
    private float maxAngularSpeed;
    private float maxAngularAcceleration;
    public float boundingRadius;

    public LinePath<Vector2> linePath;
    public FollowPath<Vector2, LinePath.LinePathParam> followPath;
    public Separation<Vector2> separation;
    public BlendedSteering<Vector2> blended;
    public final SteeringAcceleration<Vector2> steeringOutput = new SteeringAcceleration<>(new Vector2());

    public void init(PositionComponent position, List<GridPoint2> path, Iterable<Steerable<Vector2>> neighbors,
                      float boundingRadius, float maxAcceleration) {
        this.position = position;
        this.boundingRadius = boundingRadius;
        this.maxLinearAcceleration = maxAcceleration;
        this.linearVelocity.setZero();

        Array<Vector2> waypoints = new Array<>(path.size());
        for (GridPoint2 tile : path) {
            waypoints.add(new Vector2(tile.x + 0.5f, tile.y + 0.5f));
        }
        // isOpen=true: the 1-arg LinePath constructor defaults to a CLOSED path, which appends a hidden
        // segment connecting the goal back to the spawn point -- that made getEndPoint() return the spawn
        // (segments.peek().end wraps to the first waypoint), so the goal-arrival check fired on frame one.
        this.linePath = new LinePath<>(waypoints, true);
        // pathOffset > 0 is required: with offset 0 (and predictionTime 0, the other constructors' default),
        // FollowPath's target-ahead point collapses onto the current position, so desired velocity is always
        // ~zero and the enemy never moves. setArriveEnabled(false) then guarantees no deceleration anywhere
        // (including near the goal) -- FollowPath's own default is arriveEnabled=true, which would otherwise
        // slow enemies down as they approach the last waypoint; we rely on EnemyLifecycleSystem's own
        // distance-to-goal check to remove them instead, matching the old code's abrupt-stop game feel.
        this.followPath = new FollowPath<>(this, linePath, GameConstants.ENEMY_STEERING_PATH_OFFSET_TILES)
            .setArriveEnabled(false);

        RadiusProximity<Vector2> proximity = new RadiusProximity<>(this, neighbors, GameConstants.ENEMY_SEPARATION_RADIUS_TILES);
        this.separation = new Separation<>(this, proximity)
            .setDecayCoefficient(GameConstants.ENEMY_SEPARATION_DECAY_COEFFICIENT);

        this.blended = new BlendedSteering<Vector2>(this)
            .add(followPath, 1f)
            .add(separation, GameConstants.ENEMY_SEPARATION_WEIGHT);
    }

    @Override
    public void reset() {
        position = null;
        linearVelocity.setZero();
        orientation = 0f;
        tagged = false;
        maxLinearSpeed = 0f;
        maxLinearAcceleration = 0f;
        maxAngularSpeed = 0f;
        maxAngularAcceleration = 0f;
        boundingRadius = 0f;
        linePath = null;
        followPath = null;
        separation = null;
        blended = null;
    }

    @Override
    public Vector2 getPosition() {
        return position.value;
    }

    @Override
    public float getOrientation() {
        return orientation;
    }

    @Override
    public void setOrientation(float orientation) {
        this.orientation = orientation;
    }

    @Override
    public float vectorToAngle(Vector2 vector) {
        return MathUtils.atan2(vector.y, vector.x);
    }

    @Override
    public Vector2 angleToVector(Vector2 outVector, float angle) {
        outVector.x = MathUtils.cos(angle);
        outVector.y = MathUtils.sin(angle);
        return outVector;
    }

    @Override
    public Location<Vector2> newLocation() {
        return new SteeringComponent();
    }

    @Override
    public Vector2 getLinearVelocity() {
        return linearVelocity;
    }

    @Override
    public float getAngularVelocity() {
        return 0f;
    }

    @Override
    public float getBoundingRadius() {
        return boundingRadius;
    }

    @Override
    public boolean isTagged() {
        return tagged;
    }

    @Override
    public void setTagged(boolean tagged) {
        this.tagged = tagged;
    }

    @Override
    public float getZeroLinearSpeedThreshold() {
        return zeroLinearSpeedThreshold;
    }

    @Override
    public void setZeroLinearSpeedThreshold(float value) {
        this.zeroLinearSpeedThreshold = value;
    }

    @Override
    public float getMaxLinearSpeed() {
        return maxLinearSpeed;
    }

    @Override
    public void setMaxLinearSpeed(float maxLinearSpeed) {
        this.maxLinearSpeed = maxLinearSpeed;
    }

    @Override
    public float getMaxLinearAcceleration() {
        return maxLinearAcceleration;
    }

    @Override
    public void setMaxLinearAcceleration(float maxLinearAcceleration) {
        this.maxLinearAcceleration = maxLinearAcceleration;
    }

    @Override
    public float getMaxAngularSpeed() {
        return maxAngularSpeed;
    }

    @Override
    public void setMaxAngularSpeed(float maxAngularSpeed) {
        this.maxAngularSpeed = maxAngularSpeed;
    }

    @Override
    public float getMaxAngularAcceleration() {
        return maxAngularAcceleration;
    }

    @Override
    public void setMaxAngularAcceleration(float maxAngularAcceleration) {
        this.maxAngularAcceleration = maxAngularAcceleration;
    }
}
