// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.events;

import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import org.jspecify.annotations.Nullable;

public final class FileIndexingRequest {
    private static final Logger LOG = Logger.getInstance(FileIndexingRequest.class);

    private final boolean myDeleteRequest;
    private final VirtualFile myFile;
    private final int myFileId;

    private FileIndexingRequest(boolean deleteRequest, VirtualFile file) {
        myDeleteRequest = deleteRequest;
        myFile = file;
        myFileId = FileBasedIndex.getFileId(file);
    }

    public boolean isDeleteRequest() {
        return myDeleteRequest;
    }

    public VirtualFile getFile() {
        return myFile;
    }

    public int getFileId() {
        return myFileId;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return (other instanceof FileIndexingRequest otherRequest)
            && myDeleteRequest == otherRequest.myDeleteRequest
            && myFileId == otherRequest.myFileId;
    }

    @Override
    public int hashCode() {
        return myFileId * 31 + (myDeleteRequest ? 1 : 0);
    }

    @Override
    public String toString() {
        return "FileIndexingRequest(fileId=#" + myFileId + ", " + myFile + ", " + (myDeleteRequest ? "delete" : "update") + ")";
    }

    public static FileIndexingRequest updateRequest(VirtualFile file) {
        if (!(file instanceof VirtualFileWithId)) {
            LOG.error("Not a VirtualFileWithId: " + file.getClass() + " [" + file + "]");
        }
        return new FileIndexingRequest(false, file);
    }

    public static FileIndexingRequest deleteRequest(VirtualFile file) {
        if (!(file instanceof VirtualFileWithId)) {
            LOG.error("Not a VirtualFileWithId: " + file.getClass() + " [" + file + "]");
        }
        return new FileIndexingRequest(true, file);
    }
}
