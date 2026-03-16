package com.Team_Berry.Camera.Cinematic;

import com.hypixel.hytale.protocol.ServerCameraSettings;

public class CinemaPoint extends ServerCameraSettings {
    public long transitionTime = 1000L;

    public CinemaPoint(ServerCameraSettings cameraSettings, long transitionTime) {
        super(cameraSettings);
        this.transitionTime = transitionTime;
    }

    public CinemaPoint(CinemaPoint cinemaPoint) {
        super(cinemaPoint);
        this.transitionTime = cinemaPoint.transitionTime;
    }

    public CinemaPoint() {
        super();
    }

    public CinemaPoint clone() {
        return new CinemaPoint(this);
    }
}
