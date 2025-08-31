/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.virtualFileSystem.http;

import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

/**
 * @author nik
 */
public abstract class RemoteContentProvider {

  public abstract boolean canProvideContent(@Nonnull String url);

  public abstract void saveContent(String url, @Nonnull File targetFile, @Nonnull DownloadingCallback callback);

  public abstract boolean isUpToDate(@Nonnull String url, @Nonnull VirtualFile local);


  public interface DownloadingCallback {
    void finished(@Nullable FileType fileType);

    void errorOccurred(@Nonnull String errorMessage, boolean cancelled);

    void setProgressText(@Nonnull String text, boolean indeterminate);

    void setProgressFraction(double fraction);

    boolean isCancelled();
  }
}
