package com.Team_Berry.Game.Commands;

import com.Team_Berry.Game.GameManager;
import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

public class GetParticipantsIndices extends AbstractPlayerCommand {

    public GetParticipantsIndices(@NotNull String name, @NotNull String description) {
        super(name, description);
    }

    @Override
    protected void execute(
            @NotNull CommandContext commandContext,
            @NotNull Store<EntityStore> store,
            @NotNull Ref<EntityStore> ref,
            @NotNull PlayerRef playerRef,
            @NotNull World world) {

        GameManager manager = GamePlugin.get().getGameManager(world);

        if (manager == null) {
            playerRef.sendMessage(Message.raw("No active game running in this world."));
            return;
        }

        String output = manager.getParticipantIndices(manager.getActiveParticipants());

        if (output.isEmpty()) {
            playerRef.sendMessage(Message.raw("No active participants found."));
            return;
        }

        playerRef.sendMessage(Message.raw(output));
    }
}