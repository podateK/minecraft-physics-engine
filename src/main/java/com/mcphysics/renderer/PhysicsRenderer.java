package com.mcphysics.renderer;

import com.mcphysics.PhysicsEngine;
import com.mcphysics.physics.RigidBody;
import com.mcphysics.physics.Vector3D;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public final class PhysicsRenderer {

    private final PhysicsEngine plugin;
    private boolean debugEnabled;
    private boolean showForces;
    private boolean showVelocities;
    private boolean showCollisionBoundaries;
    private Particle particleType;
    private BukkitRunnable renderTask;

    public PhysicsRenderer(PhysicsEngine plugin) {
        this.plugin = plugin;
        this.debugEnabled = false;
        this.showForces = false;
        this.showVelocities = false;
        this.showCollisionBoundaries = false;
        this.particleType = Particle.FLAME;
    }

    public void start() {
        if (renderTask != null) return;
        renderTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!debugEnabled) return;
                renderFrame();
            }
        };
        renderTask.runTaskTimer(plugin, 0L, 2L);
    }

    public void stop() {
        if (renderTask != null) {
            renderTask.cancel();
            renderTask = null;
        }
    }

    public void toggleDebug() {
        debugEnabled = !debugEnabled;
        if (debugEnabled) {
            start();
        }
    }

    private void renderFrame() {
        Collection<Player> players = plugin.getServer().getOnlinePlayers();
        for (Player player : players) {
            if (!player.hasPermission("physicsengine.debug")) continue;
            World world = player.getWorld();
            double range = 64.0;

            renderRigidBodies(player, world, range);
            if (showVelocities) renderVelocities(player, world, range);
            if (showCollisionBoundaries) renderCollisionBoundaries(player, world, range);
        }
    }

    private void renderRigidBodies(Player player, World world, double range) {
        for (RigidBody body : plugin.getPhysicsWorld().getBodiesInRange(player.getLocation(), range)) {
            Location pos = body.toLocation(world);
            drawPoint(world, pos, particleType, 5);
        }
    }

    private void renderVelocities(Player player, World world, double range) {
        for (RigidBody body : plugin.getPhysicsWorld().getBodiesInRange(player.getLocation(), range)) {
            if (body.isStatic()) continue;
            Vector3D vel = body.getVelocity();
            if (vel.magnitudeSquared() < 0.001) continue;

            Location start = body.toLocation(world);
            Vector3D end = body.getPosition().add(vel.multiply(5));
            Location endLoc = end.toBukkit().toLocation(world);
            drawLine(world, start, endLoc, Particle.DUST, Color.RED, 10);
        }
    }

    private void renderCollisionBoundaries(Player player, World world, double range) {
        for (RigidBody body : plugin.getPhysicsWorld().getBodiesInRange(player.getLocation(), range)) {
            if (body.getCollider() == null) continue;
            Location center = body.toLocation(world);
            double radius = body.getCollider().getRadius();
            drawSphere(world, center, radius, Particle.DUST, Color.LIME, 20);
        }
    }

    public void drawPoint(World world, Location location, Particle particle, int count) {
        world.spawnParticle(particle, location, count, 0.05, 0.05, 0.05, 0.01);
    }

    public void drawLine(World world, Location start, Location end, Particle particle, Color color, int points) {
        double dx = (end.getX() - start.getX()) / points;
        double dy = (end.getY() - start.getY()) / points;
        double dz = (end.getZ() - start.getZ()) / points;

        for (int i = 0; i <= points; i++) {
            Location point = start.clone().add(dx * i, dy * i, dz * i);
            Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f);
            world.spawnParticle(Particle.DUST, point, 1, dustOptions);
        }
    }

    public void drawSphere(World world, Location center, double radius, Particle particle, Color color, int points) {
        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f);
        for (int i = 0; i < points; i++) {
            double theta = 2.0 * Math.PI * i / points;
            for (int j = 0; j < points / 2; j++) {
                double phi = Math.PI * j / (points / 2);
                double x = radius * Math.sin(phi) * Math.cos(theta);
                double y = radius * Math.cos(phi);
                double z = radius * Math.sin(phi) * Math.sin(theta);
                Location point = center.clone().add(x, y, z);
                world.spawnParticle(Particle.DUST, point, 1, dustOptions);
            }
        }
    }

    public void drawExplosionRadius(World world, Location center, double radius) {
        drawSphere(world, center, radius, Particle.FLAME, Color.ORANGE, 30);
    }

    public void drawForceVector(World world, Location origin, Vector3D force, Color color) {
        Location end = origin.clone().add(force.multiply(0.1).toBukkit());
        drawLine(world, origin, end, Particle.DUST, color, 8);
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
        if (debugEnabled) {
            start();
        } else {
            stop();
        }
    }

    public boolean isShowForces() {
        return showForces;
    }

    public void setShowForces(boolean showForces) {
        this.showForces = showForces;
    }

    public boolean isShowVelocities() {
        return showVelocities;
    }

    public void setShowVelocities(boolean showVelocities) {
        this.showVelocities = showVelocities;
    }

    public boolean isShowCollisionBoundaries() {
        return showCollisionBoundaries;
    }

    public void setShowCollisionBoundaries(boolean showCollisionBoundaries) {
        this.showCollisionBoundaries = showCollisionBoundaries;
    }

    public Particle getParticleType() {
        return particleType;
    }

    public void setParticleType(Particle particleType) {
        this.particleType = particleType;
    }
}
