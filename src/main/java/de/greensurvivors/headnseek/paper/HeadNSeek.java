package de.greensurvivors.headnseek.paper;

import de.greensurvivors.headnseek.paper.config.PaperConfigManager;
import de.greensurvivors.headnseek.paper.language.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class HeadNSeek extends JavaPlugin {
    private final @NotNull PaperConfigManager configManager;
    private final @NotNull MessageManager messageManager;

    public HeadNSeek() {
        this.configManager = new PaperConfigManager(this);
        this.messageManager = new MessageManager(this);
    }

    @Override
    public void onEnable () {
        configManager.reload();
        messageManager.reload(configManager.getLocale());
    }

    public @NotNull PaperConfigManager getConfigManager() {
        return configManager;
    }

    public @NotNull MessageManager getMessageManager() {
        return messageManager;
    }
}
