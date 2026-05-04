package com.Team_Berry.Game.Managers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.Team_Berry.Utils.Scheduler.KeyedScheduler;
import com.Team_Berry.Game.GamePlugin;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class PlayerModelManager {
    private static final Random RANDOM = new java.util.Random();
    private static final List<String> STARTING_MODELS = Arrays.asList(
            "Skeleton", "Skeleton_Fighter", "Skeleton_Mage", "Skeleton_Pirate_Striker", "Skeleton_Knight"
    );

    private final Map<UUID, String> assignedModels = new HashMap<>();
    private final KeyedScheduler scheduler;

    public PlayerModelManager(KeyedScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void applyRandomStartingModel(PlayerRef playerRef) {
        scheduler.schedule("apply_model_" + playerRef.getUuid(), () -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();

                store.getExternalData().getWorld().execute(() -> {
                    String modelId = assignedModels.computeIfAbsent(playerRef.getUuid(),
                            k -> STARTING_MODELS.get(RANDOM.nextInt(STARTING_MODELS.size())));

                    ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelId);
                    if (modelAsset != null) {
                        Model model = Model.createScaledModel(modelAsset, 1.0f);
                        store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(model));

                        PlayerSkinComponent skinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
                        if (skinComponent != null) {
                            skinComponent.setNetworkOutdated();
                        }
                        GamePlugin.LOGGER.atInfo().log("Applied assigned model '" + modelId + "' to " + playerRef.getUsername());
                    } else {
                        GamePlugin.LOGGER.atInfo().log("Warning: Could not find ModelAsset for ID: " + modelId);
                    }
                });
            }
        }, 500, TimeUnit.MILLISECONDS);
    }

    public void resetPlayerModel(PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();

            store.getExternalData().getWorld().execute(() -> {
                PlayerSkinComponent skinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
                if (skinComponent != null) {
                    Model newModel = CosmeticsModule.get().createModel(skinComponent.getPlayerSkin());
                    store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(newModel));
                    skinComponent.setNetworkOutdated();

                    GamePlugin.LOGGER.atInfo().log("Restored original player skin for: " + playerRef.getUsername());
                }
            });
        }
    }
}