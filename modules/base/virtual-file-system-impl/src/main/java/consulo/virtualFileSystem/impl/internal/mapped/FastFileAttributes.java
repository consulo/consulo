// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal.mapped;

import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.impl.internal.SpecializedFileAttributes;
import consulo.virtualFileSystem.internal.CleanableStorage;
import consulo.virtualFileSystem.internal.FSRecordsProxy;
import consulo.virtualFileSystem.internal.FSRecordsProxy.FileIdIndexedStorage;
import consulo.virtualFileSystem.internal.Unmappable;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.IntUnaryOperator;

import static consulo.virtualFileSystem.impl.internal.mapped.MappedFileStorageHelper.openHelperAndVerifyVersions;

/**
 * (Temporary) Different non-standard kinds of VirtualFile attributes.
 * Standard attributes are {@link FileAttribute} and {@link SpecializedFileAttributes},
 * and here are some more exotic kinds, which may or may not be useful.
 */
public final class FastFileAttributes {
    private FastFileAttributes() {
        throw new AssertionError("Not for instantiation");
    }

    public static Int4FileAttribute int4FileAttributes(FSRecordsProxy vfs,
                                                       String storageName,
                                                       int version) throws IOException {
        MappedFileStorageHelper helper = openHelperAndVerifyVersions(
            vfs,
            storageName,
            version,
            Int4FileAttribute.ROW_SIZE
        );

        Int4FileAttribute attribute = new Int4FileAttribute(helper);
        vfs.addCloseable(attribute);
        vfs.addFileIdIndexedStorage(attribute);
        return attribute;
    }

    public static TimestampedBooleanAttributeAccessor timestampedBoolean(FileAttribute attribute) throws IOException {
        return timestampedBoolean(FSRecordsProxy.getInstance(), attribute);
    }

    public static TimestampedBooleanAttributeAccessor timestampedBoolean(FSRecordsProxy vfs,
                                                                         FileAttribute attribute) throws IOException {
        MappedFileStorageHelper helper = openHelperAndVerifyVersions(
            vfs,
            attribute.getId(),
            attribute.getVersion(),
            TimestampedBooleanAttributeAccessorImpl.ROW_SIZE
        );

        TimestampedBooleanAttributeAccessorImpl accessor = new TimestampedBooleanAttributeAccessorImpl(helper);
        vfs.addCloseable(accessor);
        vfs.addFileIdIndexedStorage(accessor);
        return accessor;
    }

    public static final class Int4FileAttribute implements FileIdIndexedStorage, Closeable, Unmappable, CleanableStorage {
        public static final int FIELDS = 4;
        public static final int ROW_SIZE = FIELDS * Integer.BYTES;

        private final MappedFileStorageHelper myStorageHelper;

        public Int4FileAttribute(MappedFileStorageHelper helper) {
            myStorageHelper = helper;
        }

        public int readField(VirtualFile vFile,
                             int fieldNo) throws IOException {
            return myStorageHelper.readIntField(vFile, fieldOffset(fieldNo));
        }

        public int readField(int fileId,
                             int fieldNo) throws IOException {
            return myStorageHelper.readIntField(fileId, fieldOffset(fieldNo));
        }

        public void write(VirtualFile vFile,
                          int fieldNo,
                          int value) throws IOException {
            myStorageHelper.writeIntField(vFile, fieldOffset(fieldNo), value);
        }

        public void write(int fileId,
                          int fieldNo,
                          int value) throws IOException {
            myStorageHelper.writeIntField(fileId, fieldOffset(fieldNo), value);
        }

        public void update(VirtualFile vFile,
                           int fieldNo,
                           IntUnaryOperator updater) throws IOException {
            myStorageHelper.updateIntField(vFile, fieldOffset(fieldNo), updater);
        }

        @Override
        public void clear(int fileId) throws IOException {
            myStorageHelper.writeIntField(fileId, 0, 0);
            myStorageHelper.writeIntField(fileId, 1, 0);
            myStorageHelper.writeIntField(fileId, 2, 0);
            myStorageHelper.writeIntField(fileId, 3, 0);
        }

        private static int fieldOffset(int fieldNo) {
            if (fieldNo < 0 || fieldNo >= FIELDS) {
                throw new IllegalArgumentException("fieldNo(=" + fieldNo + ") must be in [0," + FIELDS + ")");
            }
            return fieldNo * Integer.BYTES;
        }

        @Override
        public void close() throws IOException {
            myStorageHelper.close();
        }

        @Override
        public void closeAndUnsafelyUnmap() throws IOException {
            myStorageHelper.closeAndUnsafelyUnmap();
        }

        @Override
        public void closeAndClean() throws IOException {
            myStorageHelper.closeAndClean();
        }
    }

    private static final class TimestampedBooleanAttributeAccessorImpl
        implements TimestampedBooleanAttributeAccessor, FileIdIndexedStorage,
        Unmappable, CleanableStorage, Closeable {
        public static final int ROW_SIZE = Long.BYTES;

        private final MappedFileStorageHelper myStorageHelper;

        private TimestampedBooleanAttributeAccessorImpl(MappedFileStorageHelper helper) {
            myStorageHelper = helper;
        }

        @Override
        public @Nullable Boolean readIfActual(VirtualFile vFile) throws IOException {
            long stamp = vFile.getTimeStamp();
            long fieldValue = myStorageHelper.readLongField(vFile, 0);

            //use sign bit to store true(1) | false(0)
            long timestamp = Math.abs(fieldValue);
            boolean value = fieldValue < 0;

            if (timestamp == stamp) {
                return value;
            }
            else {
                return null;
            }
        }

        @Override
        public void write(VirtualFile vFile, boolean value) throws IOException {
            long stamp = vFile.getTimeStamp();
            //use sign bit to store true(1) | false(0)
            long fieldValue = value ? -stamp : stamp;
            myStorageHelper.writeLongField(vFile, 0, fieldValue);
        }

        @Override
        public void clear(int fileId) throws IOException {
            myStorageHelper.writeLongField(fileId, 0, 0);
        }

        @Override
        public void close() throws IOException {
            myStorageHelper.close();
        }

        @Override
        public void closeAndUnsafelyUnmap() throws IOException {
            myStorageHelper.closeAndUnsafelyUnmap();
        }

        @Override
        public void closeAndClean() throws IOException {
            myStorageHelper.closeAndClean();
        }
    }

    public interface TimestampedBooleanAttributeAccessor {
        @Nullable Boolean readIfActual(VirtualFile vFile) throws IOException;

        void write(VirtualFile vFile,
                   boolean value) throws IOException;
    }
}
