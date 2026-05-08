package com.Team_Berry.Artefacts.Artefact;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface ICooldownArtefact extends IArtefactLogic {
    long getCooldownDuration(int stacks);
    void onCooldownReady(ArtefactCodec codec, Ref<EntityStore> targetRef, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds);
}