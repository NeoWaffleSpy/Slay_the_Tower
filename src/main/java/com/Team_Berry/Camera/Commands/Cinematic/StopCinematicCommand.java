package com.Team_Berry.Camera.Commands.Cinematic;

import com.Team_Berry.Camera.Cinematic.CinematicManager;
import com.Team_Berry.Camera.Cinematic.CinematicPlayer;
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
import org.jspecify.annotations.NonNull;

import java.awt.*;

public class StopCinematicCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> nameArg;
    public StopCinematicCommand() {
        super("stop", "Stop the current cinematic");
        this.nameArg = this.withRequiredArg("name", "The name of the cinematic to stop", ArgTypes.STRING);
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        CinematicPlayer c = CinematicManager.getCinematic(this.nameArg.get(commandContext));
        if (c == null) {
            commandContext.sendMessage(Message.raw("No cinematic player named " + nameArg.get(commandContext)).color(Color.RED));
            return;
        }
        c.stopCinematic(playerRef);
    }
}
