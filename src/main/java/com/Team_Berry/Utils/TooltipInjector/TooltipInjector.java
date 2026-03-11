package com.Team_Berry.Utils.TooltipInjector;

import com.Team_Berry.Utils.Files.FileUtils;
import com.Team_Berry.Utils.UtilsPlugin;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class TooltipInjector {
    private static Map<String, String> languagesCache;
    private static void getLanguages() {
        try {
            I18nModule i18n = I18nModule.get();
            if (i18n == null) {
                UtilsPlugin.LOGGER.atWarning().log("I18nModule instance is null. Cannot inject tooltips.");
                return;
            }
            Field languagesField = I18nModule.class.getDeclaredField("languages");
            languagesField.setAccessible(true);
            Map<String, Map<String, String>> tmpLanguages = (Map)languagesField.get(i18n);
            if (tmpLanguages == null) {
                UtilsPlugin.LOGGER.atWarning().log("Languages map is null. Cannot inject tooltips.");
                return;
            }
            languagesCache = tmpLanguages.computeIfAbsent("en-US", (k) -> new ConcurrentHashMap());
        } catch (Exception e) {
            UtilsPlugin.LOGGER.atSevere().log("Failed to inject weapon tooltips: " + e.getMessage());
        }
    }

    public static void reloadLanguages() {
        getLanguages();
        if (languagesCache == null)
            return;
        try {
            parseLangFile(FileUtils.getBasePackRoot().resolve("Server").resolve("Languages").resolve("en-US"));
        } catch (IOException e) {
            UtilsPlugin.LOGGER.atSevere().log("Failed to load translation file: " + e.getMessage());
        }
    }

    public static void setItemTranslation(String i, StringFormatter d) { setItemTranslation(i, d.toString()); }
    public static void setItemTranslation(String itemKey, String description) {
        if (languagesCache == null)
            getLanguages();
        if (languagesCache == null)
            return;
        languagesCache.put(itemKey, description);
    }

    public static void addToItemTranslation(String i, StringFormatter d) { addToItemTranslation(i, d.toString()); }
    public static void addToItemTranslation(String itemKey, String description) {
        if (languagesCache == null)
            getLanguages();
        if (languagesCache == null)
            return;
        languagesCache.put(itemKey, getItemTranslation(itemKey) + description);
    }

    public static String getItemTranslation(String itemKey) {
        if (languagesCache == null)
            getLanguages();
        if (languagesCache == null)
            return null;
        return languagesCache.get(itemKey);
    }

    private static void parseLangFile(Path langDir) throws IOException {
        Files.list(langDir).filter(path -> path.toString().endsWith(".lang")).forEach((path -> {
            String fileName = path.getFileName().toString();
            String namespace = fileName.replace(".lang", "");

            try {
                BufferedReader reader = Files.newBufferedReader(path);

                String line;
                try {
                    while((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            int eqIndex = line.indexOf(61);
                            if (eqIndex > 0) {
                                String key = line.substring(0, eqIndex).trim();
                                String value = line.substring(eqIndex + 1).trim();
                                value = value.replace("\\n", "\n");
                                languagesCache.put(key, value);
                                if (!key.startsWith(namespace + ".")) {
                                    languagesCache.put(namespace + "." + key, value);
                                }
                            }
                        }
                    }
                } finally {
                    if (reader != null)
                        reader.close();
                }
            } catch (Exception e) {
                UtilsPlugin.LOGGER.atSevere().log("Error parsing lang file " + path + ": " + e.getMessage());
            }
        }));
    }

    public static void injectTooltips() {
        TooltipInjector.reloadLanguages();
        setItemTranslation("items.Weapon_Battleaxe_Custom.name", "Doomer");
        StringFormatter sf2 = new StringFormatter()
                .color(Color.CYAN).append("This weapon of mass destruction is said to have been given by the ")
                .color(Color.GRAY).setUnder().append("Doom Reaper").setUnder(false)
                .color(Color.CYAN).append(" to it's strongest warrior: ")
                .setBold()
                .color("#a83232").append("D")
                .color("#a86932").append("O")
                .color("#a4a832").append("O")
                .color("#46a832").append("M")
                .color("#32a887").append("S")
                .color("#3262a8").append("T")
                .color("#7332a8").append("A")
                .color("#a83289").append("C")
                .color("#a83232").append("K")
                .append("\n\n")
                .color(Color.MAGENTA).append("Somehow it fell into your hand...\n")
                .append("Will it be a blessing, or a curse ?")
                .endColor();
        setItemTranslation("items.Weapon_Battleaxe_Custom.description", sf2);
        //<color is="#00ffff">This <color is="#ff0000"><b>WEAPON</b><color is="#00ffff"> of mass destruction is said to have been given by the <color is="#808080"><u>Doom Reaper</u><color is="#00ffff"> to it's strongest warrior: <b><color is="#a83232">D<color is="#a86932">O<color is="#a4a832">O<color is="#46a832">M<color is="#32a887">S<color is="#3262a8">T<color is="#7332a8">A<color is="#a83289">C<color is="#a83232">K</b>
        //
        //<color is="#ff00ff">Somehow it fell into your hand...
        //Will it be a blessing, or a curse ?</color>
    }
}
