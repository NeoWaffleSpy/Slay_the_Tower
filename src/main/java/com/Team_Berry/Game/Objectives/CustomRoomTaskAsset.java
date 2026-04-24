package com.Team_Berry.Game.Objectives;

import com.hypixel.hytale.builtin.adventure.objectives.ObjectivePlugin;
import com.hypixel.hytale.builtin.adventure.objectives.config.task.ObjectiveTaskAsset;
import com.hypixel.hytale.builtin.adventure.objectives.task.ObjectiveTask;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

public class CustomRoomTaskAsset extends ObjectiveTaskAsset {
    @Nonnull
    public static final BuilderCodec<CustomRoomTaskAsset> CODEC = BuilderCodec.builder(
            CustomRoomTaskAsset.class,
            CustomRoomTaskAsset::new,
            ObjectiveTaskAsset.BASE_CODEC
    ).build();

    public CustomRoomTaskAsset() {
        super();
    }

    public static void registerCodecs() {
        ObjectiveTaskAsset.CODEC.register("CustomRoomTaskAsset", CustomRoomTaskAsset.class, CustomRoomTaskAsset.CODEC);
        ObjectiveTask.CODEC.register("CustomRoomTaskAsset", CustomRoomTask.class, CustomRoomTask.CODEC);
    }

    public static void registerLogic() {
        ObjectivePlugin plugin = ObjectivePlugin.get();
        if (plugin != null) {
            plugin.registerTask(
                    "CustomRoomTaskAsset",
                    CustomRoomTaskAsset.class,
                    CustomRoomTaskAsset.CODEC,
                    CustomRoomTask.class,
                    CustomRoomTask.CODEC,
                    CustomRoomTask::new
            );
            com.Team_Berry.Game.GamePlugin.LOGGER.atInfo().log("Successfully linked CustomRoomTask logic to Objective system!");
        } else {
            com.Team_Berry.Game.GamePlugin.LOGGER.atSevere().log("CRITICAL: ObjectivePlugin was null during registerLogic()!");
        }
    }

    @Nonnull
    @Override
    public TaskScope getTaskScope() {
        return TaskScope.PLAYER;
    }

    @Override
    protected boolean matchesAsset0(ObjectiveTaskAsset task) {
        return task instanceof CustomRoomTaskAsset;
    }
}