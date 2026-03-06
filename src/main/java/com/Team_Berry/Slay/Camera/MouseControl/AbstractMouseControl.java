package com.Team_Berry.Slay.Camera.MouseControl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public abstract class AbstractMouseControl {
    protected Ref<EntityStore> ref;

    public void onPlayerMouseButton(@Nonnull PlayerMouseButtonEvent event) {
        ref = event.getPlayerRef();
        if (ref.isValid()) {
            sortData(event);
            if (event.getMouseButton().mouseButtonType == MouseButtonType.Left)
                onLeftClick(event);
            else if (event.getMouseButton().mouseButtonType == MouseButtonType.Right)
                onRightClick(event);
            else if (event.getMouseButton().mouseButtonType == MouseButtonType.Middle)
                onMiddleClick(event);
            else if (event.getMouseButton().mouseButtonType == MouseButtonType.X1)
                onThumb1Click(event);
            else if (event.getMouseButton().mouseButtonType == MouseButtonType.X2)
                onThumb2Click(event);
        }
    }

    protected abstract void sortData(@Nonnull PlayerMouseButtonEvent event);

    protected abstract void onLeftClick(@Nonnull PlayerMouseButtonEvent event);
    protected abstract void onRightClick(@Nonnull PlayerMouseButtonEvent event);
    protected abstract void onMiddleClick(@Nonnull PlayerMouseButtonEvent event);
    protected abstract void onThumb1Click(@Nonnull PlayerMouseButtonEvent event);
    protected abstract void onThumb2Click(@Nonnull PlayerMouseButtonEvent event);

    public abstract AbstractMouseControl clone();
}
