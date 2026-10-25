// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff;

import consulo.diff.DiffFileType;
import consulo.diff.DiffViewerVirtualFile;
import consulo.versionControlSystem.editor.ComplexPathVirtualFileSystem;
import consulo.virtualFileSystem.VirtualFilePathWrapper;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class CodeReviewDiffVirtualFile extends DiffViewerVirtualFile implements VirtualFilePathWrapper {
    protected CodeReviewDiffVirtualFile(String sourceId) {
        super(sourceId);
    }

    @Override
    public abstract String getPresentableName();

    @Override
    public abstract String getPath();

    @Override
    public abstract String getPresentablePath();

    @Override
    public final boolean enforcePresentableName() {
        return true;
    }

    @Override
    public abstract boolean isValid();

    @Override
    public abstract ComplexPathVirtualFileSystem<?> getFileSystem();

    @Override
    public final FileType getFileType() {
        return DiffFileType.INSTANCE;
    }

    @Override
    public final long getLength() {
        return 0L;
    }

    @Override
    public final byte[] contentsToByteArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final InputStream getInputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final OutputStream getOutputStream(@Nullable Object requestor, long newModificationStamp, long newTimeStamp) {
        throw new UnsupportedOperationException();
    }
}
