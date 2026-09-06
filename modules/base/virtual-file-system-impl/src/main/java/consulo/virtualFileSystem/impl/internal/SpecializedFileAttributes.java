// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal;

import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.impl.internal.mapped.MappedFileStorageHelper;
import consulo.virtualFileSystem.internal.CleanableStorage;
import consulo.virtualFileSystem.internal.FSRecordsProxy;
import consulo.virtualFileSystem.internal.Unmappable;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * Experimental API for faster access of file attribute if the attribute value is simple
 * byte/short/int/long. Some optimization could be applied in such scenarios,
 */
public final class SpecializedFileAttributes {
    //TODO RC: using FileAttribute.id as file name of a storage is risky -- there is no guarantee that attribute id
    //         is a valid file name! Need to apply some character-escaping (risk different attributes names collide
    //         after escaping) or use enumerated attributeId for a file name instead of attribute.id (safe, but
    //         files in 'extended-attributes' become unrecognizable by human being)

    private SpecializedFileAttributes() {
    }

    public static ByteFileAttributeAccessor specializeAsByte(FileAttribute attribute) {
        return specializeAsByte(FSRecordsProxy.getInstance(), attribute);
    }

    public static ByteFileAttributeAccessor specializeAsByte(FSRecordsProxy vfs,
                                                             FileAttribute attribute) {
        return new ByteFileAttributeAccessor() {
            @Override
            public byte read(int fileId,
                             byte defaultValue) throws IOException {
                try (DataInputStream stream = vfs.readAttributeWithLock(fileId, attribute)) {
                    if (stream == null) {
                        return defaultValue;
                    }
                    return stream.readByte();
                }
            }

            @Override
            public void write(int fileId,
                              byte value) throws IOException {
                try (DataOutputStream stream = vfs.writeAttribute(fileId, attribute)) {
                    stream.write(value);
                }
            }
        };
    }

    public static ShortFileAttributeAccessor specializeAsShort(FileAttribute attribute) {
        return specializeAsShort(FSRecordsProxy.getInstance(), attribute);
    }

    public static ShortFileAttributeAccessor specializeAsShort(FSRecordsProxy vfs,
                                                               FileAttribute attribute) {
        return new ShortFileAttributeAccessor() {
            @Override
            public short read(int fileId, short defaultValue) throws IOException {
                //stream.writeShort() writes in BIG_ENDIAN (default byte order for JVM)
                try (DataInputStream stream = vfs.readAttributeWithLock(fileId, attribute)) {
                    if (stream == null) {
                        return defaultValue;
                    }
                    return stream.readShort();
                }
            }

            @Override
            public void write(int fileId,
                              short value) throws IOException {
                try (DataOutputStream stream = vfs.writeAttribute(fileId, attribute)) {
                    stream.writeShort(value);
                }
            }
        };
    }

    public static IntFileAttributeAccessor specializeAsInt(FileAttribute attribute) {
        return specializeAsInt(FSRecordsProxy.getInstance(), attribute);
    }

    public static IntFileAttributeAccessor specializeAsInt(FSRecordsProxy vfs,
                                                           FileAttribute attribute) {
        return new IntFileAttributeAccessor() {
            @Override
            public void close() {
                // noop
            }

            @Override
            public int read(int fileId, int defaultValue) throws IOException {
                //stream.writeInt() writes in BIG_ENDIAN (default byte order for JVM)
                try (DataInputStream stream = vfs.readAttributeWithLock(fileId, attribute)) {
                    if (stream == null) {
                        return defaultValue;
                    }
                    return stream.readInt();
                }
            }

            @Override
            public void write(int fileId,
                              int value) throws IOException {
                try (DataOutputStream stream = vfs.writeAttribute(fileId, attribute)) {
                    stream.writeInt(value);
                }
            }

            @Override
            public void update(int fileId, IntUnaryOperator updater) throws IOException {
                throw new UnsupportedOperationException("Method not implemented yet");
            }
        };
    }

    public static LongFileAttributeAccessor specializeAsLong(FileAttribute attribute) {
        return specializeAsLong(FSRecordsProxy.getInstance(), attribute);
    }

