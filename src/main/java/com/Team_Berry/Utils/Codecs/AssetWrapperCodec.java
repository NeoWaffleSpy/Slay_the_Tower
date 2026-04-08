package com.Team_Berry.Utils.Codecs;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetMap;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.RawAsset;
import com.hypixel.hytale.assetstore.codec.AssetCodec;
import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec.Mode;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.codec.validation.ValidatableCodec;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bson.BsonValue;

public class AssetWrapperCodec<K, T extends JsonAssetWithMap<K, M>, M extends AssetMap<K, T>> implements Codec<K>, ValidatableCodec<K> {
    private static final boolean DISABLE_DIRECT_LOADING = true;
    private final Class<T> assetClass;
    private final AssetCodec<K, T> codec;
    @Nonnull
    private final Mode mode;
    private final Function<AssetExtraInfo<K>, K> keyGenerator;

    public AssetWrapperCodec(Class<T> assetClass, AssetCodec<K, T> codec) {
        this(assetClass, codec, Mode.GENERATE_ID);
    }

    public AssetWrapperCodec(Class<T> assetClass, AssetCodec<K, T> codec, @Nonnull Mode mode) {
        this(assetClass, codec, mode, (assetExtraInfo) -> AssetRegistry.getAssetStore(assetClass).transformKey(assetExtraInfo.generateKey()));
    }

    public AssetWrapperCodec(Class<T> assetClass, AssetCodec<K, T> codec, @Nonnull Mode mode, Function<AssetExtraInfo<K>, K> keyGenerator) {
        if (mode == Mode.NONE) {
            throw new UnsupportedOperationException("Contained asset mode can't be NONE!");
        } else {
            this.assetClass = assetClass;
            this.codec = codec;
            this.mode = mode;
            this.keyGenerator = keyGenerator;
        }
    }

    public Class<T> getAssetClass() {
        return this.assetClass;
    }

    @Nullable
    public K decode(@Nonnull BsonValue bsonValue, ExtraInfo extraInfo) {
        if (!(extraInfo instanceof AssetExtraInfo<?> typed)) {
            throw new UnsupportedOperationException("Unable to decode asset from codec used outside of an AssetStore");
        } else if (bsonValue.isString()) {
            return (K)this.codec.getKeyCodec().getChildCodec().decode(bsonValue, extraInfo);
        } else {
            @SuppressWarnings("unchecked")
            AssetExtraInfo<K> assetExtraInfo = (AssetExtraInfo<K>) typed;
            KeyedCodec<K> parentCodec = this.codec.getParentCodec();
            K parentId = (K)(parentCodec != null ? parentCodec.getOrNull(bsonValue.asDocument(), extraInfo) : null);
            AssetStore<K, T, M> assetStore = AssetRegistry.getAssetStore(this.assetClass);
            K id;
            switch (this.mode.ordinal()) {
                case 1:
                    id = (K)this.keyGenerator.apply(assetExtraInfo);
                    break;
                case 2:
                    id = (K)assetStore.transformKey(assetExtraInfo.getKey());
                    break;
                case 3:
                    id = (K)assetStore.transformKey(assetExtraInfo.getKey());
                    if (parentId == null) {
                        Object thisAssetParentId = assetExtraInfo.getData().getParentKey();
                        if (thisAssetParentId != null) {
                            parentId = (K)assetStore.transformKey(thisAssetParentId);
                        }
                    }

                    break;
                case 4:
                    id = (K)this.keyGenerator.apply(assetExtraInfo);
                    if (parentId == null && !assetExtraInfo.getKey().equals(id)) {
                        parentId = (K)assetExtraInfo.getKey();
                    }

                    break;
                default:
                    throw new UnsupportedOperationException("Contained asset mode can't be NONE!");
            }

            T parent = parentId != null ? assetStore.getAssetMap().getAsset(parentId) : null;


            char[] clone = bsonValue.asDocument().toJson().toCharArray();
            Path path = assetExtraInfo.getAssetPath();
            assetExtraInfo.getData().addContainedAsset(this.assetClass, new RawAsset<>(path, id, parentId, 0, clone, assetExtraInfo.getData(), this.mode));
            return id;
        }
    }

