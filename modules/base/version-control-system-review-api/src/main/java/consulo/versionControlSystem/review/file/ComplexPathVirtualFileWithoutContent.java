// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.file;

import consulo.versionControlSystem.editor.ComplexPathVirtualFileSystem;
import consulo.virtualFileSystem.VirtualFilePathWrapper;
import consulo.virtualFileSystem.VirtualFileWithoutContent;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.light.LightVirtualFileBase;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link consulo.virtualFileSystem.VirtualFile} of {@link ComplexPathVirtualFileSystem} that is supposed to be used a stub for opening various non-editor editors.
 * <p>
 * {@code sessionId} is an identifier which is required to differentiate files between launches.
 * This is necessary to make the files appear in "Recent Files" correctly.
 * See {@link consulo.versionControlSystem.editor.ComplexPath#getSessionId()} for details.
 */
public abstract class ComplexPathVirtualFileWithoutContent extends LightVirtualFileBase implements VirtualFileWithoutContent, VirtualFilePathWrapper {
    private final String mySessionId;

    protected ComplexPathVirtualFileWithoutContent(String sessionId) {
        super("", ComplexPathFileType.INSTANCE, 0);
        mySessionId = sessionId;
        setWritable(false);
    }

    public String getSessionId() {
        return mySessionId;
    }

    @Override
    public boolean enforcePresentableName() {
        return true;
    }

    @Override
    public abstract ComplexPathVirtualFileSystem<?> getFileSystem();

    @Override
    public FileType getFileType() {
        return ComplexPathFileType.INSTANCE;
    }

    @Override
    public long getLength() {
        return 0L;
    }

    @Override
    public byte[] contentsToByteArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream getOutputStream(@Nullable Object requestor, long newModificationStamp, long newTimeStamp) {
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
        return mySessionId.equals(that.mySessionId);
    }

    @Override
    public int hashCode() {
        return mySessionId.hashCode();
    }
}
