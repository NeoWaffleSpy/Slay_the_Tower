package com.Team_Berry.Camera.Component.Data;

import com.Team_Berry.Camera.Camera.MouseControl.AbstractMouseControl;
import com.Team_Berry.Camera.CameraPlugin;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class PlayerPOVComponent implements Component<EntityStore> {
    private @NonNull String POVName;
    private String cinematicName;
    private ServerCameraSettings camSettings;
    private AbstractMouseControl mouseControl;

    public PlayerPOVComponent(@NonNull String POVName) {
        this.POVName = POVName;
        this.cinematicName = null;
        this.camSettings = null;
        this.mouseControl = null;
    }

    public PlayerPOVComponent(@NonNull String POVName, String cinematicName, ServerCameraSettings camSettings, AbstractMouseControl mouseControl) {
        this.POVName = POVName;
        this.cinematicName = cinematicName;
        this.camSettings = camSettings;
        this.mouseControl = mouseControl;
    }


    public void setPOVName(@NonNull String POVName) { this.POVName = POVName; }
    public @NonNull String getPOVName() { return POVName; }
    public void setCinematicName(@NonNull String cinematicName) { this.cinematicName = cinematicName; }
    public @NonNull String getCinematicName() { return cinematicName; }
    public void setCamSettings(ServerCameraSettings camSettings) { this.camSettings = camSettings; }
    public ServerCameraSettings getCamSettings() { return camSettings; }
    public void setMouseControl(AbstractMouseControl mouseControl) { this.mouseControl = mouseControl; }
    public AbstractMouseControl getMouseControl() { return mouseControl; }

    public static @NonNull ComponentType<EntityStore, PlayerPOVComponent> getComponentType() {
        return CameraPlugin.get().getPlayerPOVComponentType();
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        return new PlayerPOVComponent(POVName, cinematicName, camSettings, mouseControl);
    }
}
