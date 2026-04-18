package com.Team_Berry.Rooms.Commmands.RoomTeleporterCommand;

import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RoomTeleporterRandom extends AbstractPlayerCommand {
    public RoomTeleporterRandom(@NotNull String name, @NotNull String description) {
        super(name, description);
    }

    @Override
    protected void execute(@NotNull CommandContext commandContext, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        List<RoomCodec> rooms = new ArrayList<>(RoomCodec.getAssetMap().getAssetMap().values());

        if (!rooms.isEmpty()) {
            RoomCodec randomRoom = rooms.get(ThreadLocalRandom.current().nextInt(rooms.size()));
            // RoomTeleporter.teleportToRoom(playerRef, randomRoom);
        } else {
            playerRef.sendMessage(Message.raw("No rooms exist"));
        }
    }
}
