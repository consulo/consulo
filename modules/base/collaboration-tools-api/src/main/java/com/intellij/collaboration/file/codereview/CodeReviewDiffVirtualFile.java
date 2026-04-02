// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.file.codereview;

import consulo.virtualFileSystem.VirtualFilePathWrapper;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.io.OutputStream;

@ApiStatus.Experimental
public abstract class CodeReviewDiffVirtualFile extends DiffViewerVirtualFile implements VirtualFilePathWrapper {

    protected CodeReviewDiffVirtualFile(@Nonnull String sourceId) {
        super(sourceId);
        putUserData(FileEditorManagerKeys.FORBID_TAB_SPLIT, true);
    }

    @Override
    public abstract @Nonnull String getPresentableName();

    @Override
    public abstract @Nonnull String getPath();

    @Override
    public abstract @Nonnull String getPresentablePath();

    @Override
    public final boolean enforcePresentableName() {
        return true;
    }

    @Override
    public abstract boolean isValid();

    @Override
    public abstract @Nonnull ComplexPathVirtualFileSystem<?> getFileSystem();

    @Override
    public final @Nonnull FileType getFileType() {
        return DiffFileType.INSTANCE;
    }

    @Override
    public final long getLength() {
        return 0L;
    }

    @Override
    @Nonnull
    public final byte[] contentsToByteArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final @Nonnull InputStream getInputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final @Nonnull OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) {
        throw new UnsupportedOperationException();
    }
}
