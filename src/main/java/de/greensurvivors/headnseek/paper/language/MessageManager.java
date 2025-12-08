package de.greensurvivors.headnseek.paper.language;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.apache.commons.io.FileUtils;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * manages all translatable and placeholders used by this plugin.
 */
public class MessageManager {
    private final String BUNDLE_NAME = "lang";
    final @NotNull Pattern BUNDLE_FILE_NAME_PATTERN = Pattern.compile(BUNDLE_NAME + "(?:_.*)?.properties");
    private final Plugin plugin;
    private ResourceBundle lang;
    /**
     * caches every component without placeholder for faster access in future and loads missing values automatically
     */
    private final @NotNull LoadingCache<@NotNull TranslationKey, @NotNull Component> langCache = Caffeine.newBuilder().build(
        path -> MiniMessage.miniMessage().deserialize(getStringFromLang(path)));

    public MessageManager(final @NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    private @NotNull String getStringFromLang(final @NotNull TranslationKey path) {
        try {
            return lang.getString(path.getPath());
        } catch (MissingResourceException | ClassCastException e) {
            plugin.getLogger().log(Level.WARNING, "couldn't find path: \"" + path.getPath() + "\" in lang files using fallback.", e);
            return path.getDefaultValue();
        }
    }

    /**
     * reload language file.
     */
    public void reload(final @NotNull Locale locale) {
        lang = null; // reset last bundle

        // save all missing keys
        initLangFiles();

        plugin.getLogger().info("Locale set to language: " + locale.toLanguageTag());
        File langDictionary = new File(plugin.getDataFolder(), BUNDLE_NAME);

        URL[] urls;
        try {
            urls = new URL[]{langDictionary.toURI().toURL()};
            lang = ResourceBundle.getBundle(BUNDLE_NAME, locale, new URLClassLoader(urls), UTF8ResourceBundleControl.get());

        } catch (SecurityException | MalformedURLException e) {
            plugin.getLogger().log(Level.WARNING, "Exception while reading lang bundle. Using internal", e);
        } catch (MissingResourceException ignored) { // how? missing write access?
            plugin.getLogger().log(Level.WARNING, "No translation file for " + UTF8ResourceBundleControl.get().toBundleName(BUNDLE_NAME, locale) + " found on disc. Using internal");
        }

        if (lang == null) { // fallback, since we are always trying to save defaults this never should happen
            try {
                lang = PropertyResourceBundle.getBundle(BUNDLE_NAME, locale, plugin.getClass().getClassLoader(), new UTF8ResourceBundleControl());
            } catch (MissingResourceException e) {
                plugin.getLogger().log(Level.SEVERE, "Couldn't get Ressource bundle \"lang\" for locale \"" + locale.toLanguageTag() + "\". Messages WILL be broken!", e);
            }
        }

        // clear component cache
        langCache.invalidateAll();
        langCache.cleanUp();
        langCache.asMap().clear();
    }

    private @NotNull String saveConvert(final @NotNull String theString, final boolean escapeSpace) {
        int len = theString.length();
        int bufLen = len * 2;
        if (bufLen < 0) {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuilder convertedStrBuilder = new StringBuilder(bufLen);

        for (int i = 0; i < theString.length(); i++) {
            char aChar = theString.charAt(i);
            // Handle common case first
            if ((aChar > 61) && (aChar < 127)) {
                if (aChar == '\\') {
                    if (i + 1 < theString.length()) {
                        final char bChar = theString.charAt(i + 1);
                        if (bChar == ' ' || bChar == 't' || bChar == 'n' || bChar == 'r' ||
                            bChar == 'f' || bChar == '\\' || bChar == 'u' || bChar == '=' ||
                            bChar == ':' || bChar == '#' || bChar == '!') {
                            // don't double escape already escaped chars
                            convertedStrBuilder.append(aChar);
                            convertedStrBuilder.append(bChar);
                            i++;
                            continue;
                        } else {
                            // any other char following
                            convertedStrBuilder.append('\\');
                        }
                    } else {
                        // last char was a backslash. escape!
                        convertedStrBuilder.append('\\');
                    }
                }
                convertedStrBuilder.append(aChar);
                continue;
            }

            // escape non escaped chars that have to get escaped
            switch (aChar) {
                case ' ' -> {
                    if (escapeSpace) {
                        convertedStrBuilder.append('\\');
                    }
                    convertedStrBuilder.append(' ');
                }
                case '\t' -> convertedStrBuilder.append("\\t");
                case '\n' -> convertedStrBuilder.append("\\n");
                case '\r' -> convertedStrBuilder.append("\\r");
                case '\f' -> convertedStrBuilder.append("\\f");
                case '=', ':', '#', '!' -> {
                    convertedStrBuilder.append('\\');
                    convertedStrBuilder.append(aChar);
                }
                default -> convertedStrBuilder.append(aChar);
            }
        }

        return convertedStrBuilder.toString();
    }

    /**
     * saves all missing lang files from resources to the plugins datafolder
     */
    private void initLangFiles() {
        CodeSource src = this.getClass().getProtectionDomain().getCodeSource();
        if (src != null) {
            URL jarUrl = src.getLocation();
            try (ZipInputStream zipStream = new ZipInputStream(jarUrl.openStream())) {
                ZipEntry zipEntry;
                while ((zipEntry = zipStream.getNextEntry()) != null) {
                    if (zipEntry.isDirectory()) {
                        continue;
                    }

                    String entryName = zipEntry.getName();

                    if (BUNDLE_FILE_NAME_PATTERN.matcher(entryName).matches()) {
                        File langFile = new File(new File(plugin.getDataFolder(), BUNDLE_NAME), entryName);
                        if (!langFile.exists()) { // don't overwrite existing files
                            FileUtils.copyToFile(zipStream, langFile);
                        } else { // add defaults to file to expand in case there are key-value pairs missing
                            Properties defaults = new Properties();
                            // don't close reader, since we need the stream to be still open for the next entry!
                            defaults.load(new InputStreamReader(zipStream, StandardCharsets.UTF_8));

                            Properties current = new Properties();
                            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(langFile), StandardCharsets.UTF_8)) {
                                current.load(reader);
                            } catch (Exception e) {
                                plugin.getLogger().log(Level.WARNING, "couldn't get current properties file for " + entryName + "!", e);
                                continue;
                            }

                            try (FileWriter fw = new FileWriter(langFile, StandardCharsets.UTF_8, true);
                                 // we are NOT using Properties#store since it gets rid of comments and doesn't guarantee ordering
                                 BufferedWriter bw = new BufferedWriter(fw)) {
                                boolean updated = false; // only write comment once
                                for (Map.Entry<Object, Object> translationPair : defaults.entrySet()) {
                                    if (current.get(translationPair.getKey()) == null) {
                                        if (!updated) {
                                            bw.write("# New Values where added. Is everything else up to date? Time of update: " + new Date());
                                            bw.newLine();

                                            plugin.getLogger().fine("Updated langfile \"" + entryName + "\". Might want to check the new translation strings out!");

                                            updated = true;
                                        }

                                        String key = saveConvert((String) translationPair.getKey(), true);
                                        /* No need to escape embedded and trailing spaces for value, hence
                                         * pass false to flag.
                                         */
                                        String val = saveConvert((String) translationPair.getValue(), false);
                                        bw.write((key + "=" + val));
                                        bw.newLine();
                                    } // current already knows the key
                                } // end of for
                            } // end of try
                        } // end of else (file exists)
                    } // doesn't match
                } // end of elements
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Couldn't save lang files", e);
            }
        } else {
            plugin.getLogger().warning("Couldn't save lang files: no CodeSource!");
        }
    }

