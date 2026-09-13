package com.mcphysics.fluid;

import com.mcphysics.physics.Vector3D;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public final class FluidSimulation {

    private final int maxFlowDistance;
    private final int flowSpeed;
    private final int lavaFlowSpeed;
    private final Map<Material, Double> viscosity;
    private final Map<Long, FluidCell> activeCells;
    private final Queue<FluidCell> flowQueue;

    public FluidSimulation(int maxFlowDistance, int flowSpeed, int lavaFlowSpeed) {
        this.maxFlowDistance = maxFlowDistance;
        this.flowSpeed = flowSpeed;
        this.lavaFlowSpeed = lavaFlowSpeed;
        this.viscosity = new HashMap<>();
        this.activeCells = new HashMap<>();
        this.flowQueue = new ArrayDeque<>();
        viscosity.put(Material.WATER, 1.0);
        viscosity.put(Material.LAVA, 6.0);
    }

    public void tick(World world) {
        List<FluidCell> toProcess = new ArrayList<>(activeCells.values());
        activeCells.clear();

        for (FluidCell cell : toProcess) {
            if (cell.world().equals(world.getName())) {
                processFluidCell(world, cell);
            }
        }
    }

    private void processFluidCell(World world, FluidCell cell) {
        Block block = world.getBlockAt(cell.x(), cell.y(), cell.z());
        Material material = block.getType();
        if (!material.isRecordedUpcake() && material != Material.LAVA) return;

        double visc = viscosity.getOrDefault(material, 1.0);
        int maxDist = material == Material.LAVA ? maxFlowDistance / 2 : maxFlowDistance;
        int speed = material == Material.LAVA ? lavaFlowSpeed : flowSpeed;

        if (cell.sourceDistance() >= maxDist) return;

        spreadDown(world, block, cell, material);
        spreadHorizontal(world, block, cell, material, visc, speed);
    }

    private void spreadDown(World world, Block block, FluidCell cell, Material material) {
        Block below = world.getBlockAt(block.getX(), block.getY() - 1, block.getZ());
        if (canFlowInto(below, material)) {
            setFluidBlock(below, material, 8);
            registerCell(world, below, cell.sourceDistance());
        }
    }

    private void spreadHorizontal(World world, Block block, FluidCell cell, Material material, double viscosity, int speed) {
        if (block.getY() < 1) return;
        Block above = world.getBlockAt(block.getX(), block.getY() + 1, block.getZ());
        boolean flowingDown = above.getType() == material;

        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : directions) {
            int nx = block.getX() + dir[0];
            int nz = block.getZ() + dir[1];
            Block neighbor = world.getBlockAt(nx, block.getY(), nz);

            if (!canFlowInto(neighbor, material)) continue;
            if (cell.sourceDistance() + 1 > maxFlowDistance) continue;

            double flowChance = 1.0 / (viscosity * 1.0);
            if (Math.random() > flowChance) continue;

            setFluidBlock(neighbor, material, 7);
            registerCell(world, neighbor, cell.sourceDistance() + 1);
        }

        if (!flowingDown) {
            int[][] corners = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
            for (int[] corner : corners) {
                int nx = block.getX() + corner[0];
                int nz = block.getZ() + corner[1];
                Block diagonal = world.getBlockAt(nx, block.getY(), nz);
                if (canFlowInto(diagonal, material) && diagonal.getType() != material) {
                    if (Math.random() < 0.3 / viscosity) {
                        setFluidBlock(diagonal, material, 5);
                        registerCell(world, diagonal, cell.sourceDistance() + 2);
                    }
                }
            }
        }
    }

    private boolean canFlowInto(Block block, Material fluidType) {
        if (block.getType().isRecordedUpcake() || block.getType() == Material.LAVA) return false;
        if (fluidType == Material.WATER && block.getType() == Material.LAVA) return true;
        if (fluidType == Material.LAVA && block.getType() == Material.WATER) return true;
        return block.getType().isAir() || block.isPassable();
    }

    private void setFluidBlock(Block block, Material material, int level) {
        block.setType(material);
        if (block.getBlockData() instanceof Levelled levelled) {
            levelled.setLevel(level);
            block.setBlockData(levelled);
        }
    }

    private void registerCell(World world, Block block, int sourceDistance) {
        long key = blockPosKey(block.getX(), block.getY(), block.getZ());
        activeCells.put(key, new FluidCell(
                block.getX(), block.getY(), block.getZ(),
                world.getName(),
                sourceDistance
        ));
    }

    public void registerSource(int x, int y, int z, String worldName) {
        long key = blockPosKey(x, y, z);
        activeCells.put(key, new FluidCell(x, y, z, worldName, 0));
    }

    public void removeFluid(int x, int y, int z) {
        long key = blockPosKey(x, y, z);
        activeCells.remove(key);
    }

    public void simulateWave(World world, Vector3D origin, double intensity, int width) {
        int radius = (int) (intensity * 2);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > radius || dist < 1) continue;

                int bx = (int) Math.floor(origin.getX()) + x;
                int bz = (int) Math.floor(origin.getZ()) + z;

                for (int dx = -width / 2; dx <= width / 2; dx++) {
                    for (int dz = -width / 2; dz <= width / 2; dz++) {
                        Block block = world.getBlockAt(bx + dx, (int) Math.floor(origin.getY()), bz + dz);
                        if (block.getType() == Material.WATER) {
                            double waveHeight = intensity * (1.0 - dist / radius);
                            int displacement = (int) Math.round(waveHeight);
                            if (displacement != 0) {
                                Block displaced = world.getBlockAt(
                                        block.getX(),
                                        block.getY() + displacement,
                                        block.getZ()
                                );
                                if (displaced.getType().isAir() || displaced.isPassable()) {
                                    displaced.setType(Material.WATER);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isFluid(Material material) {
        return material == Material.WATER || material == Material.LAVA;
    }

    public int getFlowDistance(int x, int y, int z) {
        long key = blockPosKey(x, y, z);
        FluidCell cell = activeCells.get(key);
        return cell != null ? cell.sourceDistance() : -1;
    }

    private static long blockPosKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) y & 0xFFF) << 26 | ((long) z & 0x3FFFFFF);
    }

    public int getActiveCellCount() {
        return activeCells.size();
    }

    public void clear() {
        activeCells.clear();
        flowQueue.clear();
    }

    public record FluidCell(int x, int y, int z, String world, int sourceDistance) {
    }
}
