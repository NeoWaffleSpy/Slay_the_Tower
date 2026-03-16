package com.Team_Berry.Camera.Commands.Cinematic;

import com.Team_Berry.Camera.Cinematic.CinematicManager;
import com.Team_Berry.Camera.Cinematic.CinematicPlayer;
import com.Team_Berry.Camera.Component.Data.PlayerPOVComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nonnull;

public class DeletePointToCinematicCommand extends AbstractPlayerCommand {
    @Nonnull private final RequiredArg<String> nameArg;
    @Nonnull private final RequiredArg<Integer> keyframe;

    public DeletePointToCinematicCommand() {
        super("delete", "delete a keyframe");
        this.nameArg = this.withRequiredArg("Cinematic Name", "The name of the cinematic", ArgTypes.STRING);
        this.keyframe = this.withRequiredArg("keyframe", "Keyframe value to delete", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        CinematicPlayer c = CinematicManager.getCinematic(this.nameArg.get(commandContext));
        if (c == null) {
            commandContext.sendMessage(Message.raw("Cinematic Player Not Found: " + this.nameArg.get(commandContext)));
            return;
        }
        int keyframe = this.keyframe.get(commandContext);
        if (c.getCinemaPoint(keyframe) == null) {
            commandContext.sendMessage(Message.raw("Cinematic Point Not Found: " + this.keyframe.get(commandContext)));
            return;
        }
        c.removeCinemaPoint(keyframe);
    }
}
