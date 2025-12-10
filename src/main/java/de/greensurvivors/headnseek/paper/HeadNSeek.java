package de.greensurvivors.headnseek.paper;

import de.greensurvivors.headnseek.paper.config.PaperConfigManager;
import de.greensurvivors.headnseek.paper.language.MessageManager;
import de.greensurvivors.headnseek.paper.socialadapter.ASocialAdapter;
import de.greensurvivors.headnseek.paper.socialadapter.SocialAdapterType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class HeadNSeek extends JavaPlugin {
    private final @NotNull PaperConfigManager configManager;
    private final @NotNull MessageManager messageManager;
    private @NotNull ASocialAdapter socialAdapter = SocialAdapterType.SLACK.createNew(this);

    public HeadNSeek() {
        this.configManager = new PaperConfigManager(this);
        this.messageManager = new MessageManager(this);
    }

    @Override
    public void onEnable () {
        new HeadManager(this);
        reload();
    }

    public void reload() {
        configManager.reload();
        messageManager.reload(configManager.getLocale());
        socialAdapter = configManager.getSocialAdapterType().createNew(this);
        socialAdapter.setUri(configManager.getSocialAdapterUri());
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
}
