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
package consulo.virtualFileSystem.internal.core.local;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.util.List;

/**
 * @author yole
 */
public class CoreLocalVirtualFile extends BaseVirtualFile {
    private final CoreLocalFileSystem myFileSystem;
    private final Path myFile;
    private @Nullable BasicFileAttributes myAttributes;
    private VirtualFile @Nullable [] myChildren;

    public CoreLocalVirtualFile(CoreLocalFileSystem fileSystem, Path file) {
        myFileSystem = fileSystem;
        myFile = file;
    }

    public CoreLocalVirtualFile(CoreLocalFileSystem fileSystem, Path file, boolean isDirectory) {
        myFileSystem = fileSystem;
        myFile = file;
        myAttributes = isDirectory ? new IncompleteDirectoryAttributes() : null;
    }

    public CoreLocalVirtualFile(CoreLocalFileSystem fileSystem, Path file, BasicFileAttributes attributes) {
        myFileSystem = fileSystem;
        myFile = file;
        myAttributes = attributes;
    }

    protected Path getFile() {
        return myFile;
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
        return false;
    }

    @Override
    public boolean isDirectory() {
        BasicFileAttributes attrs = getAttributes(false);
        return attrs != null && attrs.isDirectory();
    }

    @Override
    public boolean is(VFileProperty property) {
        BasicFileAttributes attrs = getAttributes(true);
        if (property == VFileProperty.HIDDEN) {
            return attrs instanceof DosFileAttributes dosAttrs && dosAttrs.isHidden() && myFile.getParent() != null
                || NioFiles.getFileName(myFile).startsWith(".");
        }
        if (property == VFileProperty.SYMLINK) {
            return attrs != null && attrs.isSymbolicLink();
        }
        if (property == VFileProperty.SPECIAL) {
            return attrs != null && attrs.isOther();
        }
        return super.is(property);
    }

    @Override
    public long getTimeStamp() {
        BasicFileAttributes attrs = getAttributes(true);
        return attrs != null ? attrs.lastModifiedTime().toMillis() : -1;
    }

    @Override
    public long getLength() {
        BasicFileAttributes attrs = getAttributes(false);
        return attrs != null ? attrs.size() : -1;
    }

    protected @Nullable BasicFileAttributes getAttributes(boolean full) {
        if (myAttributes == null || full && myAttributes instanceof IncompleteDirectoryAttributes) {
            try {
                myAttributes = Files.readAttributes(myFile, BasicFileAttributes.class);
            }
            catch (IOException ignored) {
            }
        }
        return myAttributes;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public @Nullable VirtualFile getParent() {
        Path parentFile = myFile.getParent();
        return parentFile != null ? new CoreLocalVirtualFile(myFileSystem, parentFile, new IncompleteDirectoryAttributes()) : null;
    }

    @Override
    public VirtualFile @Nullable [] getChildren() {
        if (myChildren == null) {
            List<Path> files = NioFiles.list(myFile);
            if (files.isEmpty()) {
                myChildren = EMPTY_ARRAY;
            }
            else {
                VirtualFile[] result = new VirtualFile[files.size()];
                for (int i = 0; i < files.size(); i++) {
                    result[i] = new CoreLocalVirtualFile(myFileSystem, files.get(i));
                }
                myChildren = result;
            }
        }
        return myChildren;
    }

    @Override
    public OutputStream getOutputStream(@Nullable Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] contentsToByteArray() throws IOException {
        return Files.readAllBytes(myFile);
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return VirtualFileUtil.inputStreamSkippingBOM(new BufferedInputStream(Files.newInputStream(myFile)), this);
    }

    @Override
    public long getModificationStamp() {
        return 0;
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

        CoreLocalVirtualFile that = (CoreLocalVirtualFile) o;

        return myFile.equals(that.myFile);
    }

    @Override
    public int hashCode() {
        return myFile.hashCode();
    }
}
