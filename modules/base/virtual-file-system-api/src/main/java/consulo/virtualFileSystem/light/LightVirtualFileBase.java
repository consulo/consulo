/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.virtualFileSystem.light;

import consulo.annotation.access.RequiredWriteAction;
import consulo.util.lang.LocalTimeCounter;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.internal.FakeVirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * In-memory implementation of {@link VirtualFile}.
 */
public abstract class LightVirtualFileBase extends BaseVirtualFile {
    private FileType myFileType;
    private String myName = "";
    private long myModStamp = LocalTimeCounter.currentTime();
    private boolean myIsWritable = true;
    private boolean myValid = true;
    private @Nullable VirtualFile myOriginalFile = null;

    public LightVirtualFileBase(String name, FileType fileType, long modificationStamp) {
        myName = name;
        myFileType = fileType;
        myModStamp = modificationStamp;
    }

    public void setFileType(FileType fileType) {
        myFileType = fileType;
    }

    @Override
    public FileType getFileType() {
        return myFileType;
    }

    public @Nullable VirtualFile getOriginalFile() {
        return myOriginalFile;
    }

    public void setOriginalFile(VirtualFile originalFile) {
        myOriginalFile = originalFile;
    }

    private static class MyVirtualFileSystem extends BaseVirtualFileSystem implements NonPhysicalFileSystem {
        private static final String PROTOCOL = "mock";

        private MyVirtualFileSystem() {
            startEventPropagation();
        }

        @Override
        @RequiredWriteAction
        public void deleteFile(@Nullable Object requestor, VirtualFile vFile) throws IOException {
        }

        @Override
        @RequiredWriteAction
        public void moveFile(@Nullable Object requestor, VirtualFile vFile, VirtualFile newParent) throws IOException {
        }

        @Override
        @RequiredWriteAction
        public void renameFile(@Nullable Object requestor, VirtualFile vFile, String newName) throws IOException {
        }

        @Override
        @RequiredWriteAction
        public VirtualFile createChildFile(@Nullable Object requestor, VirtualFile vDir, String fileName) throws IOException {
            return new FakeVirtualFile(vDir, fileName);
        }

        @Override
        @RequiredWriteAction
        public VirtualFile createChildDirectory(@Nullable Object requestor, VirtualFile vDir, String dirName) throws IOException {
            return new FakeVirtualFile(vDir, dirName);
        }

        @Override
        @RequiredWriteAction
        public VirtualFile copyFile(@Nullable Object requestor, VirtualFile virtualFile, VirtualFile newParent, String copyName) throws IOException {
            return new FakeVirtualFile(newParent, copyName);
        }

        @Override
        public String getProtocol() {
            return PROTOCOL;
        }

        @Override
        public @Nullable VirtualFile findFileByPath(String path) {
            return null;
        }

        @Override
        public void refresh(boolean asynchronous) {
        }

        @Override
        public @Nullable VirtualFile refreshAndFindFileByPath(String path) {
            return null;
        }
    }

    private static final MyVirtualFileSystem ourFileSystem = new MyVirtualFileSystem();

    @Override
    public VirtualFileSystem getFileSystem() {
        return ourFileSystem;
    }

    public @Nullable FileType getAssignedFileType() {
        return myFileType;
    }

    @Override
    public String getPath() {
        return "/" + getName();
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public boolean isWritable() {
        return myIsWritable;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isValid() {
        return myValid;
    }

    public void setValid(boolean valid) {
        myValid = valid;
    }

    @Override
    public @Nullable VirtualFile getParent() {
        return null;
    }

    @Override
    public VirtualFile @Nullable [] getChildren() {
        return EMPTY_ARRAY;
    }

    @Override
    public long getModificationStamp() {
        return myModStamp;
    }

    protected void setModificationStamp(long stamp) {
        myModStamp = stamp;
    }

    @Override
    public long getTimeStamp() {
        return 0; // todo[max] : Add UnsupportedOperationException at better times.
    }

    @Override
    public long getLength() {
        try {
            return contentsToByteArray().length;
        }
        catch (IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            assert false;
            return 0;
        }
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {
    }

    @Override
    public void setWritable(boolean b) {
        myIsWritable = b;
    }

    @Override
    public void rename(@Nullable Object requestor, String newName) throws IOException {
        myName = newName;
    }
}
