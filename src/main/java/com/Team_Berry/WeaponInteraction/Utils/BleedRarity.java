package com.Team_Berry.WeaponInteraction.Utils;

public enum BleedRarity {
    COMMON("Bleed_Damage_Flat_Common"),
    RARE("Bleed_Damage_Flat_Rare"),
    LEGENDARY("Bleed_Damage_Flat_Legendary"),
    NONE("None");

    private final String damageKey;

    BleedRarity(String damageKey) {
        this.damageKey = damageKey;
    }

    public String getDamageKey() {
        return this.damageKey;
    }

    // This handles your "it could be anywhere in the string" requirement
    public static BleedRarity fromString(String input) {
        if (input == null) return NONE;
        String lower = input.toLowerCase();

        if (lower.contains("legendary")) return LEGENDARY;
        if (lower.contains("rare")) return RARE;
        if (lower.contains("common")) return COMMON;

        return NONE;
    }
}
