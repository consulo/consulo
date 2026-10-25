/*
 * Copyright 2013-2019 consulo.io
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
package consulo.fileChooser;

import consulo.component.ComponentManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Shortcuts over {@link FileChooserFactory#newBuilder(FileChooserDescriptor)}.
 *
 * @author VISTALL
 * @since 2019-02-11
 */
public final class FileChooser {
  private FileChooser() {
  }

  private static FileChooserBuilder builder(FileChooserDescriptor descriptor) {
    return FileChooserFactory.getInstance().newBuilder(descriptor);
  }

  @RequiredUIAccess
  public static CompletableFuture<List<Path>> choosePaths(
    FileChooserDescriptor descriptor,
    @Nullable ComponentManager project,
    @Nullable Path toSelect
  ) {
    return builder(descriptor).project(project).toSelect(toSelect).showMultiple();
  }

  @RequiredUIAccess
  public static CompletableFuture<Path> choosePath(
    FileChooserDescriptor descriptor,
    @Nullable ComponentManager project,
    @Nullable Path toSelect
  ) {
    return builder(descriptor).project(project).toSelect(toSelect).showSingle();
  }

  @RequiredUIAccess
  public static CompletableFuture<VirtualFile[]> chooseFiles(
    FileChooserDescriptor descriptor,
    @Nullable ComponentManager project,
    @Nullable VirtualFile toSelect
  ) {
    return chooseFiles(descriptor, null, project, toSelect);
  }

  @RequiredUIAccess
  public static CompletableFuture<VirtualFile[]> chooseFiles(
    FileChooserDescriptor descriptor,
    @Nullable Component parent,
    @Nullable ComponentManager project,
    @Nullable VirtualFile toSelect
  ) {
    return builder(descriptor)
      .project(project)
      .parent(parent)
      .toSelect(toPath(toSelect))
      .showMultipleAsVirtualFiles()
      .thenApply(files -> files.toArray(VirtualFile.EMPTY_ARRAY));
  }

  @RequiredUIAccess
  public static CompletableFuture<VirtualFile> chooseFile(
    FileChooserDescriptor descriptor,
    @Nullable ComponentManager project,
    @Nullable VirtualFile toSelect
  ) {
    return chooseFile(descriptor, null, project, toSelect);
  }

  @RequiredUIAccess
  public static CompletableFuture<VirtualFile> chooseFile(
    FileChooserDescriptor descriptor,
    @Nullable Component parent,
    @Nullable ComponentManager project,
    @Nullable VirtualFile toSelect
  ) {
    return builder(descriptor).project(project).parent(parent).toSelect(toPath(toSelect)).showSingleAsVirtualFile();
  }

  @RequiredUIAccess
  public static CompletableFuture<VirtualFile[]> chooseFiles(
    FileChooserDescriptor descriptor,
    @Nullable ComponentManager project,
    @Nullable Component parent,
    @Nullable VirtualFile toSelect
  ) {
    return chooseFiles(descriptor, parent, project, toSelect);
  }

  @RequiredUIAccess
  public static CompletableFuture<VirtualFile> chooseFile(
    FileChooserDescriptor descriptor,
    @Nullable ComponentManager project,
    @Nullable Component parent,
    @Nullable VirtualFile toSelect
  ) {
    return chooseFile(descriptor, parent, project, toSelect);
  }

  private static @Nullable Path toPath(@Nullable VirtualFile file) {
    return file == null ? null : file.getFileSystem().getNioPath(file);
  }
}
