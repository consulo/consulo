/*
 * Copyright 2013-2022 consulo.io
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
package consulo.application.impl.internal.util;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.AsyncFileService;
import consulo.application.util.SystemInfo;
import consulo.util.io.FileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 * @author VISTALL
 * @since 15/10/2022
 */
@Singleton
@ServiceImpl
public class AsyncFileServiceImpl implements AsyncFileService {
  public static final String ASYNC_DELETE_EXTENSION = ".__del__";

  private final Application myApplication;

  @Inject
  public AsyncFileServiceImpl(Application application) {
    myApplication = application;
  }

  @Override
  @Nonnull
  public Future<Void> asyncDelete(@Nonnull Collection<File> files) {
    List<File> tempFiles = new ArrayList<>();
    for (File file : files) {
      final File tempFile = renameToTempFileOrDelete(file);
      if (tempFile != null) {
        tempFiles.add(tempFile);
      }
    }
    if (!tempFiles.isEmpty()) {
      return startDeletionThread(tempFiles.toArray(new File[tempFiles.size()]));
    }
    return CompletableFuture.completedFuture(null);
  }

  private Future<Void> startDeletionThread(@Nonnull final File... tempFiles) {
    final RunnableFuture<Void> deleteFilesTask = new FutureTask<>(new Runnable() {
      @Override
      public void run() {
        final Thread currentThread = Thread.currentThread();
        final int priority = currentThread.getPriority();
        currentThread.setPriority(Thread.MIN_PRIORITY);
        try {
          for (File tempFile : tempFiles) {
            FileUtil.delete(tempFile);
          }
        }
        finally {
          currentThread.setPriority(priority);
        }
      }
    }, null);

    myApplication.executeOnPooledThread(deleteFilesTask);

    return deleteFilesTask;
  }

  @Nullable
  private static File renameToTempFileOrDelete(@Nonnull File file) {
    String tempDir = FileUtil.getTempDirectory();
    boolean isSameDrive = true;
    if (SystemInfo.isWindows) {
      String tempDirDrive = tempDir.substring(0, 2);
      String fileDrive = file.getAbsolutePath().substring(0, 2);
      isSameDrive = tempDirDrive.equalsIgnoreCase(fileDrive);
    }

    if (isSameDrive) {
      // the optimization is reasonable only if destination dir is located on the same drive
      final String originalFileName = file.getName();
      File tempFile = getTempFile(originalFileName, tempDir);
      if (file.renameTo(tempFile)) {
        return tempFile;
      }
    }

    FileUtil.delete(file);

    return null;
  }

  private static File getTempFile(@Nonnull String originalFileName, @Nonnull String parent) {
    int randomSuffix = (int)(System.currentTimeMillis() % 1000);
    for (int i = randomSuffix; ; i++) {
      String name = "___" + originalFileName + i + ASYNC_DELETE_EXTENSION;
      File tempFile = new File(parent, name);
      if (!tempFile.exists()) return tempFile;
    }
  }
}
