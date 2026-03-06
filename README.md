# Gamemode API

This is a basic API to create pseudo gamemodes with custom camera and custom mouse controle and interactions.
This is still a work in progress.

## Set up a custom gamemode
### Custom Camera
Simply create a new instance of `ServerCameraSettings` with your custom settings (see official [doc](https://hytalemodding.dev/en/docs/guides/plugin/customizing-camera-controls))

Built-in camera modes:
- topDown
- sideView
- isometric
- isometric2 (functionnaly the same but with a more complete constructor)

Example of a top-down view
```Java
ServerCameraSettings cameraSettings = new ServerCameraSettings();
cameraSettings.positionLerpSpeed = 0.2F;
cameraSettings.rotationLerpSpeed = 0.2F;
cameraSettings.distance = 20.0F;
cameraSettings.displayCursor = true;
cameraSettings.sendMouseMotion = true;
cameraSettings.isFirstPerson = false;
cameraSettings.movementForceRotationType = MovementForceRotationType.Custom;
cameraSettings.eyeOffset = true;
cameraSettings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
cameraSettings.rotationType = RotationType.Custom;
cameraSettings.rotation = new Direction(0.0F, (-(float)Math.PI / 2F), 0.0F);
cameraSettings.mouseInputType = MouseInputType.LookAtPlane;
cameraSettings.planeNormal = new Vector3f(0.0F, 1.0F, 0.0F);
CameraTemplates.set("myCustomTemplate", cameraSettings);
```
### Custom Mouse Control
To create a custom mouse controle, create a new java class that extend `AbstractMouseControl` and override
the mouse button abstract methods, the default mouse controle is adapted to top-down view
but not very adapted to the others.

the abstract class handle the mouse event dispatch and some data parsing, but you can override it
if you need more control.

### Create the camera instance
Now in the `setup()` of your plugin, create the cameraSetting, the MouseControle class and after that,
create the camera instance with the code
```Java
new CameraInitializer("myInstanceName", new myCustomMouseControle(), false, "myCameraTemplate");
```
The `false` is a WIP setting to have the player invisible, it works but crash the client,
so I recommend keeping it to false.

Example with the available default options:
```Java
new CameraInitializer("isometricCamera2", new DefaultMouseControl(), false, "isometric");
```

### Apply Camera to a player
To apply or remove the custom gamemode, simply add the `PlayerPOVComponent` to the player.
Do not forget to remove it if it was already present as it won't register properly otherwise.
```Java
PlayerPOVComponent pPOV = store.getComponent(ref, PlayerPOVComponent.getComponentType());
if (pPOV != null)
    store.removeComponent(ref, PlayerPOVComponent.getComponentType());
store.addComponent(ref, PlayerPOVComponent.getComponentType(), new PlayerPOVComponent(name));
```

### Custom commands

- `/camera` *Command group for individual camera management*
- - `/camera get` *Get the name of the current camera instance*
- - `/camera reset` *Reset all camera, controls and setting to default*
- - `/camera set <cameraInstanceName>` *Apply the given camera configuration*
- `/cameraGroup` *Command group for global camera management*
- - `/cameraGroup activate <cameraInstanceName>`
- - `/cameraGroup deactivate <cameraInstanceName>`

**A disabled camera group reset all players using it to default and player cannot
use the configuration until reenabled**

****

## Technical issues and WIP stuff
The HidePlayer feature is a setting that exist but is unused in the game to make a kind
of spectator mode, I tried to use it, and to some extend it works, but it seem to have a bad
interaction with the MouseControle and consistently crash the client, but not the server.

The ECS part is a bit clunky, but if I wanted to use a component, I couldn't find another *cleaner*
way to do it.

I am working on a way to automatize the process with some event such as joining a specific instance
or if you use a specific item, but I need to have a working logic and way to make
it modular for easy configuration.

I welcome any feedback and advices to improve the idea.