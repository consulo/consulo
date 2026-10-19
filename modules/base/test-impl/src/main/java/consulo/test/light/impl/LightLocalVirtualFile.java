/*
 * Copyright 2013-2026 consulo.io
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
package consulo.test.light.impl;

import consulo.util.io.FileAttributes;
import consulo.util.io.FileUtil;
import consulo.util.io.NioFiles;
import consulo.virtualFileSystem.BaseVirtualFile;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public class LightLocalVirtualFile extends BaseVirtualFile {
    private final LightLocalFileSystem myFileSystem;
    private final Path myFile;

    public LightLocalVirtualFile(LightLocalFileSystem fileSystem, Path file) {
        myFileSystem = fileSystem;
        myFile = file;
    }

    @Override
    public VirtualFileSystem getFileSystem() {
        return myFileSystem;
    }

    @Override
    public String getName() {
        return NioFiles.getFileName(myFile);
    }

    @Override
    public String getPath() {
        return FileUtil.toSystemIndependentName(myFile.toString());
    }

    @Override
    public Path toNioPath() {
        return myFile;
    }

    @Override
    public boolean isWritable() {
        return myFileSystem.isWritable(this);
    }

    @Override
    public boolean isDirectory() {
        return myFileSystem.isDirectory(this);
    }

    @Override
    public boolean is(VFileProperty property) {
        FileAttributes attributes = myFileSystem.getAttributes(this);
        if (property == VFileProperty.HIDDEN) {
            return attributes != null && attributes.isHidden();
        }
        if (property == VFileProperty.SYMLINK) {
            return attributes != null && attributes.isSymLink();
        }
        if (property == VFileProperty.SPECIAL) {
            return attributes != null && attributes.isSpecial();
        }
        return super.is(property);
    }

    @Override
    public boolean isValid() {
        return myFileSystem.exists(this);
    }

    @Override
    public @Nullable VirtualFile getParent() {
        Path parent = myFile.getParent();
        return parent != null ? new LightLocalVirtualFile(myFileSystem, parent) : null;
    }

    @Override
    public VirtualFile[] getChildren() {
        String[] names = myFileSystem.list(this);
        if (names.length == 0) {
            return EMPTY_ARRAY;
        }

        VirtualFile[] children = new VirtualFile[names.length];
        for (int i = 0; i < names.length; i++) {
            children[i] = new LightLocalVirtualFile(myFileSystem, myFile.resolve(names[i]));
        }
        return children;
    }

    @Override
    public long getTimeStamp() {
        return myFileSystem.getTimeStamp(this);
    }

    @Override
    public long getLength() {
        return myFileSystem.getLength(this);
    }

    @Override
    public long getModificationStamp() {
        return 0;
    }

    @Override
    public byte[] contentsToByteArray() throws IOException {
        return myFileSystem.contentsToByteArray(this);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return VirtualFileUtil.inputStreamSkippingBOM(new BufferedInputStream(myFileSystem.getInputStream(this)), this);
    }

    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        return myFileSystem.getOutputStream(this, requestor, newModificationStamp, newTimeStamp);
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {
        if (postRunnable != null) {
            postRunnable.run();
        }
    }

    @Override
    public boolean isInLocalFileSystem() {
        return true;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LightLocalVirtualFile that = (LightLocalVirtualFile) o;

        return myFile.equals(that.myFile);
    }

    @Override
    public int hashCode() {
        return myFile.hashCode();
    }
}
