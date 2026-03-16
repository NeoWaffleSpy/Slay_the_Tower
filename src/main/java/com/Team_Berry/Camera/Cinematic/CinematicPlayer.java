package com.Team_Berry.Camera.Cinematic;

import com.Team_Berry.Camera.Camera.CameraInitializer;
import com.Team_Berry.Camera.Component.Data.PlayerPOVComponent;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class CinematicPlayer {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ArrayList<CinemaPoint> timeline = new ArrayList<>();
    public Position origin;

    public CinematicPlayer(Position origin) {
        this.origin = origin;
    }

    public void runCinematic(PlayerRef player, Position origin, World world, CommandContext commandContext) {
        PlayerPOVComponent pPOV = player.getReference().getStore().getComponent(player.getReference(), PlayerPOVComponent.getComponentType());
        if (pPOV == null)
            CameraInitializer.setPlayerPov("custom", player);
        if (commandContext != null)
            commandContext.sendMessage(Message.raw("Starting Cinematic Player").color(Color.GREEN));
        AtomicReference<Long> delta = new AtomicReference<>(0L);
        AtomicReference<Integer> index = new AtomicReference<>(1);

        /* Cinematic Player */
        timeline.forEach(point -> {
            scheduler.schedule(() -> {
                if (commandContext != null)
                    commandContext.sendMessage(Message.raw("Cinema Keyframe " + index.get()).color(Color.CYAN));
                world.execute(() -> {
                    CinemaPoint tmp = point.clone();
                    tmp.position = addPosition(tmp.position, origin);
                    CameraInitializer.editCameraSettings(player, tmp);
                });
                index.updateAndGet(i -> i + 1);
            }, delta.get(), TimeUnit.MILLISECONDS);
            delta.updateAndGet(v -> v + point.transitionTime);
        });

        /* Transition back to the Player */
        ServerCameraSettings s;
        if (pPOV != null)
            s = pPOV.getCamSettings();
        else {
            s = new ServerCameraSettings();
            s.eyeOffset = true;
        }
        s.rotationLerpSpeed = 0.05f;
        s.positionLerpSpeed = 0.05f;
        scheduler.schedule(() -> world.execute(() -> {
            CameraInitializer.editCameraSettings(player, s);
        }), delta.get(), TimeUnit.MILLISECONDS);
        delta.updateAndGet(v -> v + 1000);

        /* Reload original Camera */
        scheduler.schedule(() -> {
            if (commandContext != null)
                commandContext.sendMessage(Message.raw("Stopping Cinematic Player").color(Color.RED));
            world.execute(() -> CameraInitializer.reload(player));
        }, delta.get(), TimeUnit.MILLISECONDS);
    }

    public void addCameraPoint(ServerCameraSettings cameraSettings, long transitionTime) {
        timeline.add(new CinemaPoint(cameraSettings, transitionTime));
    }

    public void addCameraPoint(CinemaPoint cinemaPoint) {
        timeline.add(cinemaPoint);
    }

    public CinemaPoint getCinemaPoint(int index) {
        return timeline.get(index);
    }

    public void editCinemaPoint(int index, ServerCameraSettings cinemaPoint) {
        timeline.add(index, new CinemaPoint(cinemaPoint, timeline.get(index).transitionTime));
        removeCinemaPoint(index + 1);
    }

    public void editCinemaPoint(int index, CinemaPoint cinemaPoint) {
        removeCinemaPoint(index);
        timeline.add(index, cinemaPoint);
    }

    public void removeCinemaPoint(int index) {
        timeline.remove(index);
    }

    public void setOrigin(Position origin) {
        this.origin = origin;
    }

    private Position addPosition(Position a, Position b) {
        return new Position(a.x + b.x, a.y + b.y, a.z + b.z);
    }
}
