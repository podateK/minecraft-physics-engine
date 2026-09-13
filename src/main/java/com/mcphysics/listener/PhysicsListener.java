package com.mcphysics.listener;

import com.mcphysics.PhysicsEngine;
import com.mcphysics.physics.RigidBody;
import com.mcphysics.physics.Vector3D;
import com.mcphysics.physics.collision.SphereCollider;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.ArrayList;

public final class PhysicsListener implements Listener {

    private final PhysicsEngine plugin;

    public PhysicsListener(PhysicsEngine plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (!plugin.getPhysicsConfig().isFallingBlockEnabled()) return;
        Material material = event.getBlock().getType();
        if (!plugin.getBlockPhysics().isAffectedBlock(material)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!plugin.getPhysicsConfig().isProjectileEnabled()) return;
        plugin.getProjectilePhysics().registerProjectile(event.getEntity());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        plugin.getProjectilePhysics().unregisterProjectile(event.getEntity().getUniqueId());
        if (event.getHitBlock() != null) {
            Location hitLoc = event.getHitBlock().getLocation();
            plugin.getFluidSimulation().registerSource(
                    hitLoc.getBlockX(),
                    hitLoc.getBlockY(),
                    hitLoc.getBlockZ(),
                    hitLoc.getWorld() != null ? hitLoc.getWorld().getName() : ""
            );
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.getPhysicsConfig().isExplosionEnabled()) return;
        event.setCancelled(true);
        Location loc = event.getLocation();
        double power = event.getYield();
        plugin.getExplosionEngine().detonate(loc, power, new ArrayList<>(event.getEntity().getNearbyEntities(power * 2, power * 2, power * 2)));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.getPhysicsConfig().isPhysicsEnabled()) return;
        if (event.getDamager() instanceof Projectile projectile) {
            Vector3D hitPoint = new Vector3D(
                    event.getEntity().getLocation().getX(),
                    event.getEntity().getLocation().getY(),
                    event.getEntity().getLocation().getZ()
            );
            Vector3D knockback = new Vector3D(
                    projectile.getVelocity().getX() * 0.5,
                    0.2,
                    projectile.getVelocity().getZ() * 0.5
            );
            if (event.getEntity() instanceof LivingEntity living) {
                living.setVelocity(knockback.toBukkit());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getPhysicsConfig().isPhysicsEnabled()) return;
        if (event.getItem() == null) return;
        if (event.getItem().getType() == Material.FLINT_AND_STEEL) {
            Location clickedLoc = event.getClickedBlock() != null
                    ? event.getClickedBlock().getLocation().add(0.5, 1, 0.5)
                    : event.getPlayer().getEyeLocation().add(event.getPlayer().getLocation().getDirection().multiply(3));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChunkUnload(ChunkUnloadEvent event) {
        plugin.getPhysicsWorld().getBodiesInRange(
                event.getChunk().getBlock(0, 0, 0).getLocation(),
                200
        ).forEach(body -> {
            if (body.getAttachedEntity() instanceof Entity entity) {
                if (entity.getWorld().getChunkAt(entity.getLocation()).equals(event.getChunk())) {
                    plugin.getPhysicsWorld().removeBody(body);
                }
            }
        });
    }

    public void registerPhysicsEntity(Entity entity, double mass) {
        Vector3D position = new Vector3D(
                entity.getLocation().getX(),
                entity.getLocation().getY(),
                entity.getLocation().getZ()
        );
        RigidBody body = plugin.getPhysicsWorld().addBody(position, mass);
        body.setAttachedEntity(entity);
        body.setWorld(entity.getWorld());
        body.setCollider(new SphereCollider(position, 0.3));
        body.setRestitution(0.4);
        body.setFriction(0.6);
    }
}
