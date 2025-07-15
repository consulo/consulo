/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.annotation.DeprecationInfo;
import consulo.component.ComponentManager;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

@Deprecated
@DeprecationInfo("Use FileChooser")
public class IdeaFileChooser {
  private static final Logger LOG = Logger.getInstance(IdeaFileChooser.class);

  private IdeaFileChooser() {
  }

  /**
   * Normally, callback isn't invoked if a chooser was cancelled.
   * If the situation should be handled separately this interface may be used.
   */
  @Deprecated
  public interface FileChooserConsumer extends Consumer<List<VirtualFile>> {
    void cancelled();
  }

  @Nonnull
  @Deprecated
  public static VirtualFile[] chooseFiles(@Nonnull FileChooserDescriptor descriptor, @Nullable ComponentManager project, @Nullable VirtualFile toSelect) {
    return chooseFiles(descriptor, null, project, toSelect);
  }

  @Nonnull
  @Deprecated
  public static VirtualFile[] chooseFiles(@Nonnull FileChooserDescriptor descriptor, @Nullable Component parent, @Nullable ComponentManager project, @Nullable VirtualFile toSelect) {
    FileChooserDialog chooser = FileChooserFactory.getInstance().createFileChooser(descriptor, project, parent);
    return chooser.choose(project, toSelect == null ? VirtualFile.EMPTY_ARRAY : new VirtualFile[]{toSelect});
  }

  @Nullable
  @Deprecated
  public static VirtualFile chooseFile(@Nonnull FileChooserDescriptor descriptor, @Nullable ComponentManager project, @Nullable VirtualFile toSelect) {
    return chooseFile(descriptor, null, project, toSelect);
  }

  @Nullable
  @Deprecated
  public static VirtualFile chooseFile(@Nonnull FileChooserDescriptor descriptor, @Nullable Component parent, @Nullable ComponentManager project, @Nullable VirtualFile toSelect) {
    LOG.assertTrue(!descriptor.isChooseMultiple());
    return ArrayUtil.getFirstElement(chooseFiles(descriptor, parent, project, toSelect));
  }

  /**
   * Shows file/folder open dialog, allows user to choose files/folders and then passes result to callback in EDT.
   * On MacOS Open Dialog will be shown with slide effect if Macish UI is turned on.
   *
   * @param descriptor file chooser descriptor
   * @param project    project
   * @param toSelect   file to preselect
   * @param callback   callback will be invoked after user have closed dialog and only if there are files selected
   * @since 11.1
   */
  @Deprecated
  public static void chooseFiles(@Nonnull FileChooserDescriptor descriptor,
                                 @Nullable ComponentManager project,
                                 @Nullable VirtualFile toSelect,
                                 @RequiredUIAccess @Nonnull Consumer<List<VirtualFile>> callback) {
    chooseFiles(descriptor, project, null, toSelect, callback);
  }

  /**
   * Shows file/folder open dialog, allows user to choose files/folders and then passes result to callback in EDT.
   * On MacOS Open Dialog will be shown with slide effect if Macish UI is turned on.
   *
   * @param descriptor file chooser descriptor
   * @param project    project
   * @param parent     parent component
   * @param toSelect   file to preselect
   * @param callback   callback will be invoked after user have closed dialog and only if there are files selected
   * @see FileChooserConsumer
   * @since 11.1
   */
  @Deprecated
  public static void chooseFiles(@Nonnull FileChooserDescriptor descriptor,
                                 @Nullable ComponentManager project,
                                 @Nullable Component parent,
                                 @Nullable VirtualFile toSelect,
                                 @Nonnull Consumer<List<VirtualFile>> callback) {
    FileChooserFactory factory = FileChooserFactory.getInstance();
    PathChooserDialog pathChooser = factory.createPathChooser(descriptor, project, parent);
    pathChooser.choose(toSelect, callback);
  }

  /**
   * Shows file/folder open dialog, allows user to choose file/folder and then passes result to callback in EDT.
   * On MacOS Open Dialog will be shown with slide effect if Macish UI is turned on.
   *
   * @param descriptor file chooser descriptor
   * @param project    project
   * @param toSelect   file to preselect
   * @param callback   callback will be invoked after user have closed dialog and only if there is file selected
   * @since 13
   */
  @Deprecated
  public static void chooseFile(@Nonnull FileChooserDescriptor descriptor, @Nullable ComponentManager project, @Nullable VirtualFile toSelect, @Nonnull Consumer<VirtualFile> callback) {
    chooseFile(descriptor, project, null, toSelect, callback);
  }

  /**
   * Shows file/folder open dialog, allows user to choose file/folder and then passes result to callback in EDT.
   * On MacOS Open Dialog will be shown with slide effect if Macish UI is turned on.
   *
   * @param descriptor file chooser descriptor
   * @param project    project
   * @param parent     parent component
   * @param toSelect   file to preselect
   * @param callback   callback will be invoked after user have closed dialog and only if there is file selected
   * @since 13
   */
  @Deprecated
  public static void chooseFile(@Nonnull FileChooserDescriptor descriptor,
                                @Nullable ComponentManager project,
                                @Nullable Component parent,
                                @Nullable VirtualFile toSelect,
                                @Nonnull Consumer<VirtualFile> callback) {
    LOG.assertTrue(!descriptor.isChooseMultiple());
    chooseFiles(descriptor, project, parent, toSelect, files -> callback.accept(files.get(0)));
  }
}
