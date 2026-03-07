// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.file;

import consulo.virtualFileSystem.VirtualFilePathWrapper;
import consulo.virtualFileSystem.VirtualFileWithoutContent;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.light.LightVirtualFileBase;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link com.intellij.openapi.vfs.VirtualFile} of {@link ComplexPathVirtualFileSystem} that is supposed to be used
 * as a stub for opening various non-editor editors.
 * <p>
 * {@code sessionId} is an identifier which is required to differentiate files between launches.
 * This is necessary to make the files appear in "Recent Files" correctly.
 *
 * @see com.intellij.vcs.editor.ComplexPathVirtualFileSystem.ComplexPath#getSessionId()
 */
public abstract class ComplexPathVirtualFileWithoutContent extends LightVirtualFileBase
    implements VirtualFileWithoutContent, VirtualFilePathWrapper {

    protected final @Nonnull String sessionId;

    protected ComplexPathVirtualFileWithoutContent(@Nonnull String sessionId) {
        super("", null, 0);
        this.sessionId = sessionId;
        putUserData(FileEditorManagerKeys.REOPEN_WINDOW, false);
        putUserData(FileEditorManagerKeys.FORBID_TAB_SPLIT, true);
        setWritable(false);
    }

    @Override
    public boolean enforcePresentableName() {
        return true;
    }

    @Override
    public abstract @Nonnull ComplexPathVirtualFileSystem<?> getFileSystem();

    @Override
    public @Nonnull FileType getFileType() {
        return FileTypes.UNKNOWN;
    }

    @Override
    public long getLength() {
        return 0L;
    }

    @Override
    @Nonnull
    public byte[] contentsToByteArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nonnull InputStream getInputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nonnull OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ComplexPathVirtualFileWithoutContent that)) {
            return false;
        }
        return sessionId.equals(that.sessionId);
    }

    @Override
    public int hashCode() {
        return sessionId.hashCode();
    }
}
