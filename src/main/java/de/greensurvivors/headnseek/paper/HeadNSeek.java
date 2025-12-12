package de.greensurvivors.headnseek.paper;

import de.greensurvivors.headnseek.paper.config.PaperConfigManager;
import de.greensurvivors.headnseek.paper.language.MessageManager;
import de.greensurvivors.headnseek.paper.socialadapter.ASocialAdapter;
import de.greensurvivors.headnseek.paper.socialadapter.ASocialAdapterType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class HeadNSeek extends JavaPlugin {
    private final @NotNull PaperConfigManager configManager;
    private final @NotNull MessageManager messageManager;
    private final @NotNull HeadManager headManager;
    private @NotNull ASocialAdapter socialAdapter = ASocialAdapterType.DUMMY.createNew(this);

    public HeadNSeek() {
        this.configManager = new PaperConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.headManager = new HeadManager(this);
    }

    @Override
    public void onEnable () {
        reload();
        headManager.registerListeners();
    }

    public void reload() {
        configManager.reload();
        messageManager.reload(configManager.getLocale());
        socialAdapter = configManager.getSocialAdapterType().createNew(this);

        headManager.reload();
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
}
