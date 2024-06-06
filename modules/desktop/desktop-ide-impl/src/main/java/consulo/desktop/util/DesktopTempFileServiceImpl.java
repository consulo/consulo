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
package consulo.desktop.util;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.TempFileService;
import consulo.container.boot.ContainerPathManager;
import consulo.platform.Platform;
import consulo.util.io.FileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author VISTALL
 * @since 09/11/2022
 */
@Singleton
@ServiceImpl
public class DesktopTempFileServiceImpl implements TempFileService {
  private Path myCanonicalTempPathCache;

  @Override
  @Nonnull
  public Path createTempDirectory(@Nonnull Path dir, @Nonnull String prefix, @Nullable String suffix, boolean deleteOnExit) throws IOException {
    File file = doCreateTempFile(dir.toFile(), prefix, suffix, true);
    if (deleteOnExit) {
      // default deleteOnExit does not remove dirs if they are not empty
      FilesToDeleteHolder.ourFilesToDelete.add(file.getPath());
    }
    if (!file.isDirectory()) {
      throw new IOException("Cannot create a directory: " + file);
    }
    return file.toPath();
  }

  private static class FilesToDeleteHolder {
    private static final Queue<String> ourFilesToDelete = createFilesToDelete();

    private static Queue<String> createFilesToDelete() {
      final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
      Runtime.getRuntime().addShutdownHook(new Thread("FileUtil deleteOnExit") {
        @Override
        public void run() {
          String name;
          while ((name = queue.poll()) != null) {
            FileUtil.delete(new File(name));
          }
        }
      });
      return queue;
    }
  }

  @Override
  @Nonnull
  public Path createTempFile(Path dir, @Nonnull String prefix, @Nullable String suffix, boolean create, boolean deleteOnExit) throws IOException {
    File file = doCreateTempFile(dir.toFile(), prefix, suffix, false);
    if (deleteOnExit) {
      //noinspection SSBasedInspection
      file.deleteOnExit();
    }
    if (!create) {
      if (!file.delete() && file.exists()) {
        throw new IOException("Cannot delete a file: " + file);
      }
    }
    return file.toPath();
  }

  private static final Random RANDOM = new Random();

  @Nonnull
  private static File doCreateTempFile(@Nonnull File dir, @Nonnull String prefix, @Nullable String suffix, boolean isDirectory) throws IOException {
    //noinspection ResultOfMethodCallIgnored
    dir.mkdirs();

    if (prefix.length() < 3) {
      prefix = (prefix + "___").substring(0, 3);
    }
    if (suffix == null) {
      suffix = "";
    }
    // normalize and use only the file name from the prefix
    prefix = new File(prefix).getName();

    int attempts = 0;
    int i = 0;
    int maxFileNumber = 10;
    IOException exception = null;
    while (true) {
      File f = null;
      try {
        f = calcName(dir, prefix, suffix, i);

        boolean success = isDirectory ? f.mkdir() : f.createNewFile();
        if (success) {
          return normalizeFile(f);
        }
      }
      catch (IOException e) { // Win32 createFileExclusively access denied
        exception = e;
      }
      attempts++;
      int MAX_ATTEMPTS = 100;
      if (attempts > maxFileNumber / 2 || attempts > MAX_ATTEMPTS) {
        String[] children = dir.list();
        int size = children == null ? 0 : children.length;
        maxFileNumber = Math.max(10, size * 10); // if too many files are in tmp dir, we need a bigger random range than meager 10
        if (attempts > MAX_ATTEMPTS) {
          throw exception != null ? exception : new IOException("Unable to create a temporary file " + f + "\nDirectory '" + dir + "' list (" + size + " children): " + Arrays.toString(children));
        }
      }

      i++; // for some reason the file1 can't be created (previous file1 was deleted but got locked by anti-virus?). Try file2.
      if (i > 2) {
        i = 2 + RANDOM.nextInt(maxFileNumber); // generate random suffix if too many failures
      }
    }
  }

  @Nonnull
  private static File calcName(@Nonnull File dir, @Nonnull String prefix, @Nonnull String suffix, int i) throws IOException {
    prefix = i == 0 ? prefix : prefix + i;
    if (prefix.endsWith(".") && suffix.startsWith(".")) {
      prefix = prefix.substring(0, prefix.length() - 1);
    }
    String name = prefix + suffix;
    File f = new File(dir, name);
    if (!name.equals(f.getName())) {
      throw new IOException("A generated name is malformed: '" + name + "' (" + f + ")");
    }
    return f;
  }

  @Nonnull
  private static File normalizeFile(@Nonnull File temp) throws IOException {
    final File canonical = temp.getCanonicalFile();
    return Platform.current().os().isWindows() && canonical.getAbsolutePath().contains(" ") ? temp.getAbsoluteFile() : canonical;
  }

  @Override
  @Nonnull
  public Path getTempDirectory() {
    if (myCanonicalTempPathCache == null) {
      myCanonicalTempPathCache = calcCanonicalTempPath();
    }
    return myCanonicalTempPathCache;
  }

  @Nonnull
  private static Path calcCanonicalTempPath() {
    final File file = new File(ContainerPathManager.get().getTempPath());
    try {
      final String canonical = file.getCanonicalPath();
      if (!Platform.current().os().isWindows() || !canonical.contains(" ")) {
        return Path.of(canonical);
      }
    }
    catch (IOException ignore) {
    }
    return file.toPath().toAbsolutePath();
  }
}
