package com.Team_Berry.WeaponInteraction.Utils;

public enum BleedStage {
    NONE("None"),
    STAGE1("Bleed_Effect_Stage1"),
    STAGE2("Bleed_Effect_Stage2"),
    STAGE3("Bleed_Effect_Stage3"),
    BLEEDING("Bleed_Effect_Bleeding");

    private final String effectKey;

    // Constructor to link the enum constant to the effect string
    BleedStage(String effectKey) {
        this.effectKey = effectKey;
    }

    public String getEffectKey() {
        return this.effectKey;
    }
}