    public static LongFileAttributeAccessor specializeAsLong(FSRecordsProxy vfs,
                                                             FileAttribute attribute) {
        if (!attribute.isFixedSize()) {
            throw new IllegalArgumentException(attribute + " must be fixedSize");
        }
        return new LongFileAttributeAccessor() {
            @Override
            public long read(int fileId,
                             long defaultValue) throws IOException {
                //stream.writeLong() writes in BIG_ENDIAN (default byte order for JVM)
                try (DataInputStream stream = vfs.readAttributeWithLock(fileId, attribute)) {
                    if (stream == null) {
                        return defaultValue;
                    }
                    return stream.readLong();
                }
            }

            @Override
            public void write(int fileId,
                              long value) throws IOException {
                try (DataOutputStream stream = vfs.writeAttribute(fileId, attribute)) {
                    stream.writeLong(value);
                }
            }

            @Override
            public void update(int fileId, LongUnaryOperator updater) throws IOException {
                throw new UnsupportedOperationException("Method is not implemented");
            }

            @Override
            public void close() {
                // noop
            }
        };
    }

    public static LongFileAttributeAccessor specializeAsFastLong(FileAttribute attribute) throws IOException {
        return specializeAsFastLong(FSRecordsProxy.getInstance(), attribute);
    }

    public static LongFileAttributeAccessor specializeAsFastLong(FSRecordsProxy vfs,
                                                                 FileAttribute attribute) throws IOException {
        String attributeId = attribute.getId();

        MappedFileStorageHelper storageHelper = MappedFileStorageHelper.openHelperAndVerifyVersions(
            vfs,
            attributeId,
            attribute.getVersion(),
            Long.BYTES
        );

        return specializeAsFastLong(vfs, storageHelper);
    }

    private static FastLongFileAttributeAccessor specializeAsFastLong(FSRecordsProxy vfs, MappedFileStorageHelper storageHelper) {
        FastLongFileAttributeAccessor accessor = new FastLongFileAttributeAccessor(storageHelper);
        vfs.addCloseable(accessor);
        vfs.addFileIdIndexedStorage(accessor);
        return accessor;
    }

    public static LongFileAttributeAccessor specializeAsFastLong(FSRecordsProxy vfs,
                                                                 FileAttribute attribute,
                                                                 Path absolutePath) throws IOException {
        MappedFileStorageHelper storageHelper = MappedFileStorageHelper.openHelperAndVerifyVersions(
            vfs,
            absolutePath,
            attribute.getVersion(),
            Long.BYTES,
            true
        );

        return specializeAsFastLong(vfs, storageHelper);
    }

    public static IntFileAttributeAccessor specializeAsFastInt(FileAttribute attribute) throws IOException {
        return specializeAsFastInt(FSRecordsProxy.getInstance(), attribute);
    }

    public static IntFileAttributeAccessor specializeAsFastInt(FSRecordsProxy vfs,
                                                               FileAttribute attribute,
                                                               Path absolutePath) throws IOException {
        MappedFileStorageHelper storageHelper = MappedFileStorageHelper.openHelperAndVerifyVersions(
            vfs,
            absolutePath,
            attribute.getVersion(),
            Integer.BYTES,
            true
        );

        return specializeAsFastInt(vfs, storageHelper);
    }

    public static IntFileAttributeAccessor specializeAsFastInt(FSRecordsProxy vfs,
                                                               FileAttribute attribute) throws IOException {
        MappedFileStorageHelper storageHelper = MappedFileStorageHelper.openHelperAndVerifyVersions(
            vfs,
            attribute.getId(),
            attribute.getVersion(),
            Integer.BYTES
        );

        return specializeAsFastInt(vfs, storageHelper);
    }

    private static IntFileAttributeAccessor specializeAsFastInt(FSRecordsProxy vfs,
                                                                MappedFileStorageHelper storageHelper) throws IOException {
        FastIntFileAttributeAccessor accessor = new FastIntFileAttributeAccessor(storageHelper);
        vfs.addCloseable(accessor);
        vfs.addFileIdIndexedStorage(accessor);
        return accessor;
    }

    public static ShortFileAttributeAccessor specializeAsFastShort(FileAttribute attribute) throws IOException {
        return specializeAsFastShort(FSRecordsProxy.getInstance(), attribute);
    }

    public static ShortFileAttributeAccessor specializeAsFastShort(FSRecordsProxy vfs,
                                                                   FileAttribute attribute) throws IOException {
        String attributeId = attribute.getId();

        MappedFileStorageHelper storageHelper = MappedFileStorageHelper.openHelperAndVerifyVersions(
            vfs, attributeId, attribute.getVersion(), Short.BYTES
        );

        FastShortFileAttributeAccessor accessor = new FastShortFileAttributeAccessor(storageHelper);
        vfs.addCloseable(accessor);
        vfs.addFileIdIndexedStorage(accessor);
        return accessor;
    }

    public static ByteFileAttributeAccessor specializeAsFastByte(FileAttribute attribute) throws IOException {
        return specializeAsFastByte(FSRecordsProxy.getInstance(), attribute);
    }

