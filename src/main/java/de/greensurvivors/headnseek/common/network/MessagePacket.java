package de.greensurvivors.headnseek.common.network;

import de.greensurvivors.greensocket.network.packets.Packet;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class MessagePacket extends Packet {
    private final @NotNull Component msg;

    public MessagePacket(final @NotNull Component msg) {
        this.msg = msg;
    }

    public @NotNull Component getMsg() {
        return msg;
    }
}
