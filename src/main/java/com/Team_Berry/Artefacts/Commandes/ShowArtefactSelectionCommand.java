package com.Team_Berry.Artefacts.Commandes;

import com.Team_Berry.Artefacts.UI.ArtefactSelection;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

public class ShowArtefactSelectionCommand extends AbstractPlayerCommand {
    private final DefaultArg<Integer> count;
    public ShowArtefactSelectionCommand() {
        super("randomSelect", "Show the Artefact Selection page");
        this.count = withDefaultArg("count", "The amount of artefact to choose from", ArgTypes.INTEGER, 3, "3");
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        new ArtefactSelection(playerRef, store).buildPageWithCount(count.get(commandContext));
    }
}
