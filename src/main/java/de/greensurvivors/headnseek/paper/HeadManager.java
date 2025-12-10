package de.greensurvivors.headnseek.paper;

import de.greensurvivors.headnseek.paper.language.PlaceHolderKey;
import de.greensurvivors.headnseek.paper.language.TranslationKey;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HeadManager implements Listener {
    private final @NotNull HeadNSeek plugin;
    private final @NotNull NamespacedKey numberKey;
    private final @NotNull NamespacedKey wasFoundKey;

    public HeadManager(@NotNull HeadNSeek plugin) {
        this.plugin = plugin;
        this.numberKey = new NamespacedKey(plugin, "headnseeknum");
        this.wasFoundKey = new NamespacedKey(plugin, "wasFound");

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    private void onBlockBreak (final @NotNull BlockBreakEvent event) {
        final @NotNull Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) { // team

            if (event.getBlock().getState(false) instanceof Skull playerHead) {
                final @NotNull PersistentDataContainer persistentDataContainer = playerHead.getPersistentDataContainer();
                final @Nullable Integer headNumber = persistentDataContainer.get(numberKey, PersistentDataType.INTEGER);

                if (headNumber != null) {
                    final @Nullable Boolean wasFound = persistentDataContainer.get(wasFoundKey, PersistentDataType.BOOLEAN);

                    if (wasFound == null || wasFound) {
                        if (player.hasPermission(PermissionWrapper.ACTION_FIND_HEAD.getPermission())) {
                            persistentDataContainer.set(wasFoundKey, PersistentDataType.BOOLEAN, Boolean.TRUE);

                            plugin.getSocialAdapter().sendMessage(plugin.getMessageManager().getLang(
                                TranslationKey.SOCIAL_MESSAGE,
                                Placeholder.component(PlaceHolderKey.PLAYER.getKey(), player.displayName()),
                                Formatter.number(PlaceHolderKey.NUMBER.getKey(), headNumber)
                                )
                            );
                        } else {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }
}
