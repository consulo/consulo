/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.virtualFileSystem.pointer;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class LightFilePointer implements VirtualFilePointer {

  @Nonnull
  private final String myUrl;
  @Nullable
  private volatile VirtualFile myFile;
  private volatile boolean myRefreshed = false;

  public LightFilePointer(@Nonnull String url) {
    myUrl = url;
  }

  public LightFilePointer(@Nonnull VirtualFile file) {
    myUrl = file.getUrl();
    myFile = file;
  }

  @Override
  @Nullable
  public VirtualFile getFile() {
    refreshFile();
    return myFile;
  }

  @Override
  @Nonnull
  public String getUrl() {
    return myUrl;
  }

  @Override
  @Nonnull
  public String getFileName() {
    VirtualFile file = myFile;
    if (file != null) {
      return file.getName();
    }
    int index = myUrl.lastIndexOf('/');
    return index >= 0 ? myUrl.substring(index + 1) : myUrl;
  }

  @Override
  @Nonnull
  public String getPresentableUrl() {
    VirtualFile file = getFile();
    if (file != null) return file.getPresentableUrl();
    return toPresentableUrl(myUrl);
  }

  @Nonnull
  private static String toPresentableUrl(@Nonnull String url) {
    String path = VirtualFileManager.extractPath(url);
    String protocol = VirtualFileManager.extractProtocol(url);
    VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(protocol);
    return ObjectUtil.notNull(fileSystem, StandardFileSystems.local()).extractPresentableUrl(path);
  }

  @Override
  public boolean isValid() {
    return getFile() != null;
  }

  private void refreshFile() {
    VirtualFile file = myFile;
    if (file != null && file.isValid()) return;
    VirtualFileManager vfManager = VirtualFileManager.getInstance();
    VirtualFile virtualFile = vfManager.findFileByUrl(myUrl);
    if (virtualFile == null && !myRefreshed) {
      myRefreshed = true;
      Application application = ApplicationManager.getApplication();
      if (application.isDispatchThread() || !application.isReadAccessAllowed()) {
        virtualFile = vfManager.refreshAndFindFileByUrl(myUrl);
      }
      else {
        application.executeOnPooledThread(() -> vfManager.refreshAndFindFileByUrl(myUrl));
      }
    }

    myFile = virtualFile != null && virtualFile.isValid() ? virtualFile : null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LightFilePointer)) return false;

    return myUrl.equals(((LightFilePointer)o).myUrl);

  }

  @Override
  public int hashCode() {
    return myUrl.hashCode();
  }
}
