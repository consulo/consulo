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
package consulo.desktop.awt.uiOld.mac;

import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.ui.ex.UIBundle;
import java.util.function.Consumer;
import consulo.ide.impl.idea.util.ui.OwnerOptional;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileSaverDescriptor;
import consulo.fileChooser.FileSaverDialog;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWrapper;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;

/**
 * @author Denis Fokin
 */
public class MacFileSaverDialog implements FileSaverDialog {
  private FileDialog myFileDialog;
  private FileSaverDescriptor myDescriptor;

  private static String getChooserTitle(final FileChooserDescriptor descriptor) {
    final String title = descriptor.getTitle();
    return title != null ? title : UIBundle.message("file.chooser.default.title");
  }

  public MacFileSaverDialog(FileSaverDescriptor descriptor, Project project) {
    this(descriptor, ProjectIdeFocusManager.getInstance(project).getFocusOwner());
  }

  public MacFileSaverDialog(FileSaverDescriptor descriptor, Component parent) {

    String title = getChooserTitle(descriptor);
    Consumer<Dialog> dialogConsumer = owner -> myFileDialog = new FileDialog(owner, title, FileDialog.SAVE);
    Consumer<Frame> frameConsumer = owner -> myFileDialog = new FileDialog(owner, title, FileDialog.SAVE);

    myDescriptor = descriptor;

    OwnerOptional.fromComponent(parent).ifDialog(dialogConsumer).ifFrame(frameConsumer).ifNull(frameConsumer);
  }

  @Nullable
  @Override
  public VirtualFileWrapper save(@Nullable VirtualFile baseDir, @Nullable String filename) {
    myFileDialog.setDirectory(baseDir == null ? null : baseDir.getCanonicalPath());
    myFileDialog.setFile(filename);
    myFileDialog.setFilenameFilter((dir, name) -> {
      File file = new File(dir, name);
      return myDescriptor.isFileSelectable(fileToVirtualFile(file));
    });

    myFileDialog.setVisible(true);

    String file = myFileDialog.getFile();
    if (file == null) {
      return null;
    }

    return new VirtualFileWrapper(new File(myFileDialog.getDirectory() + File.separator + file));
  }

  private static VirtualFile fileToVirtualFile(File file) {
    final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
    final String vfsPath = FileUtil.toSystemIndependentName(file.getAbsolutePath());
    return localFileSystem.refreshAndFindFileByPath(vfsPath);
  }

}
