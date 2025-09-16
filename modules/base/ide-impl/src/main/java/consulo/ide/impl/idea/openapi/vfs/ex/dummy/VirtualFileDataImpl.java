/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vfs.ex.dummy;

import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.LocalTimeCounter;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class VirtualFileDataImpl extends VirtualFileImpl {
    private byte[] myContents = ArrayUtil.EMPTY_BYTE_ARRAY;
    private long myModificationStamp = LocalTimeCounter.currentTime();

    public VirtualFileDataImpl(DummyFileSystem fileSystem, VirtualFileDirectoryImpl parent, String name) {
        super(fileSystem, parent, name);
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public long getLength() {
        return myContents.length;
    }

    @Override
    public VirtualFile[] getChildren() {
        return null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return VfsUtilCore.byteStreamSkippingBOM(myContents, this);
    }

    @Override
    @Nonnull
    public OutputStream getOutputStream(final Object requestor, final long newModificationStamp, long newTimeStamp) throws IOException {
        return VfsUtilCore.outputStreamAddingBOM(
            new ByteArrayOutputStream() {
                @Override
                public void close() {
                    DummyFileSystem fs = (DummyFileSystem) getFileSystem();
                    fs.fireBeforeContentsChange(requestor, VirtualFileDataImpl.this);
                    long oldModStamp = myModificationStamp;
                    myContents = toByteArray();
                    myModificationStamp = newModificationStamp >= 0 ? newModificationStamp : LocalTimeCounter.currentTime();
                    fs.fireContentsChanged(requestor, VirtualFileDataImpl.this, oldModStamp);
                }
            },
            this
        );
    }

    @Override
    @Nonnull
    public byte[] contentsToByteArray() throws IOException {
        return myContents;
    }

    @Override
    public long getModificationStamp() {
        return myModificationStamp;
    }

    public void setModificationStamp(long modificationStamp, Object requestor) {
        myModificationStamp = modificationStamp;
    }
}
