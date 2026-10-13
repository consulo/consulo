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
package consulo.virtualFileSystem.impl.internal;

import consulo.disposer.Disposable;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;

/**
 * @author cdr
 */
class IdentityVirtualFilePointer extends VirtualFilePointerImpl implements VirtualFilePointer, Disposable {
  private volatile VirtualFile myFile;
  private final String myUrl;
  private volatile int useCount;

  IdentityVirtualFilePointer(VirtualFile file, String url) {
    myFile = file;
    myUrl = url;
  }

  @Override
  
  public String getFileName() {
    return getUrl();
  }

  @Override
  public VirtualFile getFile() {
    VirtualFile file = myFile;
    if (file == null && myUrl.startsWith(StandardFileSystems.PLATFORM_PROTOCOL + URLUtil.SCHEME_SEPARATOR)) {
      file = VirtualFileManager.getInstance().findFileByUrl(myUrl);
      myFile = file;
    }
    return file != null && file.isValid() ? file : null;
  }

  @Override
  
  public String getUrl() {
    return myUrl;
  }

  @Override
  
  public String getPresentableUrl() {
    return getUrl();
  }

  @Override
  public boolean isValid() {
    return myFile == null || myFile.isValid();
  }

  @Override
  int incrementUsageCount(int delta) {
    return useCount += delta;
  }

  @Override
  public void dispose() {
    incrementUsageCount(-1);
  }
}