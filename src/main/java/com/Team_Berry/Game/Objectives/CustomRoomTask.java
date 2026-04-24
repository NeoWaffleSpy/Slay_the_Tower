package com.Team_Berry.Game.Objectives;

import com.hypixel.hytale.builtin.adventure.objectives.Objective;
import com.hypixel.hytale.builtin.adventure.objectives.task.ObjectiveTask;
import com.hypixel.hytale.builtin.adventure.objectives.transaction.TransactionRecord;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class CustomRoomTask extends ObjectiveTask {
    @Nonnull
    public static final BuilderCodec<CustomRoomTask> CODEC = BuilderCodec.builder(
            CustomRoomTask.class,
            CustomRoomTask::new,
            ObjectiveTask.BASE_CODEC
    ).build();

    private int currentProgress = 0;
    private int maxProgress = 1;

    protected CustomRoomTask() {
        super();
    }

    public CustomRoomTask(CustomRoomTaskAsset asset, int taskSetIndex, int taskIndex) {
        super(asset, taskSetIndex, taskIndex);
    }

    @Override
    public boolean checkCompletion() {
        return false;
    }

    @Override
    protected TransactionRecord[] setup0(Objective objective, World world, Store<EntityStore> store) {
        return new TransactionRecord[0];
    }

    public void setProgress(int current, int max) {
        this.currentProgress = current;
        this.maxProgress = Math.max(1, max);
    }

    @Override
    public com.hypixel.hytale.protocol.ObjectiveTask toPacket(Objective objective) {
        com.hypixel.hytale.protocol.ObjectiveTask packet = new com.hypixel.hytale.protocol.ObjectiveTask();
        packet.currentCompletion = this.currentProgress;
        packet.completionNeeded = this.maxProgress;
        packet.taskDescriptionKey = Message.raw("Enemies killed").getFormattedMessage();
        return packet;
    }
}