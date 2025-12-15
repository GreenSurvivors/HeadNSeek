package de.greensurvivors.headnseek.paper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.greensurvivors.headnseek.common.Utils;
import de.greensurvivors.headnseek.paper.language.TranslationKey;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockType;
import org.bukkit.block.Skull;
import org.bukkit.block.data.type.WallSkull;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
public class BoardManager implements Listener { // todo clear all / near / in world boards cmd
    protected final @NotNull HeadNSeek plugin;
    protected final @NotNull FileConfiguration configuration;
    protected final @NotNull Path boardsFilePath;
    protected final @NotNull ComparableVersion dataVersion = new ComparableVersion("1.0.0");
    protected final @NotNull Set<@NotNull HeadBoard> headBoards = new HashSet<>();
    protected final @NotNull Cache<@NotNull UUID, Boolean> definingPlayers = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    static {
        ConfigurationSerialization.registerClass(HeadBoard.class);
    }

    public BoardManager(final @NotNull HeadNSeek plugin) {
        this.plugin = plugin;
        configuration = new YamlConfiguration();
        configuration.set("dataVersion", dataVersion.toString());
        configuration.set("boards", headBoards); // because this is refference based, there should be no point to ever change this
        boardsFilePath = plugin.getDataPath().resolve("headboards.yml");

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void reload() {
        headBoards.clear();

        if (Files.exists(boardsFilePath)) {
            try {
                configuration.load(Files.newBufferedReader(boardsFilePath));

                final List<?> uncheckedBoards = configuration.getList("boards");
                if (uncheckedBoards != null) {
                    headBoards.addAll(Utils.checkCollection(uncheckedBoards, HeadBoard.class));
                }
            } catch (IOException | InvalidConfigurationException e) {
                plugin.getComponentLogger().error("Couldn't load head storage file!", e);
            }
        }
    }

    public void findHead(final @NotNull @Range(from = 1, to = Integer.MAX_VALUE) Integer headNumber, final @Nullable ResolvableProfile resolvableProfile) {
        if (headBoards.isEmpty()) {
            return;
        }

        for (final @NotNull HeadBoard headBoard : headBoards) {
            setHeadOnBoard(headBoard, headNumber, resolvableProfile);
        }
    }

    public void registerForDefining(final @NotNull UUID uuid, final @NotNull Boolean updateOld) {
        this.definingPlayers.put(uuid, updateOld);
    }

    @EventHandler(ignoreCancelled = true)
    protected void onHeadClick (final @NotNull PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
            final @Nullable Block block = event.getClickedBlock();

            if (block != null) {
                final @NotNull Player player = event.getPlayer();
                final @NotNull UUID uniqueId = player.getUniqueId();
                final @Nullable Boolean updateOld = definingPlayers.getIfPresent(uniqueId);
                if (updateOld != null) {
                    if (block.getBlockData() instanceof WallSkull wallSkull) {
                        event.setCancelled(true);
                        final @NotNull World world = block.getWorld();
                        final @NotNull String worldName = world.getName();
                        final @NotNull Vector facingDir = wallSkull.getFacing().getDirection();

                        // note: fetching the location twice here is necessary since it is mutable, and we are changing it!
                        final @NotNull BlockPosition upperLeft = findCorner(facingDir, block.getLocation(), true);
                        final @NotNull BlockPosition lowerRight = findCorner(facingDir, block.getLocation(), false);

                        final @NotNull HeadBoard newHeadBoard = new HeadBoard(worldName, upperLeft, lowerRight,
                            wallSkull.getFacing(),
                            new Vector(facingDir.getZ(), 0.0D, -facingDir.getX()),
                            Utils.getHorizontalLength(upperLeft, lowerRight));


                        final double minX = Math.min(upperLeft.x(), lowerRight.x());
                        final double maxX = Math.max(upperLeft.x(), lowerRight.x());
                        final double minY = lowerRight.y();
                        final double maxY = upperLeft.y();
                        final double minZ = Math.min(upperLeft.z(), lowerRight.z());
                        final double maxZ = Math.max(upperLeft.z(), lowerRight.z());

                        boolean anyReplaced = false;
                        final @NotNull Int2ObjectMap<ResolvableProfile> oldHeads = new Int2ObjectOpenHashMap<>();
                        for (Iterator<HeadBoard> iterator = headBoards.iterator(); iterator.hasNext(); ) {
                            HeadBoard existingHeadBoard = iterator.next();
                            if (worldName.equals(existingHeadBoard.worldName) &&
                                overlaps(minX, maxX, minY, maxY, minZ, maxZ, existingHeadBoard)) {
                                if (newHeadBoard.facing == existingHeadBoard.facing) {
                                    iterator.remove();
                                    anyReplaced = true;

                                    if (updateOld) {
                                        int num = 1;
                                        for (int y = existingHeadBoard.upperLeft.blockY(); y >= existingHeadBoard.lowerRight.blockY(); y--) {
                                            for (int x = existingHeadBoard.upperLeft.blockX();
                                                 Math.abs(existingHeadBoard.lowerRight.blockX() - x) >= 0;
                                                 x += existingHeadBoard.rightDir.getBlockX()) {
                                                for (int z = existingHeadBoard.upperLeft.blockZ();
                                                     Math.abs(existingHeadBoard.lowerRight.blockZ() - z) >= 0;
                                                     z += existingHeadBoard.rightDir.getBlockZ()) {

                                                    if (world.getBlockAt(x, y, z).getState(false) instanceof Skull skull) {
                                                        oldHeads.put(num, skull.getProfile());
                                                    }
                                                    num++;
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    plugin.getMessageManager().sendLang(player, TranslationKey.ACTION_DEFINE_BOARD_ERROR_OVERLAP_CANT_MERGE); // todo add delete cmd reference here
                                    return;
                                }
                            }
                        }

                        if (!oldHeads.isEmpty()) {
                            for (final @NotNull Int2ObjectMap.Entry<@NotNull ResolvableProfile> entry : oldHeads.int2ObjectEntrySet()) {
                                setHeadOnBoard(newHeadBoard, entry.getIntKey(), entry.getValue());
                            }
                        }

                        headBoards.add(newHeadBoard);
                        definingPlayers.invalidate(uniqueId);

                        try (final @NotNull Writer writer = Files.newBufferedWriter(boardsFilePath,
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

                            writer.write(configuration.saveToString());
                        } catch (final @NotNull IOException e) {
                            plugin.getComponentLogger().error("Could not safe head file to disk. Data loss is imminent!", e);
                        }

                        if (anyReplaced) {
                            plugin.getMessageManager().sendLang(player, TranslationKey.ACTION_DEFINE_BOARD_REPLACED);
                        } else {
                            plugin.getMessageManager().sendLang(player, TranslationKey.ACTION_DEFINE_BOARD_SUCCESS);
                        }
                    } else {
                        // todo do we want to message the player here or would that be annoying?
                    }
                }
            }
        }
    }

    protected void setHeadOnBoard(final @NonNull HeadBoard headBoard, final int headNum, final ResolvableProfile profile) {
        final @Nullable World world = plugin.getServer().getWorld(headBoard.worldName);

        if (world != null) {
            final int mod = headNum % headBoard.length;

            final int x = headBoard.upperLeft.blockX() + mod * headBoard.rightDir.getBlockX();
            final int z = headBoard.upperLeft.blockZ() + mod * headBoard.rightDir.getBlockZ();
            final int y = headBoard.lowerRight.blockY() + headNum / headBoard.length;

            WallSkull data = BlockType.PLAYER_WALL_HEAD.createBlockData();
            data.setFacing(headBoard.facing);

            world.setBlockData(x, y, z, data);
            ((Skull) world.getBlockAt(x, y, z).getState(false)).setProfile(profile);
        } else {
            plugin.getComponentLogger().warn("Could not find World named {}", headBoard.worldName);
        }
    }

    protected static @NotNull BlockPosition findCorner(final @NotNull Vector facingDir, final @NotNull Location location, final boolean upperLeft) {
        final int sign = upperLeft ? 1 : -1;
        final @NotNull Vector direction = new Vector(-sign * facingDir.getZ(), 0.0D, sign * facingDir.getX());
        final Material PlayerWallHeadType = Registry.MATERIAL.get(BlockType.PLAYER_WALL_HEAD.getKey());

        // we iterate until the block is no longer a player wall head. No need to do anything in loop body!
        // left / right
        //noinspection StatementWithEmptyBody
        while (location.add(direction).getBlock().getType() == PlayerWallHeadType);
        location.subtract(direction);

        // up / down
        //noinspection StatementWithEmptyBody
        while (location.add(0D, sign, 0D).getBlock().getType() == PlayerWallHeadType);
        location.subtract(0D, sign, 0D);

        return Position.block(location);
    }

    protected static boolean overlaps(final double firstMinX, final double firstMaxX,
                                      final double firstMinY, final double firstMaxY,
                                      final double firstMinZ, final double firstMaxZ,
                                      final @NotNull HeadBoard headBoard) {
        final double secondMinX = Math.min(headBoard.upperLeft.x(), headBoard.lowerRight.x());
        final double secondMaxX = Math.max(headBoard.upperLeft.x(), headBoard.lowerRight.x());
        final double secondMinZ = Math.min(headBoard.upperLeft.z(), headBoard.lowerRight.z());
        final double secondMaxZ = Math.max(headBoard.upperLeft.z(), headBoard.lowerRight.z());

        return firstMinX < secondMaxX && firstMaxX > secondMinX
            && firstMinY < headBoard.upperLeft.y() && firstMaxY > headBoard.lowerRight.y()
            && firstMinZ < secondMaxZ && firstMaxZ > secondMinZ;
    }

    protected record HeadBoard(@NotNull String worldName, @NotNull BlockPosition upperLeft, @NotNull BlockPosition lowerRight,
                               @NotNull BlockFace facing,
                               // handy little caches
                               @NotNull Vector rightDir,  int length) implements ConfigurationSerializable {
        protected final static ComparableVersion DATA_VERSION = new ComparableVersion("1.0.0");

        @SuppressWarnings("unused")
        public static @NotNull HeadBoard deserialize(final @NotNull Map<@NotNull String, Object> serialized) throws IllegalArgumentException {
            if (serialized.get("dataVersion") instanceof String dataVersionStr) {
                final ComparableVersion dataVersion = new ComparableVersion(dataVersionStr);
                if (dataVersion.compareTo(DATA_VERSION) > 0) {
                    throw new IllegalArgumentException("Encountered newer DataVersion. Expected: \"" + DATA_VERSION + "\"; got " + dataVersion);
                }
            }

            if (serialized.get("worldName") instanceof String worldName &&
                serialized.get("upperLeft") instanceof Map<?,?> uncheckedUpperLeft &&
                serialized.get("lowerRight") instanceof Map<?,?> uncheckedLowerRight &&
                serialized.get("facing") instanceof String facingName) {

                final @NotNull BlockPosition upperLeft = Utils.deserializePosition(Utils.checkMap(uncheckedLowerRight, Object.class));
                final @NotNull BlockPosition lowerRight = Utils.deserializePosition(Utils.checkMap(uncheckedLowerRight, Object.class));
                final @NotNull BlockFace facing = BlockFace.valueOf(facingName.toLowerCase(Locale.ENGLISH));
                final @NotNull Vector rightDir = new Vector(facing.getModZ(), 0, -facing.getModX());

                return new HeadBoard(worldName,
                    upperLeft, lowerRight,
                    facing,
                    rightDir,
                    Utils.getHorizontalLength(upperLeft, lowerRight)
                );
            } else {
                throw new IllegalArgumentException(serialized + " is not a valid HeadBoard!");
            }
        }

        @Override
        public @NotNull Map<@NotNull String, Object> serialize() {
            return Map.of(
                "dataVersion", DATA_VERSION.toString(),
                "worldName", worldName,
                "upperLeft", Utils.serializePosition(upperLeft),
                "lowerRight", Utils.serializePosition(lowerRight),
                "facing", facing.name()
            );
        }
    }
}
