package com.Team_Berry.Game.Components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.Nullable;

public class QuestNPCComponent implements Component<EntityStore> {
    @Override
    public @Nullable Component<EntityStore> clone() {
        return new QuestNPCComponent();
    }
}
