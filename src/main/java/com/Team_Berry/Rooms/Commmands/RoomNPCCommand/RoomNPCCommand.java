package com.Team_Berry.Rooms.Commmands.RoomNPCCommand;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class RoomNPCCommand extends AbstractCommandCollection {
    public RoomNPCCommand() {
        super("RoomNPC", "Commands for spawning NPCs within defined room areas");
        addSubCommand(new RoomNPCSpawnByRoomId("spawn", "Spawn a specific NPC in a specific room."));
        addSubCommand(new RoomNPCSpawnRandomRoom("random", "Spawn a specific NPC in a random room."));
        addSubCommand(new RoomMobGroupSpawn("mobgroup", "Spawn a specific Mob Group in a specific room."));
    }
}