    public static ByteFileAttributeAccessor specializeAsFastByte(FSRecordsProxy vfs,
                                                                 FileAttribute attribute) throws IOException {
        String attributeId = attribute.getId();

        //RC: true byte attribute is impossible to implement since VarHandle(byte[]) is not supported by JDK -- hence
        //    we actually use int16 (short) fields
        MappedFileStorageHelper storageHelper = MappedFileStorageHelper.openHelperAndVerifyVersions(
            vfs, attributeId, attribute.getVersion(), Short.BYTES
        );

        FastByteFileAttributeAccessor accessor = new FastByteFileAttributeAccessor(storageHelper);
        vfs.addCloseable(accessor);
        vfs.addFileIdIndexedStorage(accessor);
        return accessor;
    }

    public interface LongFileAttributeAccessor extends Closeable {
        default long read(VirtualFile vFile) throws IOException {
            return read(vFile, 0);
        }

        default long read(VirtualFile vFile,
                          long defaultValue) throws IOException {
            return read(extractFileId(vFile), defaultValue);
        }

        default void write(VirtualFile vFile,
                           long value) throws IOException {
            write(extractFileId(vFile), value);
        }

        default void update(VirtualFile vFile,
                            LongUnaryOperator updater) throws IOException {
            update(extractFileId(vFile), updater);
        }

        long read(int fileId,
                  long defaultValue) throws IOException;

        void write(int fileId,
                   long value) throws IOException;

        void update(int fileId,
                    LongUnaryOperator updater) throws IOException;
    }

    public interface IntFileAttributeAccessor extends Closeable {
        default int read(VirtualFile vFile) throws IOException {
            return read(vFile, 0);
        }

        default int read(VirtualFile vFile, int defaultValue) throws IOException {
            return read(extractFileId(vFile), defaultValue);
        }

        default void write(VirtualFile vFile, int value) throws IOException {
            write(extractFileId(vFile), value);
        }

        default void update(VirtualFile vFile,
                            IntUnaryOperator updater) throws IOException {
            update(extractFileId(vFile), updater);
        }

        int read(int fileId, int defaultValue) throws IOException;

        void write(int fileId, int value) throws IOException;

        void update(int fileId,
                    IntUnaryOperator updater) throws IOException;
    }

    public interface ShortFileAttributeAccessor {
        default short read(VirtualFile vFile) throws IOException {
            return read(vFile, (short) 0);
        }

        default short read(VirtualFile vFile, short defaultValue) throws IOException {
            return read(extractFileId(vFile), defaultValue);
        }

        default void write(VirtualFile vFile, short value) throws IOException {
            write(extractFileId(vFile), value);
        }

        short read(int fileId, short defaultValue) throws IOException;

        void write(int fileId, short value) throws IOException;
    }

    public interface ByteFileAttributeAccessor {
        default byte read(VirtualFile vFile) throws IOException {
            return read(vFile, (byte) 0);
        }

        default byte read(VirtualFile vFile,
                          byte defaultValue) throws IOException {
            return read(extractFileId(vFile), defaultValue);
        }

        default void write(VirtualFile vFile,
                           byte value) throws IOException {
            write(extractFileId(vFile), value);
        }

        byte read(int fileId,
                  byte defaultValue) throws IOException;

        void write(int fileId,
                   byte value) throws IOException;
    }

    private static int extractFileId(VirtualFile vFile) {
        if (!(vFile instanceof VirtualFileWithId)) {
            throw new IllegalArgumentException(vFile + " must be instance of VirtualFileWithId");
        }
        return ((VirtualFileWithId) vFile).getId();
    }

    /** Advanced-level control over fast attributes, for expert to use */
    public interface FileAttributeAccessorEx extends FSRecordsProxy.FileIdIndexedStorage {
        @Override
        void clear(int fileId) throws IOException;

        /**
         * Clear (set to default) all the records. I.e. conceptually it is the same as
         * {@code forEach(fileId): clear(fileId) }
         * BEWARE: this method is not atomic in a multithreaded environment, i.e. it shouldn't be called in a race with
         * concurrent updates -- results are undefined.
         */
        void clear() throws IOException;

        void flush() throws IOException;
    }

    private abstract static class FastAttributeAccessorHelper implements FileAttributeAccessorEx, Closeable, Unmappable, CleanableStorage {
        protected final MappedFileStorageHelper helper;

        private FastAttributeAccessorHelper(MappedFileStorageHelper helper) {
            this.helper = helper;
        }

        @Override
        public void clear() throws IOException {
            helper.clearRecords();
        }

        @Override
        public void flush() throws IOException {
            helper.fsync();
        }

        @Override
        public void close() throws IOException {
            helper.close();
        }

