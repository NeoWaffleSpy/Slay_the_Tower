package com.Team_Berry.Artefacts.Codecs.Enums;

public enum StatEnum {
    NONE { public float compute(float a) { return 0; } },

    HEALTH { public float compute(float a) { return a; } },
    STAMINA { public float compute(float a) { return a; } },

    ATTACK_BONUS { public float compute(float a) { return a; } },
    ARMOR_BONUS { public float compute(float a) { return a; } },
}
