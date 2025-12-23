package de.greensurvivors.headnseek.paper;

import com.google.gson.*;
import de.greensurvivors.headnseek.paper.language.PlaceHolderKey;
import de.greensurvivors.headnseek.paper.language.TranslationKey;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.persistence.PersistentDataContainerView;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Skull;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class HeadManager implements Listener {
    protected final @NotNull HeadNSeek plugin;
    protected final @NotNull NamespacedKey numberKey;
    // Note: The lore already gets saved on the tile entity but not kept when breaking it.
    // There is currently no good way in the api to retrieve it.
    // So we're doing double work here, to "bugfix" around mojang.
    protected final @NotNull NamespacedKey loreKey;
    protected final @NotNull Path headConfigPath;

    protected final @NotNull Int2ObjectOpenHashMap<@NotNull ItemStack> heads = new Int2ObjectOpenHashMap<>();
    protected final @NotNull FileConfiguration headConfig = new YamlConfiguration();

    public HeadManager(final @NotNull HeadNSeek plugin) {
        this.plugin = plugin;
        numberKey = new NamespacedKey(plugin, "headnum");
        loreKey = new NamespacedKey(plugin, "lore");
        headConfigPath = plugin.getDataPath().resolve("headStorage.yml");

        heads.defaultReturnValue(ItemStack.empty());
        headConfig.options().parseComments(true);
    }

    public void reload() {
        heads.clear();

        if (Files.exists(headConfigPath)) {
            try {
                headConfig.load(Files.newBufferedReader(headConfigPath));

                for (final @NotNull Map.Entry<@NotNull String, ?> entry : headConfig.getValues(false).entrySet()) {
                    try {
                        final int headNumber = Integer.parseInt(entry.getKey());

                        if (entry.getValue() instanceof final @NotNull String base64Str) {
                            heads.put(headNumber, ItemStack.deserializeBytes(Base64.getDecoder().decode(base64Str)));
                        } else {
                            plugin.getComponentLogger().warn("Could not read head number " + headNumber + ", because " + entry.getValue() + " is not a string! Ignoring.");
                        }
                    } catch (final @NotNull NumberFormatException e) {
                        plugin.getComponentLogger().warn("Couldn't load head, because " + entry.getKey() + " is not a integer. Ignoring.", e);
                    }
                }
            } catch (IOException | InvalidConfigurationException e) {
                plugin.getComponentLogger().error("Couldn't load head storage file!", e);
            }
        }
    }

    /// DOES modify the given item by setting the persistent data container head number.
    /// everything else happens with a clone
    /// @return the ItemStack PREVIOUSLY attached to the head number. Might be empty if there wasn't any configured yet
    public @NotNull ItemStack configureHead(final @Range(from = 1, to = Integer.MAX_VALUE) int number, final @NotNull ItemStack stack) {
        stack.editPersistentDataContainer(persistentDataContainer ->
            persistentDataContainer.set(numberKey, PersistentDataType.INTEGER, number));
        final @NotNull ItemStack clone = stack.clone();
        clone.setAmount(1);

        headConfig.set(String.valueOf(number), Base64.getEncoder().encodeToString(clone.serializeAsBytes()));

        try (final @NotNull Writer writer = Files.newBufferedWriter(headConfigPath, StandardCharsets.UTF_8)) {
            writer.write(headConfig.saveToString());
        } catch (final @NotNull IOException e) {
            plugin.getComponentLogger().error("Could not safe head file to disk. Data loss is imminent!", e);
        }

        return this.heads.put(number, clone);
    }

    /// @return a clone of the ItemStack associated with the given number. Might be empty if non was found
    public @NotNull ItemStack getHead(final @Range(from = 1, to = Integer.MAX_VALUE) int number) {
        final @NotNull ItemStack stack = heads.get(number);
        return stack.isEmpty() ? stack : stack.clone();
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

                    plugin.getServer().broadcast(plugin.getMessageManager().getLang(TranslationKey.ACTION_PLACE_HEAD_BROADCAST,
                        Placeholder.component(PlaceHolderKey.PLAYER_NAME.getKey(), event.getPlayer().displayName()),
                        Formatter.number(PlaceHolderKey.NUMBER.getKey(), headNumber)
                        ), PermissionWrapper.MESSAGE_PLACE_HEAD_BROADCAST.getPermission().getName());
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

                    plugin.getMessageManager().sendLang(player, TranslationKey.ERROR_NO_PERMISSION);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onBlockDropItem(final @NotNull BlockDropItemEvent event) {
        final @NotNull Player player = event.getPlayer();

        if (player.getGameMode() != GameMode.CREATIVE && event.getBlockState() instanceof final Skull playerHead) {
            final @NotNull PersistentDataContainer blockPersistentDataContainer = playerHead.getPersistentDataContainer();
            final @Nullable Integer headNumber = blockPersistentDataContainer.get(numberKey, PersistentDataType.INTEGER);

            if (headNumber != null) {
                try {
                    restoreLore(event, blockPersistentDataContainer);
                } catch (final @NotNull JsonParseException | IllegalStateException e) {
                    plugin.getComponentLogger().warn("Could not parse lore for head number " + headNumber +
                        " at " + event.getBlock().getLocation() + " broken by " + player.name() +
                        " (" + player.getUniqueId() + ").", e);
                }

                plugin.getMessageManager().sendLang(player, TranslationKey.ACTION_FOUND,
                    Formatter.number(PlaceHolderKey.NUMBER.getKey(), headNumber));
                plugin.getSocialAdapter().sendMessage(plugin.getMessageManager().getLang(
                    TranslationKey.SOCIAL_MESSAGE_FOUND,
                    Placeholder.component(PlaceHolderKey.PLAYER_NAME.getKey(), player.displayName()),
                    Placeholder.unparsed(PlaceHolderKey.PLAYER_UUID.getKey(), player.getUniqueId().toString()),
                    Formatter.number(PlaceHolderKey.NUMBER.getKey(), headNumber)
                ));
                plugin.getProxyAdapter().sendMessage(Component.text()
                    .append(plugin.getMessageManager().getLang(TranslationKey.PLUGIN_PREFIX)).appendSpace()
                    .append(plugin.getMessageManager().getLang(
                        TranslationKey.POXY_MESSAGE_FOUND,
                        Placeholder.component(PlaceHolderKey.PLAYER_NAME.getKey(), player.displayName()),
                        Formatter.number(PlaceHolderKey.NUMBER.getKey(), headNumber)
                    )).build());

                for (final @NotNull Item item : event.getItems()) {
                    final @NotNull ItemStack itemStack = item.getItemStack();
                    if (itemStack.getType() == Registry.MATERIAL.get(ItemType.PLAYER_HEAD.key())) {
                        final @Nullable ResolvableProfile profile = itemStack.getData(DataComponentTypes.PROFILE);
                        plugin.getBoardManager().findHead(headNumber, profile);
                        break;
                    }
                }
            }
        }
    }

    protected void restoreLore(final @NonNull BlockDropItemEvent event, final @NonNull PersistentDataContainer blockPersistentDataContainer) throws JsonParseException, IllegalStateException {
        final @Nullable String jsonLoreStr = blockPersistentDataContainer.get(loreKey, PersistentDataType.STRING);
        final @Nullable List<@NotNull Component> lore;

        if (jsonLoreStr != null) {
            final @NotNull JsonArray jsonArray = JsonParser.parseString(jsonLoreStr).getAsJsonArray();
            lore = new ArrayList<>();

            for (final @NotNull JsonElement jsonElement : jsonArray) {
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
