package com.Team_Berry.Artefacts.Interfaces;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface IOnKill extends IArtefactLogic {
    void onKill(ArtefactCodec codec, Ref<EntityStore> attackerRef, Ref<EntityStore> deadEntityRef, StatEffectComponent statComp, Store<EntityStore> store, CommandBuffer<EntityStore> cmds);
}