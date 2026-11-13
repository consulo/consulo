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
import consulo.logging.Logger;
import consulo.util.io.FileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

@Singleton
@ServiceImpl
public class AsyncFileServiceImpl implements AsyncFileService {
  private static final Logger LOG = Logger.getInstance(AsyncFileServiceImpl.class);

  public static final String ASYNC_DELETE_EXTENSION = ".__del__";

  private final Application myApplication;

  @Inject
  public AsyncFileServiceImpl(Application application) {
    myApplication = application;
  }

  @Override
  public Future<Void> asyncDelete(Path... paths) {
    List<Path> movedAside = new ArrayList<>();
    for (Path path : paths) {
      Path moved = moveAsideOrDelete(path);
      if (moved != null) {
        movedAside.add(moved);
      }
    }
    if (movedAside.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    return startDeletion(movedAside);
  }

  private Future<Void> startDeletion(List<Path> paths) {
    RunnableFuture<Void> deleteTask = new FutureTask<>(() -> {
      Thread currentThread = Thread.currentThread();
      int priority = currentThread.getPriority();
      currentThread.setPriority(Thread.MIN_PRIORITY);
      try {
        for (Path path : paths) {
          FileUtil.delete(path);
        }
      }
      finally {
        currentThread.setPriority(priority);
      }
    }, null);

    myApplication.executeOnPooledThread(deleteTask);

    return deleteTask;
  }

  /**
   * Frees the path at once - by an atomic move: into the temp directory when it is of the same disk, or next to the file (of any
   * machine; such a name is ignored by the IDE). A file which cannot be moved is deleted now.
   */
  private static @Nullable Path moveAsideOrDelete(Path path) {
    if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
      return null;
    }

    String name = path.getFileName() == null ? "root" : path.getFileName().toString();
    List<Path> trashDirectories = new ArrayList<>(2);
    if (path.getFileSystem() == FileSystems.getDefault()) {
      trashDirectories.add(Path.of(FileUtil.getTempDirectory()));
    }
    if (path.getParent() != null) {
      trashDirectories.add(path.getParent());
    }

    for (Path trashDirectory : trashDirectories) {
      Path moved = moveInto(path, name, trashDirectory);
      if (moved != null) {
        return moved;
      }
    }

    FileUtil.delete(path);
    return null;
  }

  private static @Nullable Path moveInto(Path path, String name, Path directory) {
    int randomSuffix = (int) (System.currentTimeMillis() % 1000);
    for (int i = randomSuffix; i < randomSuffix + 100; i++) {
      Path target = directory.resolve("___" + name + i + ASYNC_DELETE_EXTENSION);
      try {
        return Files.move(path, target, StandardCopyOption.ATOMIC_MOVE);
      }
      catch (FileAlreadyExistsException e) {
        // the next name
      }
      catch (AtomicMoveNotSupportedException e) {
        // another disk - not this directory
        return null;
      }
      catch (IOException | UnsupportedOperationException e) {
        LOG.debug(e);
        return null;
      }
    }
    return null;
  }
}
