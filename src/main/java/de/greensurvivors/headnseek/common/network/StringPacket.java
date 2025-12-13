package de.greensurvivors.headnseek.common.network;

import de.greensurvivors.greensocket.network.packets.Packet;
import org.jetbrains.annotations.NotNull;

public class StringPacket extends Packet {
    private final @NotNull String msg;

    public StringPacket(final @NotNull String msg) {
        this.msg = msg;
    }

    public @NotNull String getMsg() {
        return msg;
    }
}
