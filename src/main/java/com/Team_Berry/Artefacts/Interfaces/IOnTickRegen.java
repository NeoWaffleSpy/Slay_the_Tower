package com.Team_Berry.Artefacts.Interfaces;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface IOnTickRegen extends IArtefactLogic {
    void onTickRegen(ArtefactCodec codec, int stacks, Ref<EntityStore> playerRef, Store<EntityStore> store, float dt);
}