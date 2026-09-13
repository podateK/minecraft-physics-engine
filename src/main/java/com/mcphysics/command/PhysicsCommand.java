package com.mcphysics.command;

import com.mcphysics.PhysicsEngine;
import com.mcphysics.physics.Vector3D;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PhysicsCommand implements CommandExecutor, TabCompleter {

    private final PhysicsEngine plugin;

    public PhysicsCommand(PhysicsEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("physicsengine.command")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "toggle" -> handleToggle(sender);
            case "status" -> handleStatus(sender);
            case "debug" -> handleDebug(sender, args);
            case "spawn" -> handleSpawn(sender, args);
            case "explosion" -> handleExplosion(sender, args);
            case "gravity" -> handleGravity(sender, args);
            case "clear" -> handleClear(sender);
            case "help" -> sendHelp(sender);
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /physics help");
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getPhysicsConfig().load(plugin.getConfig());
        sender.sendMessage(ChatColor.GREEN + "PhysicsEngine configuration reloaded.");
    }

    private void handleToggle(CommandSender sender) {
        if (plugin.getPhysicsWorld().isRunning()) {
            plugin.getPhysicsWorld().stop();
            sender.sendMessage(ChatColor.YELLOW + "PhysicsEngine stopped.");
        } else {
            plugin.getPhysicsWorld().start();
            sender.sendMessage(ChatColor.GREEN + "PhysicsEngine started.");
        }
    }

    private void handleStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- PhysicsEngine Status ---");
        sender.sendMessage(ChatColor.WHITE + "Running: " + (plugin.getPhysicsWorld().isRunning() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        sender.sendMessage(ChatColor.WHITE + "Bodies: " + ChatColor.AQUA + plugin.getPhysicsWorld().getBodyCount());
        sender.sendMessage(ChatColor.WHITE + "Tick Rate: " + ChatColor.AQUA + plugin.getPhysicsWorld().getTickRate());
        sender.sendMessage(ChatColor.WHITE + "Gravity: " + ChatColor.AQUA + plugin.getPhysicsWorld().getGravityGenerator().getStrength());
        sender.sendMessage(ChatColor.WHITE + "Debug: " + (plugin.getRenderer().isDebugEnabled() ? ChatColor.GREEN + "On" : ChatColor.RED + "Off"));
    }

    private void handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("physicsengine.debug")) {
            sender.sendMessage(ChatColor.RED + "No permission for debug.");
            return;
        }
        plugin.getRenderer().toggleDebug();
        sender.sendMessage(ChatColor.AQUA + "Debug visualization: " +
                (plugin.getRenderer().isDebugEnabled() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return;
        }

        double mass = args.length > 1 ? parseDouble(args[1], 1.0) : 1.0;
        Vector3D position = new Vector3D(
                player.getLocation().getX(),
                player.getLocation().getY() + 2,
                player.getLocation().getZ()
        );

        var body = plugin.getPhysicsWorld().addBody(position, mass);
        body.setWorld(player.getWorld());
        body.setCollider(new com.mcphysics.physics.collision.SphereCollider(position, 0.5));
        sender.sendMessage(ChatColor.GREEN + "Spawned rigid body (mass: " + mass + ")");
    }

    private void handleExplosion(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return;
        }

        double power = args.length > 1 ? parseDouble(args[1], 4.0) : 4.0;
        plugin.getExplosionEngine().detonate(
                player.getLocation(),
                power,
                new ArrayList<>(player.getNearbyEntities(power * 2, power * 2, power * 2))
        );
        sender.sendMessage(ChatColor.RED + "Explosion detonated! (power: " + power + ")");
    }

    private void handleGravity(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.WHITE + "Gravity: " + ChatColor.AQUA + plugin.getPhysicsWorld().getGravityGenerator().getStrength());
            return;
        }
        double strength = parseDouble(args[1], 9.81);
        plugin.getPhysicsWorld().getGravityGenerator().setStrength(strength);
        sender.sendMessage(ChatColor.GREEN + "Gravity set to " + strength);
    }

    private void handleClear(CommandSender sender) {
        plugin.getPhysicsWorld().clearBodies();
        plugin.getBlockPhysics().clear();
        plugin.getFluidSimulation().clear();
        sender.sendMessage(ChatColor.GREEN + "All physics objects cleared.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- PhysicsEngine Commands ---");
        sender.sendMessage(ChatColor.YELLOW + "/physics reload" + ChatColor.WHITE + " - Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/physics toggle" + ChatColor.WHITE + " - Toggle physics engine");
        sender.sendMessage(ChatColor.YELLOW + "/physics status" + ChatColor.WHITE + " - Show engine status");
        sender.sendMessage(ChatColor.YELLOW + "/physics debug" + ChatColor.WHITE + " - Toggle debug visualization");
        sender.sendMessage(ChatColor.YELLOW + "/physics spawn [mass]" + ChatColor.WHITE + " - Spawn a rigid body");
        sender.sendMessage(ChatColor.YELLOW + "/physics explosion [power]" + ChatColor.WHITE + " - Detonate explosion");
        sender.sendMessage(ChatColor.YELLOW + "/physics gravity [strength]" + ChatColor.WHITE + " - Set gravity");
        sender.sendMessage(ChatColor.YELLOW + "/physics clear" + ChatColor.WHITE + " - Clear all physics objects");
        sender.sendMessage(ChatColor.YELLOW + "/physics help" + ChatColor.WHITE + " - Show this help");
    }

    private double parseDouble(String s, double defaultValue) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("physicsengine.command")) return List.of();

        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("reload", "toggle", "status", "debug", "spawn", "explosion", "gravity", "clear", "help"), args[0]);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "spawn" -> List.of("1.0", "5.0", "10.0", "50.0");
                case "explosion" -> List.of("1.0", "4.0", "8.0", "16.0");
                case "gravity" -> List.of("0.0", "9.81", "20.0", "50.0");
                default -> List.of();
            };
        }

        return List.of();
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
