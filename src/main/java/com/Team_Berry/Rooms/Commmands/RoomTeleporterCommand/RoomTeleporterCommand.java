package com.Team_Berry.Rooms.Commmands.RoomTeleporterCommand;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class RoomTeleporterCommand extends AbstractCommandCollection {
    public RoomTeleporterCommand() {
        super("RoomTeleporter", "RoomTeleporter command collection");
        addSubCommand(new RoomTeleporterById("room", "Teleport to the room by its asset name."));
        addSubCommand(new RoomTeleporterRandom("random", "Teleport to a random room"));
    }
}
