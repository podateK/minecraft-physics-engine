package com.mcphysics.config;

import com.mcphysics.physics.PhysicsWorld;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PhysicsConfig {

    private boolean physicsEnabled;
    private int tickRate;
    private double gravity;
    private double airResistance;
    private double groundFriction;
    private int maxRigidBodies;
    private double simulationRange;

    private boolean explosionEnabled;
    private double maxExplosionPower;
    private boolean blockDamage;
    private boolean entityDamage;
    private int shockwaveSteps;

    private boolean fluidEnabled;
    private int maxFlowDistance;
    private int flowSpeed;
    private int lavaFlowSpeed;

    private boolean projectileEnabled;
    private double arrowDrag;
    private double tridentDrag;
    private int maxFlightTime;

    private boolean fallingBlockEnabled;
    private double fallSpeed;
    private double blockBreakThreshold;
    private Set<String> affectedBlocks;

    private boolean debugEnabled;
    private boolean showForces;
    private boolean showVelocities;
    private boolean showCollisionBoundaries;

    public PhysicsConfig() {
        this.affectedBlocks = new HashSet<>();
    }

    public void load(FileConfiguration config) {
        physicsEnabled = config.getBoolean("physics.enabled", true);
        tickRate = config.getInt("physics.tick-rate", 20);
        gravity = config.getDouble("physics.gravity", -9.81);
        airResistance = config.getDouble("physics.air-resistance", 0.01);
        groundFriction = config.getDouble("physics.ground-friction", 0.3);
        maxRigidBodies = config.getInt("physics.max-rigid-bodies", 500);
        simulationRange = config.getDouble("physics.simulation-range", 64);

        explosionEnabled = config.getBoolean("explosion.enabled", true);
        maxExplosionPower = config.getDouble("explosion.max-power", 100.0);
        blockDamage = config.getBoolean("explosion.block-damage", true);
        entityDamage = config.getBoolean("explosion.entity-damage", true);
        shockwaveSteps = config.getInt("explosion.shockwave-steps", 20);

        fluidEnabled = config.getBoolean("fluid.enabled", true);
        maxFlowDistance = config.getInt("fluid.max-flow-distance", 8);
        flowSpeed = config.getInt("fluid.flow-speed", 5);
        lavaFlowSpeed = config.getInt("fluid.lava-flow-speed", 30);

        projectileEnabled = config.getBoolean("projectile.enabled", true);
        arrowDrag = config.getDouble("projectile.arrow-drag", 0.001);
        tridentDrag = config.getDouble("projectile.trident-drag", 0.0005);
        maxFlightTime = config.getInt("projectile.max-flight-time", 200);

        fallingBlockEnabled = config.getBoolean("falling-block.enabled", true);
        fallSpeed = config.getDouble("falling-block.fall-speed", 0.04);
        blockBreakThreshold = config.getDouble("falling-block.block-break-threshold", 2.5);
        affectedBlocks = new HashSet<>(config.getStringList("falling-block.affected-blocks"));

        debugEnabled = config.getBoolean("debug.enabled", false);
        showForces = config.getBoolean("debug.show-forces", false);
        showVelocities = config.getBoolean("debug.show-velocities", false);
        showCollisionBoundaries = config.getBoolean("debug.show-collision-boundaries", false);
    }

    public void applyToWorld(PhysicsWorld world) {
        world.getGravityGenerator().setStrength(Math.abs(gravity));
    }

    public boolean isPhysicsEnabled() { return physicsEnabled; }
    public int getTickRate() { return tickRate; }
    public double getGravity() { return gravity; }
    public double getAirResistance() { return airResistance; }
    public double getGroundFriction() { return groundFriction; }
    public int getMaxRigidBodies() { return maxRigidBodies; }
    public double getSimulationRange() { return simulationRange; }

    public boolean isExplosionEnabled() { return explosionEnabled; }
    public double getMaxExplosionPower() { return maxExplosionPower; }
    public boolean isBlockDamage() { return blockDamage; }
    public boolean isEntityDamage() { return entityDamage; }
    public int getShockwaveSteps() { return shockwaveSteps; }

    public boolean isFluidEnabled() { return fluidEnabled; }
    public int getMaxFlowDistance() { return maxFlowDistance; }
    public int getFlowSpeed() { return flowSpeed; }
    public int getLavaFlowSpeed() { return lavaFlowSpeed; }

    public boolean isProjectileEnabled() { return projectileEnabled; }
    public double getArrowDrag() { return arrowDrag; }
    public double getTridentDrag() { return tridentDrag; }
    public int getMaxFlightTime() { return maxFlightTime; }

    public boolean isFallingBlockEnabled() { return fallingBlockEnabled; }
    public double getFallSpeed() { return fallSpeed; }
    public double getBlockBreakThreshold() { return blockBreakThreshold; }
    public Set<String> getAffectedBlocks() { return affectedBlocks; }

    public boolean isDebugEnabled() { return debugEnabled; }
    public boolean isShowForces() { return showForces; }
    public boolean isShowVelocities() { return showVelocities; }
    public boolean isShowCollisionBoundaries() { return showCollisionBoundaries; }
}
