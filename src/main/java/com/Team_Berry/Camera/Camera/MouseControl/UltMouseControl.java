package com.Team_Berry.Camera.Camera.MouseControl;

import com.Team_Berry.Slay.Component.Ult.UltExplosionComponent;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class UltMouseControl extends AbstractMouseControl{
    private long lastClickTime = 0;
    public static ComponentType<EntityStore, UltExplosionComponent> ULT_EXPLOSION_COMPONENT_TYPE;

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
        Ref<EntityStore> ref = event.getPlayerRef();
        if (event.getTargetBlock() == null && event.getTargetEntity() == null) return;

        Vector3d targetPos = null;
        if (event.getTargetBlock() != null) {
            Vector3i block = event.getTargetBlock();
            targetPos = new Vector3d(block.getX() + 1, block.getY() + 1, block.getZ() + 1);
        } else if (event.getTargetEntity() != null) {
            TransformComponent tc = store.getComponent(event.getTargetEntity().getReference(), TransformComponent.getComponentType());
            if (tc != null) targetPos = tc.getPosition();
        }

        if (targetPos != null) {
            if (store.getComponent(ref, ULT_EXPLOSION_COMPONENT_TYPE) == null) {
                long now = store.getResource(TimeResource.getResourceType()).getNow().toEpochMilli();

                //UltExplosionComponent ultExplosionComponent = new UltExplosionComponent(500L, now, targetPos);
                UltExplosionComponent ultExplosionComponent = new UltExplosionComponent(targetPos);

                store.getExternalData().getWorld().execute(() -> {

                    store.addComponent(ref, ULT_EXPLOSION_COMPONENT_TYPE, ultExplosionComponent);
                });
            } else {
                event.getPlayerRefComponent().sendMessage(Message.raw("Explosion in progress!"));
            }
        }

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
