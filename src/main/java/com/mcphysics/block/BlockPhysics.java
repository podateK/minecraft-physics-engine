package com.mcphysics.block;

import com.mcphysics.physics.Vector3D;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockPhysics {

    private final double fallSpeed;
    private final double breakThreshold;
    private final Set<Material> affectedBlocks;
    private final ConcurrentHashMap<Long, FallingBlockState> activeFallingBlocks;

    public BlockPhysics(double fallSpeed, double breakThreshold) {
        this.fallSpeed = fallSpeed;
        this.breakThreshold = breakThreshold;
        this.affectedBlocks = EnumSet.noneOf(Material.class);
        this.activeFallingBlocks = new ConcurrentHashMap<>();
        loadDefaultBlocks();
    }

    private void loadDefaultBlocks() {
        affectedBlocks.add(Material.SAND);
        affectedBlocks.add(Material.RED_SAND);
        affectedBlocks.add(Material.GRAVEL);
        affectedBlocks.add(Material.ANVIL);
        affectedBlocks.add(Material.CHIPPED_ANVIL);
        affectedBlocks.add(Material.DAMAGED_ANVIL);
        affectedBlocks.add(Material.DRAGON_EGG);
        affectedBlocks.add(Material.SCAFFOLDING);
        affectedBlocks.add(Material.POWDER_SNOW);
    }

    public void addAffectedBlock(Material material) {
        affectedBlocks.add(material);
    }

    public void removeAffectedBlock(Material material) {
        affectedBlocks.remove(material);
    }

    public boolean isAffectedBlock(Material material) {
        return affectedBlocks.contains(material);
    }

    public FallingBlockState createFallingBlock(Block block) {
        if (!isAffectedBlock(block.getType())) return null;
        if (!canFall(block)) return null;

        Material material = block.getType();
        BlockData blockData = block.getBlockData();
        Location location = block.getLocation().add(0.5, 0, 0.5);

        block.setType(Material.AIR);

        World world = location.getWorld();
        if (world == null) return null;

        FallingBlock fallingBlock = world.spawn(
                location,
                FallingBlock.class,
                entity -> {
                    entity.setBlockData(blockData);
                    entity.setDropItem(true);
                    entity.setHurtEntities(true);
                },
                CreatureSpawnEvent.SpawnReason.CUSTOM
        );

        FallingBlockState state = new FallingBlockState(
                fallingBlock,
                new Vector3D(location.getX(), location.getY(), location.getZ()),
                new Vector3D(0, -fallSpeed, 0),
                material,
                blockData,
                0
        );

        activeFallingBlocks.put(fallingBlock.getEntityId(), state);
        return state;
    }

    public List<FallingBlockState> checkAndUpdateSupport(World world) {
        List<FallingBlockState> newFallingBlocks = new ArrayList<>();

        for (int x = world.getMinHeight(); x < world.getMaxHeight(); x++) {
            for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
                for (int z = world.getMinHeight(); z < world.getMaxHeight(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!isAffectedBlock(block.getType())) continue;
                    if (!canFall(block)) continue;
                    if (hasSupportBelow(block)) continue;

                    FallingBlockState state = createFallingBlock(block);
                    if (state != null) {
                        newFallingBlocks.add(state);
                    }
                }
            }
        }
        return newFallingBlocks;
    }

    public List<BlockUpdate> tickAll() {
        List<BlockUpdate> updates = new ArrayList<>();
        var iterator = activeFallingBlocks.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            FallingBlockState state = entry.getValue();

            if (state.entity() == null || !state.entity().isValid()) {
                iterator.remove();
                continue;
            }

            state = updateFallingBlock(state);
            if (state == null) {
                iterator.remove();
                continue;
            }
            entry.setValue(state);
        }
        return updates;
    }

    private FallingBlockState updateFallingBlock(FallingBlockState state) {
        if (state.entity() == null || !state.entity().isValid()) return null;

        Location loc = state.entity().getLocation();
        Vector3D newPos = new Vector3D(loc.getX(), loc.getY(), loc.getZ());

        Block landingBlock = getBlockBelow(loc);
        if (landingBlock != null && !landingBlock.getType().isAir() && !landingBlock.isPassable()) {
            double impactVelocity = state.velocity().getY();
            double impactForce = Math.abs(impactVelocity);

            if (impactForce > breakThreshold) {
                BlockUpdate update = new BlockUpdate(
                        landingBlock.getLocation(),
                        landingBlock.getType(),
                        landingBlock.getBlockData(),
                        BlockUpdateType.BREAK,
                        impactForce
                );
                landingBlock.breakNaturally();
            }

            Block placeBlock = loc.getBlock();
            if (placeBlock.getType().isAir() || placeBlock.isPassable()) {
                placeBlock.setType(state.material());
                placeBlock.setBlockData(state.blockData());
                BlockUpdate update = new BlockUpdate(
                        placeBlock.getLocation(),
                        state.material(),
                        state.blockData(),
                        BlockUpdateType.PLACE,
                        impactForce
                );

                state.entity().remove();
                return null;
            }

            state.entity().remove();
            return null;
        }

        return new FallingBlockState(
                state.entity(),
                newPos,
                state.velocity().add(new Vector3D(0, -0.04, 0)),
                state.material(),
                state.blockData(),
                state.ticksExisted() + 1
        );
    }

    private boolean canFall(Block block) {
        if (!isAffectedBlock(block.getType())) return false;
        Block below = getBlockBelow(block.getLocation());
        if (below == null) return false;
        return below.getType().isAir() || below.isPassable();
    }

    private boolean hasSupportBelow(Block block) {
        Block below = getBlockBelow(block.getLocation());
        if (below == null) return false;
        if (!below.getType().isAir() && !below.isPassable()) return true;
        return false;
    }

    private Block getBlockBelow(Location location) {
        World world = location.getWorld();
        if (world == null) return null;
        return world.getBlockAt(
                location.getBlockX(),
                location.getBlockY() - 1,
                location.getBlockZ()
        );
    }

    public void chainReaction(Block block, int maxDepth) {
        if (maxDepth <= 0) return;
        if (!isAffectedBlock(block.getType())) return;

        World world = block.getWorld();
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int[] offset : offsets) {
            Block neighbor = world.getBlockAt(
                    block.getX() + offset[0],
                    block.getY(),
                    block.getZ() + offset[1]
            );
            if (isAffectedBlock(neighbor.getType()) && !hasSupportBelow(neighbor)) {
                createFallingBlock(neighbor);
                chainReaction(neighbor, maxDepth - 1);
            }
        }
    }

    public int getActiveFallingBlockCount() {
        return activeFallingBlocks.size();
    }

    public void clear() {
        for (var entry : activeFallingBlocks.entrySet()) {
            FallingBlockState state = entry.getValue();
            if (state.entity() != null && state.entity().isValid()) {
                state.entity().remove();
            }
        }
        activeFallingBlocks.clear();
    }

    public record FallingBlockState(
            FallingBlock entity,
            Vector3D position,
            Vector3D velocity,
            Material material,
            BlockData blockData,
            int ticksExisted
    ) {
    }

    public record BlockUpdate(
            Location location,
            Material material,
            BlockData blockData,
            BlockUpdateType type,
            double force
    ) {
    }

    public enum BlockUpdateType {
        PLACE, BREAK, COLLAPSE
    }
}