        @Override
        public void closeAndUnsafelyUnmap() throws IOException {
            helper.closeAndUnsafelyUnmap();
        }

        @Override
        public void closeAndClean() throws IOException {
            helper.closeAndClean();
        }
    }

    private static class FastLongFileAttributeAccessor extends FastAttributeAccessorHelper implements LongFileAttributeAccessor {
        private static final int FIELD_OFFSET = 0;

        private FastLongFileAttributeAccessor(MappedFileStorageHelper storageHelper) {
            super(storageHelper);
        }

        @Override
        public long read(int fileId,
                         long defaultValue) throws IOException {
            if (defaultValue != 0) {
                throw new UnsupportedOperationException(
                    "defaultValue=" + defaultValue + ": so far only 0 is supported default value for fast-attributes");
            }
            return helper.readLongField(fileId, FIELD_OFFSET);
        }

        @Override
        public void write(int fileId,
                          long value) throws IOException {
            helper.writeLongField(fileId, FIELD_OFFSET, value);
        }

        @Override
        public void update(int fileId,
                           LongUnaryOperator updater) throws IOException {
            helper.updateLongField(fileId, FIELD_OFFSET, updater);
        }

        @Override
        public void clear(int fileId) throws IOException {
            helper.writeLongField(fileId, FIELD_OFFSET, 0L);
        }
    }

    private static class FastIntFileAttributeAccessor extends FastAttributeAccessorHelper implements IntFileAttributeAccessor {
        private static final int FIELD_OFFSET = 0;

        private FastIntFileAttributeAccessor(MappedFileStorageHelper storageHelper) {
            super(storageHelper);
        }

        @Override
        public int read(int fileId,
                        int defaultValue) throws IOException {
            if (defaultValue != 0) {
                throw new UnsupportedOperationException(
                    "defaultValue=" + defaultValue + ": so far only 0 is supported default value for fast-attributes");
            }
            return helper.readIntField(fileId, FIELD_OFFSET);
        }

        @Override
        public void write(int fileId, int value) throws IOException {
            helper.writeIntField(fileId, FIELD_OFFSET, value);
        }

        @Override
        public void update(int fileId,
                           IntUnaryOperator updater) throws IOException {
            helper.updateIntField(fileId, FIELD_OFFSET, updater);
        }

        @Override
        public void clear(int fileId) throws IOException {
            helper.writeIntField(fileId, FIELD_OFFSET, 0);
        }
    }

    private static class FastShortFileAttributeAccessor extends FastAttributeAccessorHelper implements ShortFileAttributeAccessor {
        private static final int FIELD_OFFSET = 0;

        private FastShortFileAttributeAccessor(MappedFileStorageHelper helper) {
            super(helper);
        }

        @Override
        public short read(int fileId,
                          short defaultValue) throws IOException {
            if (defaultValue != 0) {
                throw new UnsupportedOperationException(
                    "defaultValue=" + defaultValue + ": so far only 0 is supported default value for fast-attributes");
            }
            return helper.readShortField(fileId, FIELD_OFFSET);
        }

        @Override
        public void write(int fileId,
                          short value) throws IOException {
            helper.writeShortField(fileId, FIELD_OFFSET, value);
        }

        @Override
        public void clear(int fileId) throws IOException {
            helper.writeShortField(fileId, FIELD_OFFSET, (short) 0);
        }
    }

    //RC: true byte attribute is impossible to implement since VarHandle(byte[]) is not supported by JDK -- hence
    //    we actually use int16 (short) fields
    private static class FastByteFileAttributeAccessor extends FastAttributeAccessorHelper implements ByteFileAttributeAccessor {
        private static final int FIELD_OFFSET = 0;

        private FastByteFileAttributeAccessor(MappedFileStorageHelper helper) {
            super(helper);
            if (helper.bytesPerRow() != Short.BYTES) {
                throw new AssertionError("Bug: helper must have 2 bytes per row, not " + helper.bytesPerRow());
            }
        }

        @Override
        public byte read(int fileId,
                         byte defaultValue) throws IOException {
            if (defaultValue != 0) {
                throw new UnsupportedOperationException(
                    "defaultValue=" + defaultValue + ": so far only 0 is supported default value for fast-attributes");
            }
            return (byte) helper.readShortField(fileId, FIELD_OFFSET);
        }

        @Override
        public void write(int fileId,
                          byte value) throws IOException {
            helper.writeShortField(fileId, FIELD_OFFSET, value);
        }

        @Override
        public void clear(int fileId) throws IOException {
            helper.writeShortField(fileId, FIELD_OFFSET, (short) 0);
        }
    }
}
