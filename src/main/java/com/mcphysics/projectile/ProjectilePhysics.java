package com.mcphysics.projectile;

import com.mcphysics.physics.Vector3D;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Trident;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ProjectilePhysics {

    private final double arrowDrag;
    private final double tridentDrag;
    private final int maxFlightTime;
    private final Map<UUID, ProjectileState> activeProjectiles;

    public ProjectilePhysics(double arrowDrag, double tridentDrag, int maxFlightTime) {
        this.arrowDrag = arrowDrag;
        this.tridentDrag = tridentDrag;
        this.maxFlightTime = maxFlightTime;
        this.activeProjectiles = new HashMap<>();
    }

    public void registerProjectile(Entity entity) {
        if (!(entity instanceof Arrow || entity instanceof Trident)) return;
        Vector velocity = entity.getVelocity();
        ProjectileType type = entity instanceof Arrow ? ProjectileType.ARROW : ProjectileType.TRIDENT;
        double drag = type == ProjectileType.ARROW ? arrowDrag : tridentDrag;

        ProjectileState state = new ProjectileState(
                type,
                new Vector3D(entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ()),
                new Vector3D(velocity.getX(), velocity.getY(), velocity.getZ()),
                drag,
                0
        );
        activeProjectiles.put(entity.getUniqueId(), state);
    }

    public void unregisterProjectile(UUID entityId) {
        activeProjectiles.remove(entityId);
    }

    public void tickAll() {
        var iterator = activeProjectiles.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            UUID id = entry.getKey();
            ProjectileState state = entry.getValue();

            state = tick(state);
            if (state == null || state.flightTime() > maxFlightTime) {
                iterator.remove();
                continue;
            }
            entry.setValue(state);
        }
    }

    public ProjectileState tick(ProjectileState state) {
        Vector3D velocity = state.velocity();
        Vector3D position = state.position();

        Vector3D gravity = new Vector3D(0, -0.05, 0);
        Vector3D dragForce = velocity.multiply(-state.drag() * velocity.magnitude());
        Vector3D acceleration = gravity.add(dragForce);

        Vector3D newVelocity = velocity.add(acceleration);
        Vector3D newPosition = position.add(newVelocity);

        return new ProjectileState(
                state.type(),
                newPosition,
                newVelocity,
                state.drag(),
                state.flightTime() + 1
        );
    }

    public ImpactResult checkImpact(Entity entity, World world) {
        Location loc = entity.getLocation();
        Vector3D position = new Vector3D(loc.getX(), loc.getY(), loc.getZ());
        Vector velocity = entity.getVelocity();
        Vector3D direction = new Vector3D(velocity.getX(), velocity.getY(), velocity.getZ());

        if (direction.magnitudeSquared() < 0.001) return null;
        direction = direction.normalize();

        Block hitBlock = entity.rayTraceBlocks(5.0).getHitBlock();
        if (hitBlock != null) {
            Vector3D hitPoint = new Vector3D(
                    hitBlock.getX() + 0.5,
                    hitBlock.getY() + 0.5,
                    hitBlock.getZ() + 0.5
            );
            Vector3D hitNormal = position.subtract(hitPoint).normalize();
            return new ImpactResult(hitPoint, hitNormal, ImpactType.BLOCK, hitBlock);
        }
        return null;
    }

    public double calculateRange(Vector3D launchVelocity, double drag) {
        Vector3D position = Vector3D.ZERO;
        Vector3D velocity = launchVelocity;
        Vector3D gravity = new Vector3D(0, -0.05, 0);
        double totalDistance = 0;

        for (int i = 0; i < maxFlightTime; i++) {
            Vector3D dragForce = velocity.multiply(-drag * velocity.magnitude());
            Vector3D acceleration = gravity.add(dragForce);
            velocity = velocity.add(acceleration);
            Vector3D newPosition = position.add(velocity);
            totalDistance += newPosition.distanceTo(position);
            position = newPosition;

            if (position.getY() < 0) break;
        }
        return totalDistance;
    }

    public Vector3D calculateLaunchVector(double angle, double speed, double yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double horizontalSpeed = speed * Math.cos(angle);
        double verticalSpeed = speed * Math.sin(angle);
        return new Vector3D(
                -Math.sin(yaw) * horizontalSpeed,
                verticalSpeed,
                Math.cos(yaw) * horizontalSpeed
        );
    }

    public Map<UUID, ProjectileState> getActiveProjectiles() {
        return activeProjectiles;
    }

    public enum ProjectileType {
        ARROW, TRIDENT
    }

    public enum ImpactType {
        BLOCK, ENTITY, NONE
    }

    public record ProjectileState(
            ProjectileType type,
            Vector3D position,
            Vector3D velocity,
            double drag,
            int flightTime
    ) {
    }

    public record ImpactResult(
            Vector3D hitPoint,
            Vector3D hitNormal,
            ImpactType impactType,
            Block hitBlock
    ) {
    }
}
