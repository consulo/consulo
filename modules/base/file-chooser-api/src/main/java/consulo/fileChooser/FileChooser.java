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
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2019-02-11
 */
public final class FileChooser {
  private static final Logger LOG = Logger.getInstance(FileChooser.class);

  private FileChooser() {
  }

  @Nonnull
  @RequiredUIAccess
  public static AsyncResult<VirtualFile[]> chooseFiles(@Nonnull FileChooserDescriptor descriptor, @Nullable ComponentManager project, @Nullable VirtualFile toSelect) {
    return chooseFiles(descriptor, null, project, toSelect);
  }

  @Nonnull
  @RequiredUIAccess
  public static AsyncResult<VirtualFile[]> chooseFiles(@Nonnull FileChooserDescriptor descriptor, @Nullable Component parent, @Nullable ComponentManager project, @Nullable VirtualFile toSelect) {
    final FileChooserDialog chooser = FileChooserFactory.getInstance().createFileChooser(descriptor, project, parent);
    return chooser.chooseAsync(project, toSelect == null ? VirtualFile.EMPTY_ARRAY : new VirtualFile[]{toSelect});
  }

  @Nonnull
  @RequiredUIAccess
  public static AsyncResult<VirtualFile> chooseFile(@Nonnull final FileChooserDescriptor descriptor, @Nullable final ComponentManager project, @Nullable final VirtualFile toSelect) {
    return chooseFile(descriptor, null, project, toSelect);
  }

  @Nonnull
  @RequiredUIAccess
  public static AsyncResult<VirtualFile> chooseFile(@Nonnull final FileChooserDescriptor descriptor,
                                                    @Nullable final Component parent,
                                                    @Nullable final ComponentManager project,
                                                    @Nullable final VirtualFile toSelect) {
    LOG.assertTrue(!descriptor.isChooseMultiple());
    AsyncResult<VirtualFile> result = AsyncResult.undefined();
    AsyncResult<VirtualFile[]> filesAsync = chooseFiles(descriptor, parent, project, toSelect);
    filesAsync.doWhenDone((files) -> result.setDone(files[0]));
    filesAsync.doWhenRejected((Runnable)result::setRejected);
    return result;
  }

  /**
   * Shows file/folder open dialog, allows user to choose files/folders and then passes result to callback in EDT.
   * On MacOS Open Dialog will be shown with slide effect if Macish UI is turned on.
   *
   * @param descriptor file chooser descriptor
   * @param project    project
   * @param parent     parent component
   * @param toSelect   file to preselect
   */
  @Nonnull
  @RequiredUIAccess
  public static AsyncResult<VirtualFile[]> chooseFiles(@Nonnull FileChooserDescriptor descriptor, @Nullable ComponentManager project, @Nullable Component parent, @Nullable VirtualFile toSelect) {
    final FileChooserFactory factory = FileChooserFactory.getInstance();
    final PathChooserDialog pathChooser = factory.createPathChooser(descriptor, project, parent);
    return pathChooser.chooseAsync(toSelect);
  }

  /**
   * Shows file/folder open dialog, allows user to choose file/folder and then passes result to callback in EDT.
   *
   * @param descriptor file chooser descriptor
   * @param project    project
   * @param parent     parent component
   * @param toSelect   file to preselect
   */
  @Nonnull
  @RequiredUIAccess
  public static AsyncResult<VirtualFile> chooseFile(@Nonnull final FileChooserDescriptor descriptor, @Nullable ComponentManager project, @Nullable Component parent, @Nullable VirtualFile toSelect) {
    LOG.assertTrue(!descriptor.isChooseMultiple());
    AsyncResult<VirtualFile> result = AsyncResult.undefined();

    AsyncResult<VirtualFile[]> filesAsync = chooseFiles(descriptor, project, parent, toSelect);
    filesAsync.doWhenDone((f) -> result.setDone(f[0]));
    filesAsync.doWhenRejected((Runnable)result::setRejected);
    return result;
  }
}
