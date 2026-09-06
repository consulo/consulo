// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.perFileVersion;

import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.impl.internal.SpecializedFileAttributes;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

class IntFileAttributeImpl implements IntFileAttribute {
    private final AutoRefreshingOnVfsCloseRef<SpecializedFileAttributes.IntFileAttributeAccessor> myAttributeAccessor;

    IntFileAttributeImpl(FileAttribute attribute, @Nullable Path fastAttributesPathOrNullForRegularAttributes) {
        myAttributeAccessor = new AutoRefreshingOnVfsCloseRef<>(fsRecords -> {
            if (fastAttributesPathOrNullForRegularAttributes != null) {
                return SpecializedFileAttributes.specializeAsFastInt(fsRecords, attribute, fastAttributesPathOrNullForRegularAttributes);
            }
            else {
                return SpecializedFileAttributes.specializeAsInt(fsRecords, attribute);
            }
        });
    }

    @Override
    public int readInt(int fileId) throws IOException {
        return myAttributeAccessor.get().read(fileId, 0);
    }

    @Override
    public void writeInt(int fileId, int value) throws IOException {
        myAttributeAccessor.get().write(fileId, value);
    }

    @Override
    public void close() throws IOException {
        myAttributeAccessor.close();
    }

    @Override
    public void closeAndUnsafelyUnmap() throws IOException {
        myAttributeAccessor.closeAndUnsafelyUnmap();
    }
}
