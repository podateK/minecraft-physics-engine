package com.mcphysics.explosion;

import com.mcphysics.physics.Vector3D;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public final class Shockwave {

    private final Vector3D origin;
    private final double power;
    private final int steps;
    private final double maxRadius;

    public Shockwave(Vector3D origin, double power, int steps) {
        this.origin = origin;
        this.power = power;
        this.steps = steps;
        this.maxRadius = power * 2;
    }

    public void propagate(World world) {
        if (world == null) return;
        double stepSize = maxRadius / steps;
        double timeStep = 0.05;
        double currentRadius = 0;
        double shockwaveSpeed = 20.0;
        double shockwaveThickness = 2.0;

        for (int step = 0; step < steps; step++) {
            currentRadius += stepSize;
            if (currentRadius > maxRadius) break;

            double pressure = calculatePressure(currentRadius);
            if (pressure < 0.01) continue;

            applyKnockbackAtRadius(world, currentRadius, pressure);
            spawnShockwaveParticles(world, currentRadius, pressure);
        }
    }

    private double calculatePressure(double distance) {
        if (distance <= 0) return power;
        double normalizedDistance = distance / maxRadius;
        return power * Math.exp(-3.0 * normalizedDistance) / (1.0 + distance * 0.1);
    }

    private void applyKnockbackAtRadius(World world, double radius, double pressure) {
        int blockRadius = (int) Math.ceil(radius);
        Vector3D originBlock = new Vector3D(
                origin.getX(),
                origin.getY(),
                origin.getZ()
        );

        for (int x = -blockRadius; x <= blockRadius; x++) {
            for (int y = -blockRadius; y <= blockRadius; y++) {
                for (int z = -blockRadius; z <= blockRadius; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (Math.abs(dist - radius) > 1.5) continue;

                    Vector3D point = originBlock.add(new Vector3D(x, y, z));
                    double falloff = 1.0 - Math.abs(dist - radius) / 1.5;
                    double wavePressure = pressure * falloff;

                    applyWaveForce(world, point, wavePressure);
                }
            }
        }
    }

    private void applyWaveForce(World world, Vector3D point, double pressure) {
        Block block = world.getBlockAt(
                (int) Math.floor(point.getX()),
                (int) Math.floor(point.getY()),
                (int) Math.floor(point.getZ())
        );

        if (block.isPassable()) return;
        if (pressure > 30.0) {
            double destroyChance = (pressure - 30.0) / 100.0;
            if (Math.random() < destroyChance) {
                block.breakNaturally();
            }
        }
    }

    private void spawnShockwaveParticles(World world, double radius, double pressure) {
        if (pressure < 0.1) return;
        int particleCount = (int) (pressure * 2);
        double angleStep = Math.PI * 2 / Math.max(1, particleCount);

        for (int i = 0; i < particleCount; i++) {
            double angle = angleStep * i;
            double px = origin.getX() + Math.cos(angle) * radius;
            double pz = origin.getZ() + Math.sin(angle) * radius;

            Vector3D particlePos = new Vector3D(px, origin.getY(), pz);
            world.spawnParticle(
                    Particle.SMOKE,
                    particlePos.getX(),
                    particlePos.getY(),
                    particlePos.getZ(),
                    3,
                    0.1,
                    0.1,
                    0.1,
                    0.02
            );
        }
    }

    public Vector3D getOrigin() {
        return origin;
    }

    public double getPower() {
        return power;
    }

    public double getMaxRadius() {
        return maxRadius;
    }

    public int getSteps() {
        return steps;
    }
}
