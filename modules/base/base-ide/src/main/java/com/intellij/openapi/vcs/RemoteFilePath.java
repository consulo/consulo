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
package com.intellij.openapi.vcs;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import com.intellij.util.PathUtil;
import com.intellij.util.text.FilePathHashingStrategy;
import consulo.util.collection.HashingStrategy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;

public class RemoteFilePath implements FilePath {

  private static final HashingStrategy<String> CASE_SENSITIVE_STRATEGY = FilePathHashingStrategy.create(true);

  @Nonnull
  private final String myPath;
  private final boolean myIsDirectory;

  public RemoteFilePath(@Nonnull String path, boolean isDirectory) {
    myPath = path;
    myIsDirectory = isDirectory;
  }

  @javax.annotation.Nullable
  @Override
  public VirtualFile getVirtualFile() {
    return null;
  }

  @Nullable
  @Override
  public VirtualFile getVirtualFileParent() {
    return null;
  }

  @Nonnull
  @Override
  public File getIOFile() {
    return new File(myPath);
  }

  @Nonnull
  @Override
  public String getName() {
    return PathUtil.getFileName(myPath);
  }

  @Nonnull
  @Override
  public String getPresentableUrl() {
    return getPath();
  }

  @Nullable
  @Override
  public Document getDocument() {
    return null;
  }

  @Nonnull
  @Override
  public Charset getCharset() {
    return getCharset(null);
  }

  @Nonnull
  @Override
  public Charset getCharset(@Nullable Project project) {
    EncodingManager em = project == null ? EncodingManager.getInstance() : EncodingProjectManager.getInstance(project);
    return em.getDefaultCharset();
  }

  @Nonnull
  @Override
  public FileType getFileType() {
    return FileTypeManager.getInstance().getFileTypeByFileName(getName());
  }

  @Override
  public void refresh() {
  }

  @Override
  public void hardRefresh() {
  }

  @Nonnull
  @Override
  public String getPath() {
    return myPath;
  }

  @Override
  public boolean isDirectory() {
    return myIsDirectory;
  }

  @Override
  public boolean isUnder(@Nonnull FilePath parent, boolean strict) {
    return FileUtil.isAncestor(parent.getPath(), getPath(), strict);
  }

  @Nullable
  @Override
  public FilePath getParentPath() {
    String parent = PathUtil.getParentPath(myPath);
    return parent.isEmpty() ? null : new RemoteFilePath(parent, true);
  }

  @Override
  public boolean isNonLocal() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RemoteFilePath other = (RemoteFilePath)o;

    if (myIsDirectory != other.myIsDirectory) return false;
    if (!CASE_SENSITIVE_STRATEGY.equals(myPath, other.myPath)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = CASE_SENSITIVE_STRATEGY.hashCode(myPath);
    result = 31 * result + (myIsDirectory ? 1 : 0);
    return result;
  }
}
