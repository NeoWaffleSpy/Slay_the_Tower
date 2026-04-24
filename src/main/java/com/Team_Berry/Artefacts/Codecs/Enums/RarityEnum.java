package com.Team_Berry.Artefacts.Codecs.Enums;

import com.hypixel.hytale.protocol.io.ProtocolException;

public enum RarityEnum {
    COMMON(0.6f, 0),
    RARE(0.3f, 1),
    LEGENDARY(0.1f, 2),

    STARTER(0f, 3),
    DEBUG(0f, 4),
    INVISIBLE(0f, 5);

    public final float weight;
    public final int index;
    private RarityEnum(float weight, int index) {
        this.weight = weight;
        this.index = index;
    }
}
