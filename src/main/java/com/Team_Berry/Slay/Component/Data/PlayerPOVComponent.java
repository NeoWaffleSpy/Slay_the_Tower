package com.Team_Berry.Slay.Component.Data;

import com.Team_Berry.Slay.Camera.MouseControl.AbstractMouseControl;
import com.Team_Berry.Slay.SlayTheTower;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class PlayerPOVComponent implements Component<EntityStore> {
    private @NonNull String POVName;
    private ServerCameraSettings camSettings;
    private AbstractMouseControl mouseControl;

    public PlayerPOVComponent(@NonNull String POVName) {
        this.POVName = POVName;
        this.camSettings = null;
        this.mouseControl = null;
    }

    public PlayerPOVComponent(@NonNull String POVName, ServerCameraSettings camSettings, AbstractMouseControl mouseControl) {
        this.POVName = POVName;
        this.camSettings = camSettings;
        this.mouseControl = mouseControl;
    }


    public void setPOVName(@NonNull String POVName) { this.POVName = POVName; }
    public @NonNull String getPOVName() { return POVName; }
    public void setCamSettings(ServerCameraSettings camSettings) { this.camSettings = camSettings; }
    public ServerCameraSettings getCamSettings() { return camSettings; }
    public void setMouseControl(AbstractMouseControl mouseControl) { this.mouseControl = mouseControl; }
    public AbstractMouseControl getMouseControl() { return mouseControl; }

    public static @NonNull ComponentType<EntityStore, PlayerPOVComponent> getComponentType() {
        return SlayTheTower.get().getPlayerPOVComponentType();
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        return new PlayerPOVComponent(POVName, camSettings, mouseControl);
    }
}
