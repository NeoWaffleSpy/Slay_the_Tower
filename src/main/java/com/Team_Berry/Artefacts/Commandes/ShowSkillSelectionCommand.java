package com.Team_Berry.Artefacts.Commandes;

import com.Team_Berry.Artefacts.UI.ArtefactSelection;
import com.Team_Berry.Artefacts.UI.SkillSelection;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

public class ShowSkillSelectionCommand extends AbstractPlayerCommand {
    public ShowSkillSelectionCommand() {
        super("skillSelect", "Show the Artefact Selection page");
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        new SkillSelection(playerRef, store).buildPage();
    }
}
