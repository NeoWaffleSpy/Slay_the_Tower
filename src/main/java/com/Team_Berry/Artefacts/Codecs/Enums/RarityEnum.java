package com.Team_Berry.Artefacts.Codecs.Enums;

public enum RarityEnum {
    COMMON { public final float weight = 0.6f; },
    RARE { public final float weight = 0.3f; },
    LEGENDARY { public final float weight = 0.1f; },

    STARTER { public final float weight = 0f; },
    DEBUG { public final float weight = 0f; }
}
