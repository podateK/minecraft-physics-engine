package com.mcphysics.explosion;

import com.mcphysics.physics.Vector3D;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExplosionEngine {

    private final double maxPower;
    private final boolean blockDamageEnabled;
    private final boolean entityDamageEnabled;
    private final int shockwaveSteps;
    private final Map<String, Double> blockResistance;
    private final Map<Location, Double> resistanceCache;

    public ExplosionEngine(double maxPower, boolean blockDamage, boolean entityDamage, int shockwaveSteps) {
        this.maxPower = maxPower;
        this.blockDamageEnabled = blockDamage;
        this.entityDamageEnabled = entityDamage;
        this.shockwaveSteps = shockwaveSteps;
        this.blockResistance = new HashMap<>();
        this.resistanceCache = new HashMap<>();
        loadDefaultResistances();
    }

    private void loadDefaultResistances() {
        blockResistance.put("STONE", 30.0);
        blockResistance.put("COBBLESTONE", 30.0);
        blockResistance.put("IRON_BLOCK", 60.0);
        blockResistance.put("DIAMOND_BLOCK", 100.0);
        blockResistance.put("BEDROCK", -1.0);
        blockResistance.put("OBSIDIAN", 1200.0);
        blockResistance.put("WATER", 100.0);
        blockResistance.put("LAVA", 100.0);
        blockResistance.put("DIRT", 15.0);
        blockResistance.put("SAND", 10.0);
        blockResistance.put("GRAVEL", 10.0);
        blockResistance.put("GLASS", 3.0);
        blockResistance.put("WOOD", 15.0);
        blockResistance.put("LEAVES", 2.0);
        blockResistance.put("TNT", 0.0);
    }

    public ExplosionResult detonate(Location center, double power, List<Entity> affectedEntities) {
        if (power <= 0 || center.getWorld() == null) return ExplosionResult.empty();

        double clampedPower = Math.min(power, maxPower);
        World world = center.getWorld();
        Vector3D centerVec = new Vector3D(center.getX(), center.getY(), center.getZ());

        List<BlockDestruction> destructions = new ArrayList<>();
        List<EntityImpulse> impulses = new ArrayList<>();
        List<Block> blocksToBreak = new ArrayList<>();

        int radius = (int) Math.ceil(clampedPower * 2);
        double exposureMap = clampedPower * clampedPower * 6;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = world.getBlockAt(
                            center.getBlockX() + x,
                            center.getBlockY() + y,
                            center.getBlockZ() + z
                    );

                    Vector3D blockPos = new Vector3D(
                            block.getX() + 0.5,
                            block.getY() + 0.5,
                            block.getZ() + 0.5
                    );

                    double distance = blockPos.distanceTo(centerVec);
                    if (distance > clampedPower * 2) continue;

                    double exposure = getExposure(centerVec, blockPos, world);
                    if (exposure <= 0) continue;

                    double impact = (1.0 - distance / (clampedPower * 2)) * exposure;
                    double resistance = getBlockResistance(block);
                    if (resistance < 0) continue;

                    double entropy = impact * (exposureMap / (resistance + 0.3));
                    if (entropy >= 1.0) {
                        blocksToBreak.add(block);
                        destructions.add(new BlockDestruction(block.getLocation(), block.getType(), entropy));
                    }
                }
            }
        }

        if (entityDamageEnabled) {
            for (Entity entity : affectedEntities) {
                Vector3D entityPos = new Vector3D(
                        entity.getLocation().getX(),
                        entity.getLocation().getY(),
                        entity.getLocation().getZ()
                );
                double distance = entityPos.distanceTo(centerVec);
                if (distance > clampedPower * 2) continue;

                double exposure = 1.0 - distance / (clampedPower * 2);
                double knockbackStrength = clampedPower * 10 * Math.max(0, exposure);
                double damage = clampedPower * 7 * Math.max(0, exposure);

                Vector3D direction = entityPos.subtract(centerVec).normalize();
                if (direction.magnitudeSquared() < 0.01) {
                    direction = new Vector3D(0, 1, 0);
                }
                Vector3D impulse = direction.multiply(knockbackStrength);
                impulses.add(new EntityImpulse(entity, impulse, damage));
            }
        }

        for (Block block : blocksToBreak) {
            block.breakNaturally();
        }

        Shockwave shockwave = new Shockwave(centerVec, clampedPower, shockwaveSteps);
        shockwave.propagate(world);

        return new ExplosionResult(
                center,
                clampedPower,
                destructions.size(),
                impulses.size(),
                destructions,
                impulses
        );
    }

    public double getExposure(Vector3D origin, Vector3D target, World world) {
        Vector3D direction = target.subtract(origin);
        double distance = direction.magnitude();
        if (distance < 0.01) return 1.0;
        direction = direction.divide(distance);

        int steps = (int) Math.ceil(distance * 2);
        double stepSize = distance / steps;
        double exposed = 0;
        double blocked = 0;

        for (int i = 0; i < steps; i++) {
            Vector3D point = origin.add(direction.multiply(stepSize * i));
            Block block = world.getBlockAt(
                    (int) Math.floor(point.getX()),
                    (int) Math.floor(point.getY()),
                    (int) Math.floor(point.getZ())
            );
            if (!block.isPassable() && !block.isLiquid()) {
                blocked++;
            }
        }

        exposed = (steps - blocked) / (double) steps;
        return exposed;
    }

    private double getBlockResistance(Block block) {
        String typeName = block.getType().name();
        return blockResistance.getOrDefault(typeName, 15.0);
    }

    public void setBlockResistance(String material, double resistance) {
        blockResistance.put(material, resistance);
    }

    public double getMaxPower() {
        return maxPower;
    }

    public boolean isBlockDamageEnabled() {
        return blockDamageEnabled;
    }

    public boolean isEntityDamageEnabled() {
        return entityDamageEnabled;
    }

    public record ExplosionResult(
            Location center,
            double power,
            int blocksDestroyed,
            int entitiesAffected,
            List<BlockDestruction> destructions,
            List<EntityImpulse> impulses
    ) {
        public static ExplosionResult empty() {
            return new ExplosionResult(null, 0, 0, 0, List.of(), List.of());
        }
    }

    public record BlockDestruction(Location location, org.bukkit.Material material, double entropy) {
    }

    public record EntityImpulse(Entity entity, Vector3D impulse, double damage) {
    }
}
