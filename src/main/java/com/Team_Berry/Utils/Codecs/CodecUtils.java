package com.Team_Berry.Utils.Codecs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.protocol.*;

public class CodecUtils {
    public static final BuilderCodec<Direction> DIR_CODEC = BuilderCodec.builder(Direction.class, () -> new Direction(0, 0, 0))
            .append(new KeyedCodec<>("Yaw", Codec.FLOAT, true), (h, v) -> h.yaw = (float) Math.toRadians(v), h -> (float) Math.toDegrees(h.yaw))
            .addValidator(Validators.min(-360f)).addValidator(Validators.max(360f)).add()
            .append(new KeyedCodec<>("Pitch", Codec.FLOAT, true), (h, v) -> h.pitch = (float) Math.toRadians(v), h -> (float) Math.toDegrees(h.pitch))
            .addValidator(Validators.min(-360f)).addValidator(Validators.max(360f)).add()
            .append(new KeyedCodec<>("Roll", Codec.FLOAT, true), (h, v) -> h.roll = (float) Math.toRadians(v), h -> (float) Math.toDegrees(h.roll))
            .addValidator(Validators.min(-360f)).addValidator(Validators.max(360f)).add()
            .build();

    public static final BuilderCodec<Position> POS_CODEC = BuilderCodec.builder(Position.class, () -> new Position(0, 0, 0))
            .append(new KeyedCodec<>("X", Codec.DOUBLE, true), (h, v) -> h.x = v, h -> h.x).add()
            .append(new KeyedCodec<>("Y", Codec.DOUBLE, true), (h, v) -> h.y = v, h -> h.y).add()
            .append(new KeyedCodec<>("Z", Codec.DOUBLE, true), (h, v) -> h.z = v, h -> h.z).add()
            .build();

    public static final BuilderCodec<Vector3f> VEC3_CODEC = BuilderCodec.builder(Vector3f.class, () -> new Vector3f(0, 0, 0))
            .append(new KeyedCodec<>("X", Codec.FLOAT, true), (h, v) -> h.x = v, h -> h.x).add()
            .append(new KeyedCodec<>("Y", Codec.FLOAT, true), (h, v) -> h.y = v, h -> h.y).add()
            .append(new KeyedCodec<>("Z", Codec.FLOAT, true), (h, v) -> h.z = v, h -> h.z).add()
            .build();

    public static final BuilderCodec<Vector3f> CLAMP_VEC3_CODEC = BuilderCodec.builder(Vector3f.class, () -> new Vector3f(0, 0, 0))
            .append(new KeyedCodec<>("X", Codec.FLOAT, true), (h, v) -> h.x = v, h -> h.x)
            .addValidator(Validators.min(0f)).addValidator(Validators.max(1f)).add()
            .append(new KeyedCodec<>("Y", Codec.FLOAT, true), (h, v) -> h.y = v, h -> h.y)
            .addValidator(Validators.min(0f)).addValidator(Validators.max(1f)).add()
            .append(new KeyedCodec<>("Z", Codec.FLOAT, true), (h, v) -> h.z = v, h -> h.z)
            .addValidator(Validators.min(0f)).addValidator(Validators.max(1f)).add()
            .build();

    public static final BuilderCodec<Vector2f> VEC2_CODEC = BuilderCodec.builder(Vector2f.class, () -> new Vector2f(0, 0))
            .append(new KeyedCodec<>("X", Codec.FLOAT, true), (h, v) -> h.x = v, h -> h.x).add()
            .append(new KeyedCodec<>("Y", Codec.FLOAT, true), (h, v) -> h.y = v, h -> h.y).add()
            .build();
}
