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
package consulo.versionControlSystem.impl.internal;

import consulo.document.Document;
import consulo.project.Project;
import consulo.util.collection.CharSequenceHashingStrategy;
import consulo.util.collection.HashingStrategy;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;

public class RemoteFilePath implements FilePath {

  private static final HashingStrategy<CharSequence> CASE_SENSITIVE_STRATEGY = CharSequenceHashingStrategy.of(true);

  
  private final String myPath;
  private final boolean myIsDirectory;

  public RemoteFilePath(String path, boolean isDirectory) {
    myPath = path;
    myIsDirectory = isDirectory;
  }

  @Override
  public @Nullable VirtualFile getVirtualFile() {
    return null;
  }

  @Override
  public @Nullable VirtualFile getVirtualFileParent() {
    return null;
  }

  
  @Override
  public File getIOFile() {
    return new File(myPath);
  }

  
  @Override
  public String getName() {
    return PathUtil.getFileName(myPath);
  }

  
  @Override
  public String getPresentableUrl() {
    return getPath();
  }

  @Override
  public @Nullable Document getDocument() {
    return null;
  }

  
  @Override
  public Charset getCharset() {
    return getCharset(null);
  }

  
  @Override
  public Charset getCharset(@Nullable Project project) {
    EncodingManager em = project == null ? EncodingManager.getInstance() : EncodingProjectManager.getInstance(project);
    return em.getDefaultCharset();
  }

  
  @Override
  public FileType getFileType() {
    return FileTypeRegistry.getInstance().getFileTypeByFileName(getName());
  }

  @Override
  public void refresh() {
  }

  @Override
  public void hardRefresh() {
  }

  
  @Override
  public String getPath() {
    return myPath;
  }

  @Override
  public boolean isDirectory() {
    return myIsDirectory;
  }

  @Override
  public boolean isUnder(FilePath parent, boolean strict) {
    return FileUtil.isAncestor(parent.getPath(), getPath(), strict);
  }

  @Override
  public @Nullable FilePath getParentPath() {
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
