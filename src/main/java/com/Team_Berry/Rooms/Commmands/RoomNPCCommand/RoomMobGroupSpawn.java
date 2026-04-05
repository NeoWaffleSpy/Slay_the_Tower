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

public class RoomMobGroupSpawn extends AbstractPlayerCommand {
    private final RequiredArg<String> roomName;
    private final RequiredArg<String> mobGroupId;

    public RoomMobGroupSpawn(@NotNull String name, @NotNull String description) {
        super(name, description);
        this.roomName = this.withRequiredArg("RoomName", "The asset name of the room", ArgTypes.STRING);
        this.mobGroupId = this.withRequiredArg("MobGroupId", "The ID of the Mob Group asset to spawn", ArgTypes.STRING);
    }

    @Override
    protected void execute(@NotNull CommandContext commandContext, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        RoomNPCSpawner.spawnMobGroupById(store, roomName.get(commandContext), mobGroupId.get(commandContext));
    }
}