package com.Team_Berry.Game.Components;

import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.Nullable;

public class QuestNPCComponent implements Component<EntityStore> {
    RoomCodec room = null;

    public QuestNPCComponent() {
    }

    public QuestNPCComponent(RoomCodec room) {
        this.room = room;
    }


    @Override
    public @Nullable Component<EntityStore> clone() {
        QuestNPCComponent copy = new QuestNPCComponent(this.room);
        return copy;
    }

    public RoomCodec getRoom() {
        return room;
    }
}
