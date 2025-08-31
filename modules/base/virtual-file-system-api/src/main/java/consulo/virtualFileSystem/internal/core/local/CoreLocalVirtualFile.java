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
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class CoreLocalVirtualFile extends VirtualFile {
  private final VirtualFileSystem myFileSystem;
  private final File myIoFile;
  private VirtualFile[] myChildren;
  private final boolean isDirectory;

  public CoreLocalVirtualFile(@Nonnull VirtualFileSystem fileSystem, @Nonnull File ioFile) {
    myFileSystem = fileSystem;
    myIoFile = ioFile;
    isDirectory = ioFile.isDirectory();
  }

  @Nonnull
  @Override
  public String getName() {
    return myIoFile.getName();
  }

  @Nonnull
  @Override
  public VirtualFileSystem getFileSystem() {
    return myFileSystem;
  }

  @Nonnull
  @Override
  public String getPath() {
    return FileUtil.toSystemIndependentName(myIoFile.getAbsolutePath());
  }

  @Override
  public boolean isWritable() {
    return false; // Core VFS isn't writable.
  }

  @Override
  public boolean isDirectory() {
    return isDirectory;
  }

  @Override
  public boolean isValid() {
    return true; // Core VFS cannot change, doesn't refresh so once found, any file is writable
  }

  @Override
  public VirtualFile getParent() {
    File parentFile = myIoFile.getParentFile();
    return parentFile != null ? new CoreLocalVirtualFile(myFileSystem, parentFile) : null;
  }

  @Override
  public VirtualFile[] getChildren() {
    VirtualFile[] answer = myChildren;
    if (answer == null) {
      List<VirtualFile> result = new ArrayList<VirtualFile>();
      File[] files = myIoFile.listFiles();
      if (files == null) {
        answer = EMPTY_ARRAY;
      }
      else {
        for (File file : files) {
          result.add(new CoreLocalVirtualFile(myFileSystem, file));
        }
        answer = result.toArray(new VirtualFile[result.size()]);
      }
      myChildren = answer;
    }
    return answer;
  }

  @Override
  public boolean isInLocalFileSystem() {
    return true;
  }

  @Nonnull
  @Override
  public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
    return new FileOutputStream(myIoFile);
  }

  @Nonnull
  @Override
  public byte[] contentsToByteArray() throws IOException {
    return RawFileLoader.getInstance().loadFileBytes(myIoFile);
  }

  @Override
  public long getTimeStamp() {
    return myIoFile.lastModified();
  }

  @Override
  public long getLength() {
    return myIoFile.length();
  }

  @Override
  public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) {
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return VirtualFileUtil.inputStreamSkippingBOM(new BufferedInputStream(new FileInputStream(myIoFile)), this);
  }

  @Override
  public long getModificationStamp() {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CoreLocalVirtualFile that = (CoreLocalVirtualFile)o;

    return myIoFile.equals(that.myIoFile);
  }

  @Override
  public int hashCode() {
    return myIoFile.hashCode();
  }
}
