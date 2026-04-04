package com.Team_Berry.Rooms.Commmands.RoomTeleporterCommand;

import com.Team_Berry.Rooms.Utils.RoomTeleporter;
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

public class RoomTeleporterById extends AbstractPlayerCommand {
    private final RequiredArg<String> roomName;


    public RoomTeleporterById(@NotNull String name, @NotNull String description) {
        super(name, description);
        this.roomName = this.withRequiredArg("RoomName", "The name of the room you want to teleport to", ArgTypes.STRING);

    }

    @Override
    protected void execute(@NotNull CommandContext commandContext, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        RoomTeleporter.teleportToRoomById(playerRef, roomName.get(commandContext));
    }
}
