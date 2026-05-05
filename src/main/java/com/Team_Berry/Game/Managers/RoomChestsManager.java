package com.Team_Berry.Game.Managers;

import com.Team_Berry.Game.GamePlugin;
import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RoomChestsManager {
    private static final String RELICS_CHEST = "Relics_Chest";
    private final World world;

    public RoomChestsManager(World world) {
        this.world = world;
    }

    private void log(String message) {
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", world.getName(), message));
    }

    public void breakBlockAt(BlockPosition pos) {
        if (pos == null || world == null) return;

        world.execute(() -> {
            boolean success = world.breakBlock(pos.x, pos.y, pos.z, 0);

            if (success) {
                log("Successfully broke the block at " + pos.x + ", " + pos.y + ", " + pos.z);
            } else {
                log("Failed to break block at " + pos.x + ", " + pos.y + ", " + pos.z + ". It may already be empty.");
            }
        });
    }

    public void setupRoomChests(RoomCodec room) {
        if (room == null || room.chestPositions == null || room.chestPositions.length == 0) {
            return;
        }

        world.execute(() -> {

            int restoredCount = 0;
            for (BlockPosition pos : room.chestPositions) {
                BlockType currentBlock = world.getBlockType(pos.x, pos.y, pos.z);

                if (currentBlock == null || !currentBlock.getId().equals(RELICS_CHEST)) {
                    world.setBlock(pos.x, pos.y, pos.z, RELICS_CHEST);
                    restoredCount++;
                }
            }

            if (restoredCount > 0) {
                log("Restored " + restoredCount + " missing " + RELICS_CHEST + "(s) in room " + room.getId());
            }

            List<BlockPosition> validChests = new ArrayList<>(Arrays.asList(room.chestPositions));
            Collections.shuffle(validChests);

            int amountToBreak = validChests.size() / 2;

            for (int i = 0; i < amountToBreak; i++) {
                BlockPosition pos = validChests.get(i);

                boolean success = world.breakBlock(pos.x, pos.y, pos.z, 0);
                if (success) {
                    log("Successfully broke the block at " + pos.x + ", " + pos.y + ", " + pos.z);
                }
            }

            log(String.format("Chest RNG: Setup complete. Removed %d '%s'(s) in room '%s'.",
                    amountToBreak, RELICS_CHEST, room.getId()));
        });
    }

    public void randomizeRoomChests(RoomCodec room) {
        if (room == null || room.chestPositions == null || room.chestPositions.length == 0) {
            return;
        }

        world.execute(() -> {
            List<BlockPosition> validChests = new ArrayList<>();

            for (BlockPosition pos : room.chestPositions) {
                BlockType blockType = world.getBlockType(pos.x, pos.y, pos.z);

                if (blockType != null && blockType.getId().equals(RELICS_CHEST)) {
                    validChests.add(pos);
                } else {
                    String actualId = (blockType != null) ? blockType.getId().toString() : "Empty/Air";
                    log(String.format("Position %d, %d, %d is NOT a '%s'! (Found: %s)",
                            pos.x, pos.y, pos.z, RELICS_CHEST, actualId));
                }
            }

            log(String.format("Chest RNG: Found a total of %d '%s'(s) in room '%s'.",
                    validChests.size(), RELICS_CHEST, room.getId()));

            if (!validChests.isEmpty()) {
                Collections.shuffle(validChests);
                int amountToBreak = validChests.size() / 2;

                for (int i = 0; i < amountToBreak; i++) {
                    BlockPosition pos = validChests.get(i);
                    breakBlockAt(pos);
                }

                log(String.format("Chest RNG: Queued removal of %d '%s'(s).", amountToBreak, RELICS_CHEST));
            }
        });
    }

    public void restoreRoomChests(RoomCodec room) {
        if (room == null || room.chestPositions == null || room.chestPositions.length == 0) {
            return;
        }

        world.execute(() -> {

            int restoredCount = 0;
            for (BlockPosition pos : room.chestPositions) {
                BlockType currentBlock = world.getBlockType(pos.x, pos.y, pos.z);

                if (currentBlock == null || !currentBlock.getId().equals(RELICS_CHEST)) {
                    world.setBlock(pos.x, pos.y, pos.z, RELICS_CHEST);
                    restoredCount++;
                }
            }

            if (restoredCount > 0) {
                log("Restored " + restoredCount + " missing " + RELICS_CHEST + "(s) in room " + room.getId());
            }
        });
    }
}
