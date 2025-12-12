package de.greensurvivors.headnseek.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class GetCmd implements ICommand {
    @Override
    public void buildSubCmd(final @NotNull LiteralArgumentBuilder<@NotNull CommandSourceStack> cmdBuilder) {

    }
}
