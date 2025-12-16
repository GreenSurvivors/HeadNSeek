package de.greensurvivors.headnseek.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.headnseek.paper.HeadNSeek;
import de.greensurvivors.headnseek.paper.PermissionWrapper;
import de.greensurvivors.headnseek.paper.language.TranslationKey;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefineBoardCmd extends ACommand {

    public DefineBoardCmd(final @NotNull HeadNSeek plugin) {
        super(plugin);
    }

    @Override
    public void buildSubCmd(final @NotNull LiteralArgumentBuilder<@NotNull CommandSourceStack> cmdBuilder) {
        final @NotNull LiteralCommandNode<@NotNull CommandSourceStack> subRoot = Commands.literal(getLiteral())
            .requires(stack -> {
                final @NotNull CommandSender sender = stack.getSender();
                return sender instanceof Player && sender.hasPermission(PermissionWrapper.CMD_DEFINE_BOARD.getPermission());
            }).executes(context -> {
                final @NotNull Player player = (Player) context.getSource().getSender();

                plugin.getBoardManager().registerForDefining(player.getUniqueId(), false);
                plugin.getMessageManager().sendLang(player, TranslationKey.CMD_DEFINE_BOARD_START);

                return Command.SINGLE_SUCCESS;
            }).then(Commands.argument("should update overlapping boards", BoolArgumentType.bool())
                .executes(context -> {
                    final @NotNull Boolean updateOld = BoolArgumentType.getBool(context, "should update overlapping boards");
                    final @NotNull Player player = (Player) context.getSource().getSender();

                    plugin.getBoardManager().registerForDefining(player.getUniqueId(), updateOld);
                    plugin.getMessageManager().sendLang(player, TranslationKey.CMD_DEFINE_BOARD_START);

                    return Command.SINGLE_SUCCESS;
                })
            ).build();

        cmdBuilder.then(subRoot);
        cmdBuilder.then(Commands.literal("defbrd").redirect(subRoot));
    }

    @Override
    protected @Nullable Permission getPermission() {
        return PermissionWrapper.CMD_DEFINE_BOARD.getPermission();
    }

    @Override
    protected @NotNull String getLiteral() {
        return "define_board";
    }
}
