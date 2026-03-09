package com.Team_Berry.Slay.Camera.MouseControl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class UltMouseControl extends AbstractMouseControl{
    private long lastClickTime = 0;
    @Override
    protected void sortData(@NotNull PlayerMouseButtonEvent event) {

    }


    @Override
    protected void onLeftClick(@NotNull PlayerMouseButtonEvent event) {
        if (event.getMouseButton().state != MouseButtonState.Released) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < 50) return;
        lastClickTime = currentTime;

        Store<EntityStore> store = event.getPlayerRef().getStore();

        if (event.getTargetBlock() == null && event.getTargetEntity() == null) return;

        //store.getExternalData().getWorld().execute(() -> {
            Vector3d targetPos = null;
            if (event.getTargetBlock() != null) {
                Vector3i block = event.getTargetBlock();
                targetPos = new Vector3d(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
            } else if (event.getTargetEntity() != null) {
                TransformComponent tc = store.getComponent(event.getTargetEntity().getReference(), TransformComponent.getComponentType());
                if (tc != null) targetPos = tc.getPosition();
            }

            if (targetPos != null) {
                ParticleUtil.spawnParticleEffect("Explosion_Medium", targetPos, store);

                List<Ref<EntityStore>> hits = TargetUtil.getAllEntitiesInSphere(targetPos, 5.0, store);
                for (Ref<EntityStore> entityRef : hits) {
                    if (entityRef.isValid() && !entityRef.equals(event.getPlayerRef())) {
                        event.getPlayerRefComponent().sendMessage(Message.raw("Found entity!"));
                    }
                }
            }
        //});
    }

    @Override
    protected void onRightClick(@NotNull PlayerMouseButtonEvent event) {


    }

    @Override
    protected void onMiddleClick(@NotNull PlayerMouseButtonEvent event) {

    }

    @Override
    protected void onThumb1Click(@NotNull PlayerMouseButtonEvent event) {

    }

    @Override
    protected void onThumb2Click(@NotNull PlayerMouseButtonEvent event) {

    }

    @Override
    public AbstractMouseControl clone() {
        return null;
    }
}
