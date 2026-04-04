package com.Team_Berry.Artefacts.Commandes;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import java.awt.*;

public class GiveArtefactCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> artefactName;
    private final DefaultArg<Integer> artefactCount;
    public GiveArtefactCommand() {
        super("give", "Give an artefact to a player");
        this.artefactName = this.withRequiredArg("ArtefactName", "The name of the artefact to give", ArgTypes.STRING);
        this.artefactCount = this.withDefaultArg("Amount", "amount of artefacts to give", ArgTypes.INTEGER, 1, "1");
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
        ArtefactCodec artefact = ArtefactCodec.getAssetMap().getAsset(artefactName.get(commandContext));
        if (artefact == null) {
            commandContext.sendMessage(Message.raw("No valid artefact with this name").color(Color.RED));
            return;
        }
        if (artefactCount.provided(commandContext))
            statComp.setStackArtefact(artefact, artefactCount.get(commandContext));
        else
            statComp.addArtifact(artefact);
    }
}
