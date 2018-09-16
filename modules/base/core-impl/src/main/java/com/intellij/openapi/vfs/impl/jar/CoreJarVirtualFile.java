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
package com.intellij.openapi.vfs.impl.jar;

import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.io.BufferExposingByteArrayInputStream;
import com.intellij.openapi.util.io.FileAttributes;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.util.SmartList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author yole
 */
@Deprecated
public class CoreJarVirtualFile extends VirtualFile {
  private final CoreJarHandler myHandler;
  private final String myName;
  private final FileAttributes myEntry;
  private final VirtualFile myParent;
  private List<VirtualFile> myChildren = null;

  public CoreJarVirtualFile(@Nonnull CoreJarHandler handler, @Nonnull String name, @Nonnull FileAttributes entry, @Nullable CoreJarVirtualFile parent) {
    myHandler = handler;
    myName = name;
    myEntry = entry;
    myParent = parent;

    if (parent != null) {
      if (parent.myChildren == null) {
        parent.myChildren = new SmartList<VirtualFile>();
      }
      parent.myChildren.add(this);
    }
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Nonnull
  @Override
  public VirtualFileSystem getFileSystem() {
    return myHandler.getFileSystem();
  }

  @Override
  @Nonnull
  public String getPath() {
    if (myParent == null) {
      return FileUtil.toSystemIndependentName(myHandler.getFile().getPath()) + "!/";
    }

    String parentPath = myParent.getPath();
    StringBuilder answer = new StringBuilder(parentPath.length() + 1 + myName.length());
    answer.append(parentPath);
    if (answer.charAt(answer.length() - 1) != '/') {
      answer.append('/');
    }
    answer.append(myName);

    return answer.toString();
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public boolean isDirectory() {
    return myEntry.isDirectory();
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public VirtualFile getParent() {
    return myParent;
  }

  @Override
  public VirtualFile[] getChildren() {
    if (myChildren == null) {
      return VirtualFile.EMPTY_ARRAY;
    }
    return myChildren.toArray(new VirtualFile[myChildren.size()]);
  }

  @Nonnull
  @Override
  public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
    throw new UnsupportedOperationException("JarFileSystem is read-only");
  }

  @Nonnull
  @Override
  public byte[] contentsToByteArray() throws IOException {
    Couple<String> pair = ((CoreJarFileSystem)getFileSystem()).splitPath(getPath());
    return myHandler.contentsToByteArray(pair.second);
  }

  @Override
  public long getTimeStamp() {
    return myEntry.lastModified;
  }

  @Override
  public long getLength() {
    return myEntry.length;
  }

  @Override
  public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) { }

  @Override
  public InputStream getInputStream() throws IOException {
    return new BufferExposingByteArrayInputStream(contentsToByteArray());
  }

  @Override
  public long getModificationStamp() {
    return 0;
  }
}
