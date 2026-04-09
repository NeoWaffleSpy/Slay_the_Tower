package com.Team_Berry.Rooms.Commmands.RoomNPCCommand;

import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.Utils.RoomNPCSpawner;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RoomNPCSpawnRandomRoom extends AbstractPlayerCommand {
    private final RequiredArg<String> npcId;

    public RoomNPCSpawnRandomRoom(@NotNull String name, @NotNull String description) {
        super(name, description);
        this.npcId = this.withRequiredArg("NPCId", "The asset ID of the NPC", ArgTypes.STRING);
    }

    @Override
    protected void execute(@NotNull CommandContext commandContext, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        List<RoomCodec> rooms = new ArrayList<>(RoomCodec.getAssetMap().getAssetMap().values());

        if (!rooms.isEmpty()) {
            RoomCodec randomRoom = rooms.get(ThreadLocalRandom.current().nextInt(rooms.size()));
            RoomNPCSpawner.spawnInRoom(store, randomRoom, npcId.get(commandContext));
            playerRef.sendMessage(Message.raw("NPC Spawned"));
        } else {
            playerRef.sendMessage(Message.raw("No rooms found to spawn NPC in."));
        }
    }
}