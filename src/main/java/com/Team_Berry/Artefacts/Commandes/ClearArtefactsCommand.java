package com.Team_Berry.Artefacts.Commandes;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.Team_Berry.Artefacts.UI.ArtefactSelection;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import java.awt.*;

public class ClearArtefactsCommand extends AbstractPlayerCommand {
    public ClearArtefactsCommand() {
        super("clear", "clear all artefacts on the character");
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        StatEffectComponent statComp = store.getComponent(ref, StatEffectComponent.getComponentType());
        if (statComp == null) {
            playerRef.getReference().getStore().addComponent(playerRef.getReference(), StatEffectComponent.getComponentType(), new StatEffectComponent());
            statComp = store.getComponent(ref, StatEffectComponent.getComponentType());
            if (statComp == null) {
                commandContext.sendMessage(Message.raw("Error while initializing StatEffectComponent").color(Color.RED));
                ArtefactPlugin.LOGGER.atSevere().log("Error while initializing StatEffectComponent");
                return;
            }
        }
        statComp.flush();
    }
}
