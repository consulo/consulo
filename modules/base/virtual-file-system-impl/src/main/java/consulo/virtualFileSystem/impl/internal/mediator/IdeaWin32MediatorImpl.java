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
package consulo.virtualFileSystem.impl.internal.mediator;

import consulo.util.io.FileAttributes;
import consulo.virtualFileSystem.impl.internal.windows.FileInfo;
import consulo.virtualFileSystem.impl.internal.windows.WindowsFileSystemHelper;
import consulo.virtualFileSystem.internal.FileSystemMediator;

import javax.annotation.Nonnull;

class IdeaWin32MediatorImpl implements FileSystemMediator {
  private WindowsFileSystemHelper myInstance = WindowsFileSystemHelper.getInstance();

  @Override
  public FileAttributes getAttributes(@Nonnull final String path) {
    final FileInfo fileInfo = myInstance.getInfo(path);
    return fileInfo != null ? fileInfo.toFileAttributes() : null;
  }

  @Override
  public String resolveSymLink(@Nonnull final String path) {
    return myInstance.resolveSymLink(path);
  }

  @Override
  public boolean clonePermissions(@Nonnull String source, @Nonnull String target, boolean execOnly) {
    return false;
  }
}

