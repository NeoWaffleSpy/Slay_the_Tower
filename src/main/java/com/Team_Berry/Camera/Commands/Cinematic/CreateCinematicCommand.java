package com.Team_Berry.Camera.Commands.Cinematic;

import com.Team_Berry.Camera.Cinematic.CinematicManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class CreateCinematicCommand extends AbstractPlayerCommand {
    @Nonnull
    private final RequiredArg<String> nameArg;
    private final DefaultArg<Boolean> force;

    protected CreateCinematicCommand() {
        super("create", "Create a cinematic with the given name");
        this.nameArg = this.withRequiredArg("name", "The cinematic name", ArgTypes.STRING);
        this.force = this.withDefaultArg("force", "The cinematic name", ArgTypes.BOOLEAN, false, "false");
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        String cinemaName = this.nameArg.get(commandContext);
        if (!this.force.get(commandContext) && (CinematicManager.getCinematic(cinemaName) != null || CinematicManager.get(cinemaName) != null)) {
            commandContext.sendMessage(Message.raw("A cinematic with this name already exists, use --force true to override"));
            return;
        }
        Vector3d t = store.getComponent(ref, TransformComponent.getComponentType()).getPosition();
        if (t == null) {
            commandContext.sendMessage(Message.raw("Failed to create cinematic"));
            return;
        }
        Position p = new Position(t.x, t.y, t.z);
        CinematicManager.createCinematic(cinemaName, p);
    }
}
