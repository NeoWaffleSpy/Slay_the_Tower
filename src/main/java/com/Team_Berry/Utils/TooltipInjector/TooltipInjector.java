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
    private static final Color tierLow = Color.GREEN;
    private static final Color tierMed = Color.ORANGE;
    private static final Color tierHigh = Color.RED;
    private static final Color colorMobility = new Color(255, 215, 0);
    private static final Color colorDamage = new Color(200, 42, 42);
    private static final Color colorKnockback = new Color(149, 157, 255);
    private static final Color colorEffect = Color.GREEN;
    private static final Color textWhite = Color.WHITE;
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
        injectDaggerTooltips();
        injectBowTooltips();
        injectArtifactTooltips();

    }

    public static void injectDaggerTooltips() {
        registerSkill("DaggerThrow", false, tierLow, "Low", null, null, null, "1.2");
        registerSkill("Disengage", true, tierLow, "Low", null, null, null, "6");
        registerSkill("SecondWind", true, null, null, null, null, "Invincibility", "30");
        registerSkill("ShadowDash", true, tierMed, "Medium", null, null, "Stun", "8");
        registerSkill("PocketBomb", false, tierHigh, "High", tierMed, "Medium", null, "8");
        registerSkill("TwinStab", false, tierHigh, "High", null, null, null, "1.9");
        registerSkill("Whirl", true, tierHigh, "High", null, null, null, "7");
        registerSkill("WideSlash", false, tierMed, "Medium", tierMed, "Medium", null, "4");
    }

    public static void injectBowTooltips() {
        registerSkill("BombShot", false, tierMed, "Medium", null, null, null, "8");
        registerSkill("Burst", true, tierHigh, "High", null, null, "Speed", "10");
        registerSkill("ElectricShot", false, tierHigh, "High", null, null, null, "8");
        registerSkill("FrostVolley", false, tierLow, "Low", null, null, "Slow", "5");
        registerSkill("PoisonCloud", false, tierMed, "dps", null, null, null, "17");
        registerSkill("JumpShot", true, tierMed, "Medium", null, null, null, "5");
        registerSkill("WolfForm", true, tierLow, "Low", null, null, "Speed", "6");
        registerSkill("BearForm", false, tierHigh, "High", tierHigh, "High", null, "8");
    }

    private static void registerSkill(String id, boolean hasMobility,
                                      Color damageTierColor, String damageTier,
                                      Color kbTierColor, String kbTier,
                                      String effect,
                                      String cooldown) {
        StringFormatter sf = new StringFormatter();


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

    public static void injectArtifactTooltips() {
        Color colorDamage = new Color(200, 42, 42);
        Color colorUtility = new Color(149, 157, 255);
        Color colorMobility = new Color(255, 215, 0);
        Color colorHealth = Color.GREEN;

        registerArtifact("Bleed_Artefact", "Rare", "Bleed Chance", "+10%", colorDamage, "+10% Chance");

        registerArtifact("Critical_Damage", "Rare", "Crit Damage", "+20%", colorDamage, null);

        registerArtifact("Critical", "Common", "Crit Chance", "+10%", colorDamage, null);

        registerArtifact("Debuff", "Rare", "Effect", "Armor Shred", colorUtility, null);

        registerArtifact("Explosion_On_Kill_Artefact", "Rare", "Effect", "Explosion on Kill", colorDamage, "+5 Explosion Damage");

        registerArtifact("Health", "Common", "Max Health", "+10", colorHealth, null);

        registerArtifact("MultDamage", "Common", "Damage", "+10%", colorDamage, null);

        registerArtifact("Projectile_Reflect_Artefact", "Legendary", "Effect", "Reflect Projectiles", colorUtility, "+10 Reflected Damage");

        registerArtifact("Shield_Artefact", "Rare", "Base Cooldown", "60s", colorUtility, "-10% Cooldown (min 10s)");

        registerArtifact("Speed_On_Kill_Artefact", "Common", "Effect", "Speed on Kill", colorMobility, "Increases speed gained");

        registerArtifact("Stamina_Artefact", "Common", "Max Stamina", "+5", colorMobility, null);
    }

    private static void registerArtifact(String id, String rarity,
                                         String primaryStatName, String primaryStatValue, Color primaryColor,
                                         String stackEffect) {
        StringFormatter sf = new StringFormatter();

        Color colorCommon = Color.LIGHT_GRAY;
        Color colorRare = new Color(50, 150, 255);
        Color colorLegendary = new Color(255, 165, 0);
        Color colorStack = new Color(255, 105, 180);
        Color textWhite = Color.WHITE;

        Color rarityColor = colorCommon;
        if (rarity.equalsIgnoreCase("Rare")) rarityColor = colorRare;
        else if (rarity.equalsIgnoreCase("Legendary")) rarityColor = colorLegendary;

        sf.color(rarityColor).setBold().append(rarity).setBold(false).endColor().append("\n");

        String originalDesc = getItemTranslation(id + ".description");
        if (originalDesc != null) {
            sf.color(textWhite).append(originalDesc).endColor().append("\n");
        }

        if (primaryStatName != null && primaryStatValue != null) {
            sf.color(primaryColor).append(primaryStatName).append(": ")
                    .color(textWhite).append(primaryStatValue).endColor().append("\n");
        }

        if (stackEffect != null) {
            sf.color(colorStack).setItalic().append("Per Stack: ")
                    .color(textWhite).append(stackEffect).setItalic(false).endColor().append("\n");
        }

        setItemTranslation(id + ".description", sf.toString().trim());
    }
}
