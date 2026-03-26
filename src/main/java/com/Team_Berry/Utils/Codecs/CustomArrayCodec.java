package com.Team_Berry.Utils.Codecs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.RawJsonCodec;
import com.hypixel.hytale.codec.WrappedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.exception.CodecException;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.ArraySchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.schema.metadata.Metadata;
import com.hypixel.hytale.codec.util.RawJsonReader;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bson.BsonArray;
import org.bson.BsonNull;
import org.bson.BsonValue;

public class CustomArrayCodec<T> implements Codec<ArrayList<T>>, RawJsonCodec<ArrayList<T>>, WrappedCodec<T> {
    private final Codec<T> codec;
    private final IntFunction<ArrayList<T>> arrayConstructor;
    @Nullable
    private final Supplier<T> defaultValue;
    private List<Metadata> metadata;
    private ArrayList<T> emptyArray;

    public CustomArrayCodec(Codec<T> codec, IntFunction<ArrayList<T>> arrayConstructor) {
        this(codec, arrayConstructor, (Supplier)null);
    }

    public CustomArrayCodec(Codec<T> codec, IntFunction<ArrayList<T>> arrayConstructor, @Nullable Supplier<T> defaultValue) {
        this.codec = codec;
        this.arrayConstructor = arrayConstructor;
        this.defaultValue = defaultValue;
    }

    public Codec<T> getChildCodec() {
        return this.codec;
    }

    public ArrayList<T> decode(@Nonnull BsonValue bsonValue, @Nonnull ExtraInfo extraInfo) {
        BsonArray bsonArray = bsonValue.asArray();
        ArrayList<T> array = this.arrayConstructor.apply(bsonArray.size());
        int i = 0;

        for(int size = bsonArray.size(); i < size; ++i) {
            BsonValue value = bsonArray.get(i);
            extraInfo.pushIntKey(i);

            try {
                array.add(i, this.decodeElement(value, extraInfo));
            } catch (Exception e) {
                throw new CodecException("Failed to decode", value, extraInfo, e);
            } finally {
                extraInfo.popKey();
            }
        }

        return array;
    }

    @Nonnull
    public BsonValue encode(@Nonnull ArrayList<T> array, ExtraInfo extraInfo) {
        BsonArray bsonArray = new BsonArray();

        for(T t : array) {
            if (t == null) {
                bsonArray.add(new BsonNull());
            } else {
                bsonArray.add(this.codec.encode(t, extraInfo));
            }
        }

        return bsonArray;
    }

    public ArrayList<T> decodeJson(@Nonnull RawJsonReader reader, @Nonnull ExtraInfo extraInfo) throws IOException {
        reader.expect('[');
        reader.consumeWhiteSpace();
        if (reader.tryConsume(']')) {
            if (this.emptyArray == null) {
                this.emptyArray = this.arrayConstructor.apply(0);
            }

            return this.emptyArray;
        } else {
            int i = 0;
            ArrayList<T> arr = this.arrayConstructor.apply(10);

            while(true) {
                extraInfo.pushIntKey(i, reader);

                try {
                    arr.add(i, this.decodeJsonElement(reader, extraInfo));
                    ++i;
                } catch (Exception e) {
                    throw new CodecException("Failed to decode", reader, extraInfo, e);
                } finally {
                    extraInfo.popKey();
                }

                reader.consumeWhiteSpace();
                if (reader.tryConsumeOrExpect(']', ',')) {
                    if (arr.size() == i) {
                        return arr;
                    }

                    return arr;
                }

                reader.consumeWhiteSpace();
            }
        }
    }

    @Nonnull
    public CustomArrayCodec<T> metadata(Metadata metadata) {
        if (this.metadata == null) {
            this.metadata = new ObjectArrayList();
        }

        this.metadata.add(metadata);
        return this;
    }

    @Nonnull
    public Schema toSchema(@Nonnull SchemaContext context) {
        ArraySchema arraySchema = new ArraySchema();
        Schema childSchema = context.refDefinition(this.codec);
        if (this.metadata != null) {
            for(int i = 0; i < this.metadata.size(); ++i) {
                Metadata meta = (Metadata)this.metadata.get(i);
                meta.modify(childSchema);
            }
        }

        arraySchema.setItem(childSchema);
        return arraySchema;
    }

    @Nullable
    public Supplier<T> getDefaultSupplier() {
        return this.defaultValue;
    }

    @Nullable
    protected T decodeElement(@Nonnull BsonValue value, ExtraInfo extraInfo) {
        if (!value.isNull()) {
            return (T)this.codec.decode(value, extraInfo);
        } else {
            return (T)(this.defaultValue == null ? null : this.defaultValue.get());
        }
    }

    @Nullable
    protected T decodeJsonElement(@Nonnull RawJsonReader reader, ExtraInfo extraInfo) throws IOException {
        if (!reader.tryConsume("null")) {
            return (T)this.codec.decodeJson(reader, extraInfo);
        } else {
            return (T)(this.defaultValue == null ? null : this.defaultValue.get());
        }
    }

    @Nonnull
    public static <T> CustomArrayCodec<T> ofBuilderCodec(@Nonnull BuilderCodec<T> codec, IntFunction<ArrayList<T>> arrayConstructor) {
        return new CustomArrayCodec<T>(codec, arrayConstructor, codec.getSupplier());
    }
}