    public BsonValue encode(@Nonnull K key, ExtraInfo extraInfo) {
        if (key.toString().startsWith("*")) {
            T asset = (T)(AssetRegistry.getAssetStore(this.assetClass).getAssetMap().getAsset(key));
            if (asset != null) {
                return this.codec.encode(asset, extraInfo);
            }
        }

        return this.codec.getKeyCodec().getChildCodec().encode(key, extraInfo);
    }

    @Nullable
    public K decodeJson(@Nonnull RawJsonReader reader, ExtraInfo extraInfo) throws IOException {
        if (!(extraInfo instanceof AssetExtraInfo<?> typed)) {
            throw new UnsupportedOperationException("Unable to decode asset from codec used outside of an AssetStore");
        } else {
            @SuppressWarnings("unchecked")
            AssetExtraInfo<K> assetExtraInfo = (AssetExtraInfo<K>) typed;
            int lineStart = reader.getLine() - 1;
            if (reader.peekFor('"')) {
                return (K)this.codec.getKeyCodec().getChildCodec().decodeJson(reader, extraInfo);
            } else {
                reader.mark();
                K parentId = null;
                boolean needsSkip = false;
                KeyedCodec<K> parentCodec = this.codec.getParentCodec();
                if (parentCodec != null && RawJsonReader.seekToKey(reader, parentCodec.getKey())) {
                    parentId = (K)parentCodec.getChildCodec().decodeJson(reader, extraInfo);
                    needsSkip = true;
                }

                AssetStore<K, T, M> assetStore = AssetRegistry.getAssetStore(this.assetClass);
                K id;
                switch (this.mode.ordinal()) {
                    case 1:
                        id = (K)this.keyGenerator.apply(assetExtraInfo);
                        break;
                    case 2:
                        id = (K)assetStore.transformKey(assetExtraInfo.getKey());
                        break;
                    case 3:
                        id = (K)assetStore.transformKey(assetExtraInfo.getKey());
                        if (parentId == null) {
                            Object thisAssetParentId = assetExtraInfo.getData().getParentKey();
                            if (thisAssetParentId != null) {
                                parentId = (K)assetStore.transformKey(thisAssetParentId);
                            }
                        }

                        break;
                    case 4:
                        id = (K)this.keyGenerator.apply(assetExtraInfo);
                        if (parentId == null && !assetExtraInfo.getKey().equals(id)) {
                            parentId = (K)assetExtraInfo.getKey();
                        }

                        break;
                    default:
                        throw new UnsupportedOperationException("Contained asset mode can't be NONE!");
                }

                T parent = parentId != null ? assetStore.getAssetMap().getAsset(parentId) : null;

                if (needsSkip) {
                    reader.skipObjectContinued();
                }

                char[] clone = reader.cloneMark();
                reader.unmark();
                Path path = assetExtraInfo.getAssetPath();
                assetExtraInfo.getData().addContainedAsset(this.assetClass, new RawAsset<>(path, id, parentId, lineStart, clone, assetExtraInfo.getData(), this.mode));
                return id;
            }
        }
    }

    @Nonnull
    public Schema toSchema(@Nonnull SchemaContext context) {
        Schema keySchema = context.refDefinition(this.codec.getKeyCodec().getChildCodec());
        keySchema.setTitle(this.assetClass.getSimpleName());
        return keySchema;
    }

    public void validate(K k, @Nonnull ExtraInfo extraInfo) {
        AssetRegistry.getAssetStore(this.assetClass).validate(k, extraInfo.getValidationResults(), extraInfo);
    }

    public void validateDefaults(ExtraInfo extraInfo, @Nonnull Set<Codec<?>> tested) {
        tested.add(this);
    }
}
