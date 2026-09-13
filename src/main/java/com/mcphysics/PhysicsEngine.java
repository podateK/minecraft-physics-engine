package com.mcphysics;

import com.mcphysics.block.BlockPhysics;
import com.mcphysics.command.PhysicsCommand;
import com.mcphysics.config.PhysicsConfig;
import com.mcphysics.explosion.ExplosionEngine;
import com.mcphysics.fluid.FluidSimulation;
import com.mcphysics.listener.PhysicsListener;
import com.mcphysics.physics.PhysicsWorld;
import com.mcphysics.projectile.ProjectilePhysics;
import com.mcphysics.renderer.PhysicsRenderer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class PhysicsEngine extends JavaPlugin {

    private PhysicsConfig physicsConfig;
    private PhysicsWorld physicsWorld;
    private ExplosionEngine explosionEngine;
    private ProjectilePhysics projectilePhysics;
    private FluidSimulation fluidSimulation;
    private BlockPhysics blockPhysics;
    private PhysicsRenderer renderer;
    private PhysicsListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        physicsConfig = new PhysicsConfig();
        physicsConfig.load(getConfig());

        physicsWorld = new PhysicsWorld(this);
        physicsConfig.applyToWorld(physicsWorld);

        explosionEngine = new ExplosionEngine(
                physicsConfig.getMaxExplosionPower(),
                physicsConfig.isBlockDamage(),
                physicsConfig.isEntityDamage(),
                physicsConfig.getShockwaveSteps()
        );

        projectilePhysics = new ProjectilePhysics(
                physicsConfig.getArrowDrag(),
                physicsConfig.getTridentDrag(),
                physicsConfig.getMaxFlightTime()
        );

        fluidSimulation = new FluidSimulation(
                physicsConfig.getMaxFlowDistance(),
                physicsConfig.getFlowSpeed(),
                physicsConfig.getLavaFlowSpeed()
        );

        blockPhysics = new BlockPhysics(
                physicsConfig.getFallSpeed(),
                physicsConfig.getBlockBreakThreshold()
        );

        renderer = new PhysicsRenderer(this);
        renderer.setDebugEnabled(physicsConfig.isDebugEnabled());
        renderer.setShowForces(physicsConfig.isShowForces());
        renderer.setShowVelocities(physicsConfig.isShowVelocities());
        renderer.setShowCollisionBoundaries(physicsConfig.isShowCollisionBoundaries());

        listener = new PhysicsListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        getCommand("physics").setExecutor(new PhysicsCommand(this));
        getCommand("physics").setTabCompleter(new PhysicsCommand(this));

        if (physicsConfig.isPhysicsEnabled()) {
            physicsWorld.start();
        }

        startSubsystemTicks();

        getLogger().info("PhysicsEngine v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (physicsWorld != null) {
            physicsWorld.stop();
        }
        if (renderer != null) {
            renderer.stop();
        }
        if (blockPhysics != null) {
            blockPhysics.clear();
        }
        if (fluidSimulation != null) {
            fluidSimulation.clear();
        }
        getLogger().info("PhysicsEngine disabled.");
    }

    private void startSubsystemTicks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!physicsConfig.isProjectileEnabled()) return;
                projectilePhysics.tickAll();
            }
        }.runTaskTimer(this, 1L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!physicsConfig.isFluidEnabled()) return;
                getServer().getWorlds().forEach(world ->
                        fluidSimulation.tick(world)
                );
            }
        }.runTaskTimer(this, 5L, 5L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!physicsConfig.isFallingBlockEnabled()) return;
                blockPhysics.tickAll();
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    public PhysicsConfig getPhysicsConfig() {
        return physicsConfig;
    }

    public PhysicsWorld getPhysicsWorld() {
        return physicsWorld;
    }

    public ExplosionEngine getExplosionEngine() {
        return explosionEngine;
    }

    public ProjectilePhysics getProjectilePhysics() {
        return projectilePhysics;
    }

    public FluidSimulation getFluidSimulation() {
        return fluidSimulation;
    }

    public BlockPhysics getBlockPhysics() {
        return blockPhysics;
    }

    public PhysicsRenderer getRenderer() {
        return renderer;
    }

    public PhysicsListener getListener() {
        return listener;
    }
}
