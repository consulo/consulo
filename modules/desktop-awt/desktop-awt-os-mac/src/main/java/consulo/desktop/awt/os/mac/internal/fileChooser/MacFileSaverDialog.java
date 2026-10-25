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
package consulo.desktop.awt.os.mac.internal.fileChooser;

import consulo.desktop.awt.ui.OwnerOptional;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileSaverDescriptor;
import consulo.fileChooser.util.FileChooserUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.localize.UILocalize;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author Denis Fokin
 */
public class MacFileSaverDialog {
  private FileDialog myFileDialog;
  private FileSaverDescriptor myDescriptor;

  private static LocalizeValue getChooserTitle(FileChooserDescriptor descriptor) {
    return descriptor.getTitle().orIfEmpty(UILocalize.fileChooserDefaultTitle());
  }

  public MacFileSaverDialog(FileSaverDescriptor descriptor, Project project) {
    this(descriptor, ProjectIdeFocusManager.getInstance(project).getFocusOwner());
  }

  public MacFileSaverDialog(FileSaverDescriptor descriptor, Component parent) {
    LocalizeValue title = getChooserTitle(descriptor);
    Consumer<Dialog> dialogConsumer = owner -> myFileDialog = new FileDialog(owner, title.get(), FileDialog.SAVE);
    Consumer<Frame> frameConsumer = owner -> myFileDialog = new FileDialog(owner, title.get(), FileDialog.SAVE);

    myDescriptor = descriptor;

    OwnerOptional.fromComponent(parent).ifDialog(dialogConsumer).ifFrame(frameConsumer).ifNull(frameConsumer);
  }

  @RequiredUIAccess
  public CompletableFuture<Path> saveAsync(@Nullable Path baseDir, @Nullable String filename) {
    myFileDialog.setDirectory(baseDir == null ? null : baseDir.toString());
    myFileDialog.setFile(filename);
    myFileDialog.setFilenameFilter((dir, name) -> FileChooserUtil.isPathAccepted(myDescriptor, dir.toPath().resolve(name)));

    CompletableFuture<Path> result = new CompletableFuture<>();
    SwingUtilities.invokeLater(() -> {
      myFileDialog.setVisible(true);

      String file = myFileDialog.getFile();
      if (file == null) {
        result.completeExceptionally(new CancellationException());
        return;
      }

      result.complete(Path.of(myFileDialog.getDirectory(), file));
    });
    return result;
  }
}
