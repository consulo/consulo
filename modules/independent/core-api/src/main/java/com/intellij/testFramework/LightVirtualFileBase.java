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
package com.intellij.testFramework;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.util.LocalTimeCounter;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;

/**
 * In-memory implementation of {@link VirtualFile}.
 */
public abstract class LightVirtualFileBase extends VirtualFile {
  private FileType myFileType;
  private String myName = "";
  private long myModStamp = LocalTimeCounter.currentTime();
  private boolean myIsWritable = true;
  private boolean myValid = true;
  private VirtualFile myOriginalFile;

  public LightVirtualFileBase(final String name, final FileType fileType, final long modificationStamp) {
    myName = name;
    myFileType = fileType;
    myModStamp = modificationStamp;
  }

  public void setFileType(final FileType fileType) {
    myFileType = fileType;
  }

  public VirtualFile getOriginalFile() {
    return myOriginalFile;
  }

  public void setOriginalFile(VirtualFile originalFile) {
    myOriginalFile = originalFile;
  }

  private static class MyVirtualFileSystem extends DeprecatedVirtualFileSystem implements NonPhysicalFileSystem {
    @NonNls private static final String PROTOCOL = "mock";

    private MyVirtualFileSystem() {
      startEventPropagation();
    }

    @Override
    public void deleteFile(Object requestor, @Nonnull VirtualFile vFile) throws IOException {

    }

    @Override
    public void moveFile(Object requestor, @Nonnull VirtualFile vFile, @Nonnull VirtualFile newParent) throws IOException {

    }

    @Override
    public void renameFile(Object requestor, @Nonnull VirtualFile vFile, @Nonnull String newName) throws IOException {

    }

    @Override
    public VirtualFile createChildFile(Object requestor, @Nonnull VirtualFile vDir, @Nonnull String fileName) throws IOException {
      return null;
    }

    @Nonnull
    @Override
    public VirtualFile createChildDirectory(Object requestor, @Nonnull VirtualFile vDir, @Nonnull String dirName) throws IOException {
      return null;
    }

    @Override
    public VirtualFile copyFile(Object requestor, @Nonnull VirtualFile virtualFile, @Nonnull VirtualFile newParent, @Nonnull String copyName)
            throws IOException {
      return null;
    }

    @Override
    @Nonnull
    public String getProtocol() {
      return PROTOCOL;
    }

    @Override
    @Nullable
    public VirtualFile findFileByPath(@Nonnull String path) {
      return null;
    }

    @Override
    public void refresh(boolean asynchronous) {
    }

    @Override
    @Nullable
    public VirtualFile refreshAndFindFileByPath(@Nonnull String path) {
      return null;
    }
  }

  private static final MyVirtualFileSystem ourFileSystem = new MyVirtualFileSystem();

  @Override
  @Nonnull
  public VirtualFileSystem getFileSystem() {
    return ourFileSystem;
  }

  @Nullable
  public FileType getAssignedFileType() {
    return myFileType;
  }

  @Nonnull
  @Override
  public String getPath() {
    return "/" + getName();
  }

  @Override
  @Nonnull
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
  public VirtualFile getParent() {
    return null;
  }

  @Override
  public VirtualFile[] getChildren() {
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
  public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) {
  }

  @Override
  public void setWritable(boolean b) {
    myIsWritable = b;
  }

  @Override
  public void rename(Object requestor, @Nonnull String newName) throws IOException {
    myName = newName;
  }
}
