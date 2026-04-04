package com.Team_Berry.Rooms.Commmands.RoomNPCCommand;

import com.Team_Berry.Rooms.Utils.RoomNPCSpawner;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

public class RoomNPCSpawnByRoomId extends AbstractPlayerCommand {
    private final RequiredArg<String> roomName;
    private final RequiredArg<String> npcId;

    public RoomNPCSpawnByRoomId(@NotNull String name, @NotNull String description) {
        super(name, description);
        this.npcId = this.withRequiredArg("NPCId", "The asset ID of the NPC (e.g. hytale:undead_skeleton)", ArgTypes.STRING);
        this.roomName = this.withRequiredArg("RoomName", "The asset name of the room", ArgTypes.STRING);
    }

    @Override
    protected void execute(@NotNull CommandContext commandContext, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        RoomNPCSpawner.spawnInRoomById(store, roomName.get(commandContext), npcId.get(commandContext));
    }
}