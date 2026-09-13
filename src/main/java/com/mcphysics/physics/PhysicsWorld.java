package com.mcphysics.physics;

import com.mcphysics.physics.collision.CollisionResolver;
import com.mcphysics.physics.gravity.GravityGenerator;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PhysicsWorld {

    private final Plugin plugin;
    private final List<RigidBody> bodies;
    private final ConcurrentHashMap<UUID, RigidBody> bodyMap;
    private final ForceRegistry forceRegistry;
    private final GravityGenerator gravityGenerator;
    private final CollisionResolver collisionResolver;
    private final double tickRate;
    private final double fixedDt;
    private final int maxBodies;
    private final double simulationRange;
    private boolean running;
    private BukkitRunnable tickTask;

    public PhysicsWorld(Plugin plugin) {
        this.plugin = plugin;
        this.bodies = new ArrayList<>();
        this.bodyMap = new ConcurrentHashMap<>();
        this.forceRegistry = new ForceRegistry();
        this.gravityGenerator = new GravityGenerator(9.81);
        this.collisionResolver = new CollisionResolver();
        this.tickRate = 20.0;
        this.fixedDt = 1.0 / tickRate;
        this.maxBodies = 500;
        this.simulationRange = 64.0;
        this.running = false;
    }

    public void start() {
        if (running) return;
        running = true;
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        };
        tickTask.runTaskTimer(plugin, 0L, 1L);
    }

    public void stop() {
        running = false;
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    public void tick() {
        applyForces();
        integrateBodies(fixedDt);
        detectAndResolveCollisions();
        syncEntities();
        pruneDeadBodies();
    }

    private void applyForces() {
        for (RigidBody body : bodies) {
            if (body.isUseGravity()) {
                gravityGenerator.applyGravity(body);
            }
        }
        forceRegistry.applyForces();
    }

    private void integrateBodies(double dt) {
        for (RigidBody body : bodies) {
            if (!body.isStatic() && !body.isKinematic()) {
                body.integrate(dt);
            }
        }
    }

    private void detectAndResolveCollisions() {
        collisionResolver.resolveMultipleCollisions(bodies);
    }

    private void syncEntities() {
        for (RigidBody body : bodies) {
            if (body.getAttachedEntity() != null) {
                body.syncWithEntity();
            }
        }
    }

    private void pruneDeadBodies() {
        bodies.removeIf(body -> !body.isAlive());
        bodyMap.values().removeIf(body -> !body.isAlive());
    }

    public RigidBody addBody(RigidBody body) {
        if (bodies.size() >= maxBodies) {
            removeOldestDynamic();
        }
        bodies.add(body);
        bodyMap.put(body.getId(), body);
        return body;
    }

    public RigidBody addBody(Vector3D position, double mass) {
        RigidBody body = new RigidBody(position, mass);
        return addBody(body);
    }

    public void removeBody(RigidBody body) {
        body.destroy();
        bodies.remove(body);
        bodyMap.remove(body.getId());
    }

    public void removeBody(UUID id) {
        RigidBody body = bodyMap.get(id);
        if (body != null) {
            removeBody(body);
        }
    }

    public void clearBodies() {
        for (RigidBody body : new ArrayList<>(bodies)) {
            body.destroy();
        }
        bodies.clear();
        bodyMap.clear();
    }

    private void removeOldestDynamic() {
        Iterator<RigidBody> it = bodies.iterator();
        while (it.hasNext()) {
            RigidBody body = it.next();
            if (!body.isStatic()) {
                removeBody(body);
                return;
            }
        }
    }

    public RigidBody getBody(UUID id) {
        return bodyMap.get(id);
    }

    public List<RigidBody> getBodiesInRange(Location center, double range) {
        Vector3D centerVec = new Vector3D(center.getX(), center.getY(), center.getZ());
        List<RigidBody> result = new ArrayList<>();
        double rangeSq = range * range;
        for (RigidBody body : bodies) {
            if (body.getPosition().distanceSquaredTo(centerVec) <= rangeSq) {
                result.add(body);
            }
        }
        return result;
    }

    public List<RigidBody> getBodiesInWorld(World world) {
        List<RigidBody> result = new ArrayList<>();
        for (RigidBody body : bodies) {
            if (body.getWorld() != null && body.getWorld().equals(world)) {
                result.add(body);
            }
        }
        return result;
    }

    public RigidBody findNearestBody(Vector3D point, double maxDistance) {
        RigidBody nearest = null;
        double nearestDistSq = maxDistance * maxDistance;
        for (RigidBody body : bodies) {
            double distSq = body.getPosition().distanceSquaredTo(point);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = body;
            }
        }
        return nearest;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public ForceRegistry getForceRegistry() {
        return forceRegistry;
    }

    public GravityGenerator getGravityGenerator() {
        return gravityGenerator;
    }

    public CollisionResolver getCollisionResolver() {
        return collisionResolver;
    }

    public int getBodyCount() {
        return bodies.size();
    }

    public boolean isRunning() {
        return running;
    }

    public double getTickRate() {
        return tickRate;
    }

    public double getFixedDt() {
        return fixedDt;
    }

    public int getMaxBodies() {
        return maxBodies;
    }

    public double getSimulationRange() {
        return simulationRange;
    }
}
