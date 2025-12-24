package de.greensurvivors.headnseek.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import de.greensurvivors.headnseek.paper.HeadNSeek;
import de.greensurvivors.headnseek.paper.PermissionWrapper;
import de.greensurvivors.headnseek.paper.language.PlaceHolderKey;
import de.greensurvivors.headnseek.paper.language.TranslationKey;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GetCmd extends ACommand {
    private final static DynamicCommandExceptionType NUMBER_NOT_CONFIGURED = new DynamicCommandExceptionType(input ->
        MessageComponentSerializer.message().serialize(Component.text("Head number " + input + " was not configured!")));

    public GetCmd(final @NotNull HeadNSeek plugin) {
        super(plugin);
    }

    @Override
    public void buildSubCmd(final @NotNull LiteralArgumentBuilder<@NotNull CommandSourceStack> cmdBuilder) {
        cmdBuilder.then(Commands.literal(getLiteral())
            .requires(stack -> {
                final @NotNull CommandSender sender = stack.getSender();
                return sender instanceof Player && sender.hasPermission(PermissionWrapper.CMD_GET.getPermission());
            }).then(Commands.argument("head number", IntegerArgumentType.integer(1))
                .suggests((context, suggestionsBuilder) -> {
                    for (final int headNum : plugin.getHeadManager().getHeadNumbers()) {
                        final @NotNull String headNumStr = String.valueOf(headNum);

                        if (headNumStr.startsWith(suggestionsBuilder.getRemaining())) {
                            suggestionsBuilder.suggest(headNum);
                        }
                    }

                    return suggestionsBuilder.buildFuture();
                }).executes(context -> {
                    final int number = IntegerArgumentType.getInteger(context, "head number");

                    final @NotNull ItemStack stack = plugin.getHeadManager().getHead(number);
                    if (stack.isEmpty()) {
                        throw NUMBER_NOT_CONFIGURED.create(number);
                    } else {
                        final @NotNull Player player = ((Player) context.getSource().getSender());

                        player.give(List.of(stack), true);
                        plugin.getMessageManager().sendLang(player, TranslationKey.CMD_GET_SUCCESS,
                            Formatter.number(PlaceHolderKey.NUMBER.getKey(), number));
                    }

                    return Command.SINGLE_SUCCESS;
                })
            ));
    }

    @Override
    protected @Nullable Permission getPermission() {
        return PermissionWrapper.CMD_GET.getPermission();
    }

    @Override
    protected @NotNull String getLiteral() {
        return "get";
    }
}
