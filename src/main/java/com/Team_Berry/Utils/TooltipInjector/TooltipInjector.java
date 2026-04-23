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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
            Map<String, Map<String, String>> tmpLanguages = (Map) languagesField.get(i18n);
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

    public static void setItemTranslation(String i, StringFormatter d) {
        setItemTranslation(i, d.toString());
    }

    public static void setItemTranslation(String itemKey, String description) {
        if (languagesCache == null)
            getLanguages();
        if (languagesCache == null)
            return;
        languagesCache.put(itemKey, description);
    }

    public static void addToItemTranslation(String i, StringFormatter d) {
        addToItemTranslation(i, d.toString());
    }

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
                    while ((line = reader.readLine()) != null) {
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
        StringFormatter sf = new StringFormatter();
        setItemTranslation("items.Weapon_Battleaxe_Custom.name", "Doomer");
        sf.clear().color(Color.CYAN).append("This weapon of mass destruction is said to have been given by the ")
                .color(Color.GRAY).setUnder().append("Doom Reaper").setUnder(false)
                .color(Color.CYAN).append(" to it's strongest warrior: ")
                .color(Color.RED).setBold().append("DOOMSTACK").setBold(false).append("\n\n")
                .color(Color.MAGENTA).append("Somehow it fell into your hand...\n")
                .append("Will it be a blessing, or a curse ?")
                .endColor();
        setItemTranslation("items.Weapon_Battleaxe_Custom.description", sf);
        setItemTranslation("client.itemTooltip.damageCauseResistance.environmental", "Environmental Resistance");
        injectDaggerTooltips();
    }

    public static void injectDaggerTooltips() {
        TooltipInjector.reloadLanguages();
        Color tierLow = Color.GREEN;
        Color tierMed = Color.ORANGE;
        Color tierHigh = Color.RED;

        registerDaggerSkill("DaggerThrow", false, tierLow, "Low", null, null, null, "1.2");

        registerDaggerSkill("Disengage", true, tierLow, "Low", null, null, null, "6");

        registerDaggerSkill("SecondWind", true, null, null, null, null, "Invincibility", "30");

        registerDaggerSkill("ShadowDash", true, tierMed, "Medium", null, null, "Stun", "8");

        registerDaggerSkill("PocketBomb", false, tierHigh, "High", tierMed, "Medium", null, "8");

        registerDaggerSkill("TwinStab", false, tierHigh, "High", null, null, null, "1.9");

        registerDaggerSkill("Whirl", true, tierHigh, "High", null, null, null, "7");

        registerDaggerSkill("WideSlash", false, tierMed, "Medium", tierMed, "Medium", null, "4");
    }

    private static void registerDaggerSkill(String id, boolean hasMobility,
                                            Color damageTierColor, String damageTier,
                                            Color kbTierColor, String kbTier,
                                            String effect,
                                            String cooldown) {
        StringFormatter sf = new StringFormatter();

        Color colorMobility = new Color(255, 215, 0);
        Color colorDamage = new Color(200, 42, 42);
        Color colorKnockback = new Color(149, 157, 255);
        Color colorEffect = Color.GREEN;
        Color textWhite = Color.WHITE;


        sf.color(Color.white).append(getItemTranslation(id + ".description")).append("\n");

        if (hasMobility) {
            sf.color(colorMobility).append("Mobility").endColor().append("\n");
        }
        if (damageTier != null) {
            sf.color(colorDamage).append("Damage: ").color(damageTierColor).append(damageTier).endColor().append("\n");
        }
        if (kbTier != null) {
            sf.color(colorKnockback).append("Knockback: ").color(kbTierColor).append(kbTier).endColor().append("\n");
        }
        if (effect != null) {
            sf.color(colorEffect).append("Effect: ").color(textWhite).append(effect).endColor().append("\n");
        }

        sf.color(Color.GRAY).setItalic().append("Cooldown ").append(cooldown).setItalic(false).endColor();

        setItemTranslation(id + ".description", sf.toString());

    }
}
