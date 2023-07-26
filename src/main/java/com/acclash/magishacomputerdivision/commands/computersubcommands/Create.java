package com.acclash.magishacomputerdivision.commands.computersubcommands;

import com.acclash.magishacomputerdivision.MagishaComputerDivision;
import com.acclash.magishacomputerdivision.commands.ComputerSubCommand;
import com.acclash.magishacomputerdivision.utils.Calculator;
import com.acclash.magishacomputerdivision.utils.Serialization;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Create extends ComputerSubCommand {

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Creates a computer of the specified type.";
    }

    @Override
    public String getSyntax() {
        return ChatColor.GOLD + "To create a computer, stand where the chair should be, face where the monitor should be, and enter: /computer create <type>";
    }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length == 1) {
            player.sendMessage(ChatColor.RED + "You need to enter some arguments.");
            player.sendMessage(getSyntax());
        } else if (args.length > 1) {
            List<String> poops = new ArrayList<>(Arrays.stream(args).toList());
            poops.remove(0);
            StringBuilder pcType = new StringBuilder();
            for (String s : poops) {
                if (poops.get(poops.size() - 1).equals(s)) {
                    pcType.append(s);
                } else {
                    pcType.append(s).append(" ");
                }
            }
            switch (pcType.toString()) {
                case "Dell Dimension L500R" -> {
                    // Preliminary checks
                    if (player.isInsideVehicle()) {
                        player.sendMessage(ChatColor.YELLOW + "You can't ride an entity while doing this.");
                        return;
                    }

                    // Start creating computer
                    Location loc = player.getLocation();
                    Location blockLoc = loc.getBlock().getLocation();
                    String blockS = Serialization.serialize(blockLoc);
                    try {
                        ResultSet resultSet = MagishaComputerDivision.getPlugin().getDB().executeQuery("SELECT * FROM `computers` WHERE block_loc = '" + blockLoc + "'");
                        if (!resultSet.next()) {

                            BlockFace direction = getPlayerDirection(player);
                            Block standingBlock = player.getLocation().getBlock();

                            //Block monitorSupportBlock = standingBlock.getRelative(direction, 2).getRelative(BlockFace.UP);
                            //if (monitorSupportBlock.getType() == Material.AIR) {
                            //    player.sendMessage(ChatColor.RED + "Unable to create computer. There would've been no block behind the monitor.");
                            //    return;
                            //}

                            // Place the stairs
                            standingBlock.setType(Material.SPRUCE_STAIRS);
                            Stairs stairs = (Stairs) standingBlock.getBlockData();
                            stairs.setFacing(getOppositeDirection(direction));
                            standingBlock.setBlockData(stairs);

                            // Create the eChair
                            Location eChairLoc = Calculator.calculateEChairLoc(blockLoc, direction);
                            LivingEntity eChair = (LivingEntity) player.getWorld().spawnEntity(eChairLoc, EntityType.CHICKEN);
                            eChair.getPersistentDataContainer().set(new NamespacedKey(MagishaComputerDivision.getPlugin(), "isEChair"), PersistentDataType.STRING, "true");
                            eChair.setRotation(Calculator.getMonitorBlockFaceAndYaw(loc.getYaw()).getInteger(), 0);
                            eChair.setAI(false);
                            eChair.setInvisible(true);
                            eChair.setInvulnerable(true);
                            eChair.setSilent(true);

                            // Place the monitor
                            Block monitorBlock = standingBlock.getRelative(direction, 1).getRelative(BlockFace.UP);
                            Location monitorLoc = monitorBlock.getLocation();
                            String monitorS = Serialization.serialize(monitorLoc);
                            ItemFrame monitor = (ItemFrame) loc.getWorld().spawnEntity(monitorLoc, EntityType.ITEM_FRAME);
                            monitor.getPersistentDataContainer().set(new NamespacedKey(MagishaComputerDivision.getPlugin(), "isMonitor"), PersistentDataType.STRING, "true");
                            monitor.setFacingDirection(getOppositeDirection(direction), true);
                            ItemStack screen = new ItemStack(Material.FILLED_MAP);
                            MapMeta screenMeta = (MapMeta) screen.getItemMeta();
                            MapView mapView = Bukkit.getServer().getMap(5);
                            screenMeta.setMapView(mapView);
                            screen.setItemMeta(screenMeta);
                            monitor.setItem(screen);

                            // Place the tower
                            Block towerBlock = monitorBlock.getRelative(getBlockFaceLeft(direction));
                            String towerS = Serialization.serialize(towerBlock.getLocation());
                            towerBlock.setType(Material.SANDSTONE_WALL);

                            // Place the pressure plate
                            String keyboardS = Serialization.serialize(monitorBlock.getLocation());
                            monitorBlock.setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);

                            // Place the button
                            Block mouseBlock = monitorBlock.getRelative(getBlockFaceRight(direction));
                            String mouseS = Serialization.serialize(mouseBlock.getLocation());
                            BlockData mouseData = Material.STONE_BUTTON.createBlockData("[face=floor]");
                            mouseBlock.setBlockData(mouseData);

                            // Save computer
                            String sql2 = "INSERT INTO computers (type, block_loc, monitor_loc, tower_loc, keyboard_loc, button_loc, block_face)" +
                                          "VALUES ('Dell Dimension l500R', '" + blockS + "', '" + monitorS + "', '" + towerS + "', '" + keyboardS + "', '" + mouseS + "', '" + direction + "')";
                            MagishaComputerDivision.getPlugin().getDB().executeUpdate(sql2);
                            String sql3 = "SELECT `id` FROM `computers` WHERE `block_loc` = '" + blockS + "'";
                            ResultSet resultSet1 = MagishaComputerDivision.getPlugin().getDB().executeQuery(sql3);
                            resultSet1.next();
                            player.sendMessage(ChatColor.GREEN + "Successfully created a Dell Dimension L500R with an ID of " + resultSet1.getInt("id"));
                        } else {
                            player.sendMessage(ChatColor.RED + "Unable to create computer. You must fully remove the old one that was in it's place first by using /computer remove");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                case "Compaq Presario" ->
                        player.sendMessage(ChatColor.YELLOW + "Unfortunately, the Compaq Presario hasn't been made yet.");
                default ->
                        player.sendMessage(ChatColor.RED + "That isn't a valid type of computer. Please check your spelling and try again.");
            }
        }

    }

    public BlockFace getPlayerDirection(Player player) {
        float yaw = player.getLocation().getYaw();

        if (yaw >= 45.1 && yaw <= 135) {
            return BlockFace.WEST;
        } else if (yaw <= 45.0 && yaw >= -44.9) {
            return BlockFace.SOUTH;
        } else if (yaw <= -45.0 && yaw >= -134.9) {
            return BlockFace.EAST;
        } else {
            return BlockFace.NORTH;
        }
    }

    public BlockFace getOppositeDirection(BlockFace blockFace) {
        return switch (blockFace) {
            case NORTH -> BlockFace.SOUTH;
            case EAST -> BlockFace.WEST;
            case SOUTH -> BlockFace.NORTH;
            default -> BlockFace.EAST;
        };
    }

    public BlockFace getBlockFaceLeft(BlockFace input) {
        return switch (input) {
            case NORTH -> BlockFace.WEST;
            case SOUTH -> BlockFace.EAST;
            case WEST -> BlockFace.SOUTH;
            default -> BlockFace.NORTH;
        };
    }

    public BlockFace getBlockFaceRight(BlockFace input) {
        return switch (input) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
    }

    public void updateItemFrame(ItemFrame itemFrame, Object data) {
        // this method will update the item in the ItemFrame with the given data
        // this is just a placeholder, you need to implement what the data is and how to use it to update the ItemFrame
        itemFrame.setItem((ItemStack) data);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> compTypes = new ArrayList<>();
            compTypes.add("Dell Dimension L500R");
            compTypes.add("Compaq Presario");

            return compTypes;
        }
        return null;
    }
}