// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.internal.core.local;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

final class IncompleteDirectoryAttributes implements BasicFileAttributes {
    @Override
    public FileTime lastModifiedTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileTime lastAccessTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileTime creationTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRegularFile() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public boolean isSymbolicLink() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOther() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public Object fileKey() {
        throw new UnsupportedOperationException();
    }
}
