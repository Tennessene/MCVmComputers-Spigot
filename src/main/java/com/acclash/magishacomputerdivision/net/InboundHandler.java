package com.acclash.magishacomputerdivision.net;

import com.acclash.magishacomputerdivision.MagishaComputerDivision;
import com.acclash.magishacomputerdivision.utils.ComputerFunctions;
import com.acclash.magishacomputerdivision.utils.NetworkUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.NoSuchElementException;
import java.util.UUID;

public class InboundHandler extends ChannelInboundHandlerAdapter {

    Entity vehicle;

    public static final String NAME = "com.acclash.magishacomputerdivision:inbound_handler";

    public final Player player;
    public final UUID playerUUID;

    public InboundHandler(Player player) {
        this.player = player;
        this.playerUUID = player.getUniqueId();
    }

    public static void attach(Player player) {
        ChannelPipeline pipe = NetworkUtil.getConnection(((CraftPlayer) player).getHandle().connection).channel.pipeline();
        detach(player);
        pipe.addBefore("packet_handler", NAME, new InboundHandler(player));
    }

    public static void detach(Player player) {
        try {
            NetworkUtil.getConnection(((CraftPlayer) player).getHandle().connection).channel.pipeline().remove(NAME);
        } catch (NoSuchElementException ignored) {}
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ServerboundPlayerInputPacket input) {
                vehicle = player.getVehicle();
            if (vehicle == null) return;
            boolean jumping = input.isJumping();
            if (jumping) {
                if (vehicle.getPersistentDataContainer().has(new NamespacedKey(MagishaComputerDivision.getPlugin(), "isEChair"), PersistentDataType.STRING)) {
                    ComputerFunctions.sendSpaceInput(player, vehicle);
                }
            }
        }
        super.channelRead(ctx, msg);
    }
}
