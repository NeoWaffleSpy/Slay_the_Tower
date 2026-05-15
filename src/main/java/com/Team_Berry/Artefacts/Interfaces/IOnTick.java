package com.Team_Berry.Artefacts.Interfaces;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface IOnTick extends IArtefactLogic {
    void onTick(ArtefactCodec codec, int stacks, Ref<EntityStore> playerRef, StatEffectComponent statComp, Store<EntityStore> store, float dt);
}