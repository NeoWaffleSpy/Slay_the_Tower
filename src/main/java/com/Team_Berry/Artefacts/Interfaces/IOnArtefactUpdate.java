package com.Team_Berry.Artefacts.Interfaces;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface IOnArtefactUpdate extends IArtefactLogic {
    void onStackChange(ArtefactCodec codec, Ref<EntityStore> targetRef, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds, int currentStacks);
}