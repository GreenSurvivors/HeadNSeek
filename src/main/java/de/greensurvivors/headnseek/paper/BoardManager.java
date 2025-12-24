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
import org.bukkit.*;
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
import org.bukkit.util.BoundingBox;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
public class BoardManager implements Listener {
    protected static final @NotNull ComparableVersion MANAGER_DATA_VERSION = new ComparableVersion("1.0.0");
    protected final @NotNull HeadNSeek plugin;
    protected final @NotNull FileConfiguration configuration;
    protected final @NotNull Path boardsFilePath;
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
        configuration.set("dataVersion", MANAGER_DATA_VERSION.toString());
        boardsFilePath = plugin.getDataPath().resolve("headboards.yml");
    }

    public void reload() {
        headBoards.clear();

        if (Files.exists(boardsFilePath)) {
            try {
                configuration.load(Files.newBufferedReader(boardsFilePath));

                final ComparableVersion foundVersion = new ComparableVersion(configuration.getString("dataVersion", ""));
                if (foundVersion.compareTo(MANAGER_DATA_VERSION) > 0) {
                    plugin.getComponentLogger().error("Trying to load headboards.yml and encountered newer version. Expected: \"{}\", got: \"{}\"", MANAGER_DATA_VERSION, foundVersion);
                }

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

    public @Range(from = 0, to = Integer.MAX_VALUE) int removeAllBoardsNear(final @NotNull Location center, final double radius) {
        final int oldSize = headBoards.size();
        final @NotNull String worldName = center.getWorld().getName();

        final BlockPosition centerPosition = Position.block(center);
        headBoards.removeIf(headBoard -> headBoard.worldName.equals(worldName) &&
            doesCubeIntersectSphere(headBoard.boundingBox, centerPosition, radius));

        saveBoards();

        return oldSize - headBoards.size();
    }

    public @Range(from = 0, to = Integer.MAX_VALUE) int removeAllBoardsInWorld(final @NotNull String worldName) {
        final int oldSize = headBoards.size();
        headBoards.removeIf(headBoard -> headBoard.worldName.equals(worldName));
        saveBoards();

        return oldSize - headBoards.size();
    }

    public @Range(from = 0, to = Integer.MAX_VALUE) int removeAllBoards() {
        final int oldSize = headBoards.size();

        headBoards.clear();
        saveBoards();

        return oldSize;
    }

    @EventHandler(ignoreCancelled = true)
    protected void onHeadClick(final @NotNull PlayerInteractEvent event) {
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

                        // note: fetching the block location twice here is necessary since it is mutable, and we are changing it!
                        final @NotNull Location upperLeft = findCorner(facingDir, block.getLocation(), true);
                        final @NotNull Location lowerRight = findCorner(facingDir, block.getLocation(), false);
                        final @NotNull BoundingBox boundingBox = BoundingBox.of(upperLeft.getBlock(), lowerRight.getBlock());
                        final int length = calcLength(boundingBox);

                        final @NotNull HeadBoard newHeadBoard = new HeadBoard(worldName,
                            boundingBox,
                            wallSkull.getFacing(),
                            new Vector(facingDir.getZ(), 0.0D, -facingDir.getX()),
                            length);

                        boolean anyReplaced = false;
                        final @NotNull Int2ObjectMap<ResolvableProfile> oldHeads = new Int2ObjectOpenHashMap<>();
                        for (Iterator<HeadBoard> iterator = headBoards.iterator(); iterator.hasNext(); ) {
                            HeadBoard existingHeadBoard = iterator.next();
                            if (worldName.equals(existingHeadBoard.worldName) &&
                                newHeadBoard.boundingBox.overlaps(existingHeadBoard.boundingBox)) {
                                if (newHeadBoard.facing == existingHeadBoard.facing) {
                                    iterator.remove();
                                    anyReplaced = true;

                                    if (updateOld) {
                                        int num = 1;
                                        for (int y = (int) existingHeadBoard.boundingBox.getMaxY(); y > existingHeadBoard.boundingBox.getMinY(); y--) {
                                            for (int x = (int) existingHeadBoard.boundingBox.getMaxX(); x > existingHeadBoard.boundingBox.getMinX(); x--) {
                                                for (int z = (int) existingHeadBoard.boundingBox.getMaxZ(); z > existingHeadBoard.boundingBox.getMinZ(); z--) {

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
                        highlightBoard(newHeadBoard);

                        saveBoards();

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

    private static int calcLength(final @NonNull BoundingBox boundingBox) {
        return boundingBox.getWidthX() > 1 ? (int) boundingBox.getWidthX() : (int) boundingBox.getWidthZ();
    }

    protected void highlightBoard(final @NotNull HeadBoard headBoard) { // todo just outline?
        final @Nullable World world = plugin.getServer().getWorld(headBoard.worldName);

        if (world != null) {
            for (int y = (int) headBoard.boundingBox.getMaxY(); y > headBoard.boundingBox.getMinY(); y--) {
                for (int x = (int) headBoard.boundingBox.getMaxX(); x > headBoard.boundingBox.getMinX(); x--) {
                    for (int z = (int) headBoard.boundingBox.getMaxZ(); z > headBoard.boundingBox.getMinZ(); z--) {
                        world.spawnParticle(Particle.HAPPY_VILLAGER, x - 0.5, y - 0.5, z - 0.5, 2);
                    }
                }
            }
        }
    }

    protected void saveBoards() {
        try (final @NotNull Writer writer = Files.newBufferedWriter(boardsFilePath, StandardCharsets.UTF_8)) {
            configuration.set("boards", headBoards.toArray());

            writer.write(configuration.saveToString());
        } catch (final @NotNull IOException e) {
            plugin.getComponentLogger().error("Could not safe head file to disk. Data loss is imminent!", e);
        }
    }

    protected void setHeadOnBoard(final @NonNull HeadBoard headBoard,
                                  @Range(from = 1, to = Integer.MAX_VALUE) int headNum,
                                  final @Nullable ResolvableProfile profile) {
        final int boardSpace = (int) (headBoard.length * headBoard.boundingBox.getHeight());
        if (boardSpace < headNum) {
            plugin.getComponentLogger().info("Ignored placement of head number " + headNum + " at board at " + headBoard.worldName + " [" + headBoard.boundingBox.getMax() + "], because it was not big enough (" + boardSpace + ").");
            return;
        }

        final @Nullable World world = plugin.getServer().getWorld(headBoard.worldName);

        if (world != null) {
            // the first head is assigned to number 1, but needs to get shifted to block 0
            headNum = Math.max(0, headNum - 1);
            final int mod = headNum % headBoard.length;

            // -1 because our bounding box is the whole block the head is in big.
            // therefore we can't treat them like it would be Block cordinates, but shrink the box by one.
            final int x = (int) headBoard.boundingBox.getMaxX() - 1 + mod * headBoard.rightDir.getBlockX();
            final int y = (int) headBoard.boundingBox.getMaxY() - 1 - headNum / headBoard.length;
            final int z = (int) headBoard.boundingBox.getMaxZ() - 1 + mod * headBoard.rightDir.getBlockZ();

            final @NotNull WallSkull data = BlockType.PLAYER_WALL_HEAD.createBlockData();
            data.setFacing(headBoard.facing);

            world.setBlockData(x, y, z, data);
            final Skull head = (Skull) world.getBlockAt(x, y, z).getState(false);
            head.setProfile(profile);
            head.update(true, false); // even though we are not working with a snapshot here, the state somehow needs to get updated.
        } else {
            plugin.getComponentLogger().warn("Could not find World named {}", headBoard.worldName);
        }
    }

    protected static @NotNull Location findCorner(final @NotNull Vector facingDir, final @NotNull Location location, final boolean upperLeft) {
        final int sign = upperLeft ? 1 : -1;
        final @NotNull Vector direction = new Vector(-sign * facingDir.getZ(), 0.0D, sign * facingDir.getX());
        final Material PlayerWallHeadType = Registry.MATERIAL.get(BlockType.PLAYER_WALL_HEAD.getKey());

        // we iterate until the block is no longer a player wall head. No need to do anything in loop body!
        // left / right - depending on sign
        //noinspection StatementWithEmptyBody
        while (location.add(direction).getBlock().getType() == PlayerWallHeadType);
        location.subtract(direction);

        // up / down - depending on sign
        //noinspection StatementWithEmptyBody
        while (location.add(0D, sign, 0D).getBlock().getType() == PlayerWallHeadType);
        location.subtract(0D, sign, 0D);

        return location;
    }

    // stolen from https://stackoverflow.com/questions/4578967/cube-sphere-intersection-test
    protected static boolean doesCubeIntersectSphere(final @NotNull BoundingBox boundingBox, final @NotNull BlockPosition centerSphere, final double radius) {
        double dist_squared = radius * radius;
        if (centerSphere.blockX() < boundingBox.getMinX()) {
            dist_squared -= Utils.square(centerSphere.blockX() - (int) boundingBox.getMinX());
        } else if (centerSphere.blockX() > boundingBox.getMaxX()) {
            dist_squared -= Utils.square(centerSphere.blockX() - (int) boundingBox.getMaxX());
        }
        if (centerSphere.blockY() < boundingBox.getMinY()) {
            dist_squared -= Utils.square(centerSphere.blockY() - (int) boundingBox.getMinY());
        } else if (centerSphere.y() > boundingBox.getMaxY()) {
            dist_squared -= Utils.square(centerSphere.blockY() - (int) boundingBox.getMaxY());
        }
        if (centerSphere.blockZ() < boundingBox.getMinZ()) {
            dist_squared -= Utils.square(centerSphere.blockZ() - (int) boundingBox.getMinZ());
        } else if (centerSphere.blockZ() > boundingBox.getMaxZ()) {
            dist_squared -= Utils.square(centerSphere.blockZ() - (int) boundingBox.getMaxZ());
        }
        return dist_squared > 0;
    }

    protected record HeadBoard(@NotNull String worldName,
                               @NotNull BoundingBox boundingBox,
                               @NotNull BlockFace facing,
                               // handy little caches
                               @NotNull Vector rightDir, int length) implements ConfigurationSerializable {
        protected final static @NotNull ComparableVersion BOARD_DATA_VERSION = new ComparableVersion("1.0.0");

        @SuppressWarnings("unused")
        public static @NotNull HeadBoard deserialize(final @NotNull Map<@NotNull String, Object> serialized) throws IllegalArgumentException {
            if (serialized.get("dataVersion") instanceof String dataVersionStr) {
                final ComparableVersion dataVersion = new ComparableVersion(dataVersionStr);
                if (dataVersion.compareTo(BOARD_DATA_VERSION) > 0) {
                    throw new IllegalArgumentException("Encountered newer DataVersion. Expected: \"" + BOARD_DATA_VERSION + "\"; got " + dataVersion);
                }
            }

            if (serialized.get("worldName") instanceof String worldName &&
                serialized.get("boundingBox") instanceof BoundingBox boundingBox &&
                serialized.get("facing") instanceof String facingName) {

                final @NotNull BlockFace facing = BlockFace.valueOf(facingName.toUpperCase(Locale.ENGLISH));
                final @NotNull Vector rightDir = new Vector(facing.getModZ(), 0, -facing.getModX());

                return new HeadBoard(worldName,
                    boundingBox,
                    facing,
                    rightDir,
                    calcLength(boundingBox)
                );
            } else {
                throw new IllegalArgumentException(serialized + " is not a valid HeadBoard!");
            }
        }

        @Override
        public @NotNull Map<@NotNull String, Object> serialize() {
            return Map.of(
                "dataVersion", BOARD_DATA_VERSION.toString(),
                "worldName", worldName,
                "boundingBox", boundingBox,
                "facing", facing.name()
            );
        }
    }
}