    /**
     * prepend the message with the plugins prefix before sending it to the audience.
     */
    public void sendMessage(final @NotNull Audience audience, final @NotNull Component messages) {
        audience.sendMessage(Component.text().append(langCache.get(TranslationKey.PLUGIN_PREFIX)).appendSpace().append(messages));
    }

    /**
     * get a component from lang file and apply the given tag resolver.
     * Note: might be slightly slower than {@link #getLang(TranslationKey)} since this can not use cache.
     */
    public @NotNull Component getLang(@NotNull TranslationKey path, @NotNull TagResolver... resolver) {
        return MiniMessage.miniMessage().deserialize(getStringFromLang(path), resolver);
    }

    /**
     * get a component from lang file
     */
    public @NotNull Component getLang(@NotNull TranslationKey path) {
        return langCache.get(path);
    }

    /**
     * send a component from the lang file to the audience, prefixed with this plugins prefix.
     */
    public void sendLang(@NotNull Audience audience, @NotNull TranslationKey path) {
        sendMessage(audience, getLang(path));
    }

    /**
     * send a component from the lang file to the audience, prefixed with this plugins prefix and applying the given tag resolver.
     * Note: might be slightly slower than {@link #sendLang(Audience, TranslationKey)} since this can not use cache.
     */
    public void sendLang(@NotNull Audience audience, @NotNull TranslationKey path, @NotNull TagResolver... resolver) {
        sendMessage(audience, getLang(path, resolver));
    }
}