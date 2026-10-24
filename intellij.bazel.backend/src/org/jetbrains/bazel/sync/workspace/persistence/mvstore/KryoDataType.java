package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.h2.mvstore.type.DataType;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

// consulo: upstream Kotlin top-level declarations
final class KryoDataType {
    static final int DEFAULT_BUFFER_SIZE = 1024 * 1024;

    private static final byte DISCRIMINATOR_ERROR = 0;
    private static final byte DISCRIMINATOR_PRESENT = 1;
    private static final byte DISCRIMINATOR_OUTDATED = 2;

    private KryoDataType() {
    }

    static DataType<ValueFrame> createFrameDataType() {
        return new BasicDataType<>() {
            @Override
            public int getMemory(ValueFrame obj) {
                if (obj instanceof ValueFrame.Present present) {
                    return present.getPayload().length + 16;
                }
                else {
                    return 1;
                }
            }

            @Override
            public void write(WriteBuffer buff, ValueFrame obj) {
                if (obj instanceof ValueFrame.Present present) {
                    buff.put(DISCRIMINATOR_PRESENT);
                    buff.putVarInt(present.getVersion());
                    buff.putVarInt(present.getPayload().length);
                    buff.put(present.getPayload());
                }
                else if (obj instanceof ValueFrame.Outdated) {
                    buff.put(DISCRIMINATOR_OUTDATED);
                }
                else if (obj instanceof ValueFrame.Error) {
                    buff.put(DISCRIMINATOR_ERROR);
                }
            }

            @Override
            public ValueFrame read(ByteBuffer buff) {
                byte discriminator = buff.get();
                return switch (discriminator) {
                    case DISCRIMINATOR_PRESENT -> {
                        int version = DataUtils.readVarInt(buff);
                        int length = DataUtils.readVarInt(buff);
                        byte[] payload = new byte[length];
                        buff.get(payload);
                        yield new ValueFrame.Present(version, payload);
                    }
                    case DISCRIMINATOR_ERROR -> ValueFrame.Error.INSTANCE;
                    case DISCRIMINATOR_OUTDATED -> ValueFrame.Outdated.INSTANCE;
                    default -> throw new IllegalStateException("Invalid value discriminator: " + discriminator);
                };
            }

            @Override
            public ValueFrame[] createStorage(int size) {
                return new ValueFrame[size];
            }
        };
    }
}

/**
 * Frame of each value put inside MVStore map, it offers graceful fallback in
 * case of failure or schema version divergence.
 */
sealed interface ValueFrame {
    final class Present implements ValueFrame {
        private final int myVersion;
        private final byte[] myPayload;

        Present(int version, byte[] payload) {
            myVersion = version;
            myPayload = payload;
        }

        int getVersion() {
            return myVersion;
        }

        byte[] getPayload() {
            return myPayload;
        }
    }

    final class Outdated implements ValueFrame {
        static final Outdated INSTANCE = new Outdated();

        private Outdated() {
        }

        // consulo: data object equality
        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof Outdated;
        }

        @Override
        public int hashCode() {
            return Outdated.class.getName().hashCode();
        }

        @Override
        public String toString() {
            return "Outdated";
        }
    }

    final class Error implements ValueFrame {
        static final Error INSTANCE = new Error();

        private Error() {
        }

        // consulo: data object equality
        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof Error;
        }

        @Override
        public int hashCode() {
            return Error.class.getName().hashCode();
        }

        @Override
        public String toString() {
            return "Error";
        }
    }
}
