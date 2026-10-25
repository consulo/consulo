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
package consulo.fileChooser.util;

import consulo.application.Application;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.ui.UIAccess;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.io.FileUtil;
import consulo.util.io.NioFiles;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.ThrowableConsumer;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFileType;

import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 20/01/2022
 */
public class FileChooserUtil {
  private static final Object NO_INPUT = new Object();

  public static boolean isFileHidden(@Nullable VirtualFile file) {
    return file != null && file.isValid() && file.isInLocalFileSystem() && (file.is(VFileProperty.HIDDEN) || Platform.current().os().isUnix() && file.getName().startsWith("."));
  }

  public static boolean isArchive(@Nullable VirtualFile file) {
    if (file == null) {
      return false;
    }
    VirtualFile parent = file.getParent();
    if (isArchiveFileSystem(file) && parent == null) {
      return true;
    }
    return !file.isDirectory() && file.getFileType() instanceof ArchiveFileType && !isArchiveFileSystem(parent);
  }

  private static boolean isArchiveFileSystem(@Nullable VirtualFile file) {
    return file != null && file.getFileSystem() instanceof ArchiveFileSystem;
  }

  /**
   * Validates the chosen paths: {@link FileChooserDescriptor#validateSelectedPaths(List)} right away, then the
   * {@link FileChooserDescriptor#getVirtualFilesValidator() virtual files validator}, when the descriptor has one, on
   * the virtual files resolved off the UI thread.
   *
   * @return the message of the failed validation, {@link LocalizeValue#empty() empty} when the paths are accepted -
   * completed on the UI thread
   */
  @RequiredUIAccess
  public static CompletableFuture<LocalizeValue> validateSelectedPaths(FileChooserDescriptor descriptor, List<Path> paths) {
    try {
      descriptor.validateSelectedPaths(paths);
    }
    catch (Exception e) {
      return CompletableFuture.completedFuture(messageOf(e));
    }

    ThrowableConsumer<List<VirtualFile>, Exception> validator = descriptor.getVirtualFilesValidator();
    if (validator == null) {
      return CompletableFuture.completedFuture(LocalizeValue.empty());
    }

    Application application = Application.get();
    CoroutineScope scope = CoroutineScope.of(application.coroutineContext());
    scope.putCopyableUserData(UIAccess.KEY, UIAccess.current());

    return Coroutine
      .first(CodeExecution.<Object, List<VirtualFile>>apply(ignored -> toVirtualFiles(descriptor, paths)))
      .then(UIAction.<List<VirtualFile>, LocalizeValue>apply(files -> {
        try {
          validator.consume(files);
          return LocalizeValue.empty();
        }
        catch (Exception e) {
          return messageOf(e);
        }
      }))
      .runAsync(scope, NO_INPUT)
      .toFuture();
  }

  /**
   * Resolves the chosen paths into virtual files - call it off the UI thread. Each file goes through
   * {@link FileChooserDescriptor#getFileToSelect(VirtualFile)}, so that an archive is answered as its archive root when
   * the descriptor chooses archive contents; an entry inside an archive is resolved by its url.
   */
  public static List<VirtualFile> toVirtualFiles(FileChooserDescriptor descriptor, List<Path> paths) {
    VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();

    List<VirtualFile> files = new ArrayList<>(paths.size());
    for (Path path : paths) {
      VirtualFile file = virtualFileManager.refreshAndFindFileByNioPath(path);
      if (file != null && file.isValid()) {
        VirtualFile toSelect = descriptor.getFileToSelect(file);
        if (toSelect != null) {
          files.add(toSelect);
        }
      }
    }
    return files;
  }

  /**
   * Whether a native chooser lists the path - from the path alone, without the virtual file system: directories, and
   * the files the descriptor chooses which match its extension filter.
   */
  public static boolean isPathAccepted(FileChooserDescriptor descriptor, Path path) {
    if (Files.isDirectory(path)) {
      return true;
    }
    if (!descriptor.isChooseFiles() && !descriptor.isChooseJars() && !descriptor.isChooseJarContents()) {
      return false;
    }

    Pair<LocalizeValue, List<String>> extensionFilter = descriptor.getExtensionFilter();
    if (extensionFilter != null) {
      String name = path.getFileName() == null ? "" : path.getFileName().toString();
      boolean matches = false;
      for (String extension : extensionFilter.getSecond()) {
        if (StringUtil.endsWithIgnoreCase(name, "." + extension)) {
          matches = true;
          break;
        }
      }
      if (!matches) {
        return false;
      }
    }
    return descriptor.isPathSelectable(path);
  }

  /**
   * The path typed in a text field, or its nearest existing parent - for preselecting it in a chooser, without the
   * virtual file system.
   */
  public static @Nullable Path findExistingPathOrParent(@Nullable String text) {
    if (StringUtil.isEmptyOrSpaces(text)) {
      return null;
    }

    Path path = NioFiles.toPath(FileUtil.toSystemDependentName(text.trim()));
    while (path != null && !Files.exists(path)) {
      path = path.getParent();
    }
    return path;
  }

  private static LocalizeValue messageOf(Exception e) {
    String message = e.getMessage();
    return LocalizeValue.of(message != null ? message : e.toString());
  }
}
