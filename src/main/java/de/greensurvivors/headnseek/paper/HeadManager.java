package de.greensurvivors.headnseek.paper;

import com.google.gson.*;
import de.greensurvivors.headnseek.paper.language.PlaceHolderKey;
import de.greensurvivors.headnseek.paper.language.TranslationKey;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.persistence.PersistentDataContainerView;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Skull;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class HeadManager implements Listener {
    private final @NotNull HeadNSeek plugin;
    private final @NotNull NamespacedKey numberKey;
    // Note: The lore already gets saved on the tile entity but not kept when breaking it.
    // There is currently no good way in the api to retrieve it.
    // So we're doing double work here, to "bugfix" around mojang.
    private final @NotNull NamespacedKey loreKey;

    private final @NotNull Int2ObjectOpenHashMap<@NotNull ItemStack> heads = new Int2ObjectOpenHashMap<>();

    public HeadManager(final @NotNull HeadNSeek plugin) {
        this.plugin = plugin;
        numberKey = new NamespacedKey(plugin, "headnum");
        loreKey = new NamespacedKey(plugin, "lore");

        heads.defaultReturnValue(ItemStack.empty());
    }

    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void reload() { // todo

    }

    public @NotNull ItemStack setHead(final @Range(from = 1, to = 8) int number, final @NotNull ItemStack stack) {
        final @NotNull ItemStack clone = stack.clone();

        clone.editPersistentDataContainer(persistentDataContainer ->
            persistentDataContainer.set(numberKey, PersistentDataType.INTEGER, number));

        // todo safe to disk

        return this.heads.put(number, clone);
    }

    public @NotNull ItemStack getHead(final @Range(from = 1, to = 8) int number) {
        return heads.get(number);
    }

    /**
     * Tile entities do not retain the persistent data container used by this plugin when getting placed.
     * Also, they do save the lore but, it gets discarded when breaking the block and getting the item.
     * (why??)
     * So we just save and load it ourselves.
     */
    @EventHandler(ignoreCancelled = true)
    private void onBlockPlace(final @NotNull BlockPlaceEvent event) {
        final @NotNull ItemStack itemInHand = event.getItemInHand();

        if (itemInHand.getType() == Registry.MATERIAL.get(ItemType.PLAYER_HEAD.key())) {
            final @NotNull PersistentDataContainerView persistentDataContainerViewItem = itemInHand.getPersistentDataContainer();
            final @Nullable Integer headNumber = persistentDataContainerViewItem.get(numberKey, PersistentDataType.INTEGER);

            if (headNumber != null) {
                if (event.getBlockPlaced().getState(false) instanceof final @NotNull Skull playerHeadBlockState) {
                    final @NotNull PersistentDataContainer persistentDataContainerBockState = playerHeadBlockState.getPersistentDataContainer();

                    persistentDataContainerBockState.set(numberKey, PersistentDataType.INTEGER, headNumber);

                    final @Nullable List<@NotNull Component> lore = itemInHand.lore();
                    if (lore != null) {
                        final @NotNull JsonArray listObj = new JsonArray();

                        for (Component loreLine : lore) {
                            listObj.add(GsonComponentSerializer.gson().serializeToTree(loreLine));
                        }

                        persistentDataContainerBockState.set(loreKey, PersistentDataType.STRING, listObj.toString());
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onBlockBreak(final @NotNull BlockDropItemEvent event) {
        final @NotNull Player player = event.getPlayer();
        if (event.getBlock().getState(false) instanceof Skull playerHead) {
            final @Nullable Integer headNumber = playerHead.getPersistentDataContainer().get(numberKey, PersistentDataType.INTEGER);

            if (headNumber != null) {
                if (!player.hasPermission(PermissionWrapper.ACTION_FIND_HEAD.getPermission())) {
                    event.setCancelled(true);
                    // todo message
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onBlockDropItem(final @NotNull BlockDropItemEvent event) {
        final @NotNull Player player = event.getPlayer();

        if (event.getBlockState() instanceof Skull playerHead) {
            final @NotNull PersistentDataContainer blockPersistentDataContainer = playerHead.getPersistentDataContainer();
            final @Nullable Integer headNumber = blockPersistentDataContainer.get(numberKey, PersistentDataType.INTEGER);

            if (headNumber != null) {
                try {
                    restoreLore(event, blockPersistentDataContainer);
                } catch (JsonParseException | IllegalStateException e) {
                    plugin.getComponentLogger().warn("Could not parse lore for head number " + headNumber +
                        " at " + event.getBlock().getLocation() + " broken by " + player.name() +
                        " (" + player.getUniqueId() + ").", e);
                }

                plugin.getSocialAdapter().sendMessage(plugin.getMessageManager().getLang(
                    TranslationKey.SOCIAL_MESSAGE,
                    Placeholder.component(PlaceHolderKey.PLAYER.getKey(), player.displayName()),
                    // todo item name??
                    Formatter.number(PlaceHolderKey.NUMBER.getKey(), headNumber)
                ));
            }
        }
    }

    private void restoreLore(@NonNull BlockDropItemEvent event, @NonNull PersistentDataContainer blockPersistentDataContainer) throws JsonParseException, IllegalStateException {
        final @Nullable String jsonLoreStr = blockPersistentDataContainer.get(loreKey, PersistentDataType.STRING);
        final @Nullable List<@NotNull Component> lore;

        if (jsonLoreStr != null) {
            final @NotNull JsonArray jsonArray = JsonParser.parseString(jsonLoreStr).getAsJsonArray();
            lore = new ArrayList<>();

            for (JsonElement jsonElement : jsonArray) {
                lore.add(GsonComponentSerializer.gson().deserializeFromTree(jsonElement));
            }

            for (final @NotNull Item item : event.getItems()) {
                final @NotNull ItemStack itemStack = item.getItemStack();

                if (itemStack.getType() == Registry.MATERIAL.get(ItemType.PLAYER_HEAD.key())) {
                    itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
                }
            }
        }
    }
}
