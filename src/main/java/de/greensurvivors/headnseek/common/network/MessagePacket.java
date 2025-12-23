package de.greensurvivors.headnseek.common.network;

import de.greensurvivors.greensocket.network.packets.Packet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

public class MessagePacket extends Packet {
    private final @NotNull String msg;

    public MessagePacket(final @NotNull Component msg) {
        this.msg = MiniMessage.miniMessage().serialize(msg);
    }

    public @NotNull String getMsg() {
        return msg;
    }
}
