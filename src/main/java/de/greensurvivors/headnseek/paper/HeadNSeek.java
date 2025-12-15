package de.greensurvivors.headnseek.paper;

import de.greensurvivors.headnseek.paper.command.CmdBase;
import de.greensurvivors.headnseek.paper.config.PaperConfigManager;
import de.greensurvivors.headnseek.paper.language.MessageManager;
import de.greensurvivors.headnseek.paper.network.AProxyAdapter;
import de.greensurvivors.headnseek.paper.network.DummyAdapter;
import de.greensurvivors.headnseek.paper.network.GreenSocketAdapter;
import de.greensurvivors.headnseek.paper.socialadapter.ASocialAdapter;
import de.greensurvivors.headnseek.paper.socialadapter.ASocialAdapterType;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class HeadNSeek extends JavaPlugin {
    private final @NotNull PaperConfigManager configManager;
    private final @NotNull MessageManager messageManager;
    private final @NotNull HeadManager headManager;
    private final @NotNull CmdBase cmdBase;
    private final @NotNull BoardManager boardManager;
    private @NotNull ASocialAdapter socialAdapter = ASocialAdapterType.DUMMY.createNew(this);
    private @NotNull AProxyAdapter proxyAdapter;

    public HeadNSeek() {
        configManager = new PaperConfigManager(this);
        messageManager = new MessageManager(this);
        headManager = new HeadManager(this);
        proxyAdapter = new DummyAdapter(this);
        cmdBase = new CmdBase(this);
        boardManager = new BoardManager(this);
    }

    @Override
    public void onEnable () {
        reload();
        headManager.registerListeners();

        if (getServer().getPluginManager().isPluginEnabled("GreenSocket")) {
            proxyAdapter = new GreenSocketAdapter(this);
        }

        final LifecycleEventManager<Plugin> lifecycleManager = this.getLifecycleManager();
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, cmdBase::onCmdServerRegistration);
    }

    public void reload() {
        configManager.reload();
        messageManager.reload(configManager.getLocale());
        socialAdapter = configManager.getSocialAdapterType().createNew(this);

        headManager.reload();
        boardManager.reload();
    }

    public @NotNull PaperConfigManager getConfigManager() {
        return configManager;
    }

    public @NotNull MessageManager getMessageManager() {
        return messageManager;
    }

    public @NotNull ASocialAdapter getSocialAdapter() {
        return socialAdapter;
    }

    public @NotNull HeadManager getHeadManager() {
        return headManager;
    }

    public @NotNull AProxyAdapter getProxyAdapter() {
        return proxyAdapter;
    }

    public @NotNull BoardManager getBoardManager() {
        return boardManager;
    }
}
