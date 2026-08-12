/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.fileChooser.impl.system;

import consulo.virtualFileSystem.LocalFileSystem;
import consulo.component.ComponentManager;
import consulo.desktop.swt.ui.impl.TargetSWT;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDialog;
import consulo.fileChooser.PathChooserDialog;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.eclipse.swt.widgets.DirectoryDialog;

import org.jspecify.annotations.Nullable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 10/07/2021
 */
public class DesktopSwtFileChooserDialog implements FileChooserDialog, PathChooserDialog {
  private final FileChooserDescriptor myDescriptor;

  public DesktopSwtFileChooserDialog(FileChooserDescriptor descriptor) {
    myDescriptor = descriptor;
  }

  @RequiredUIAccess
  
  @Override
  public CompletableFuture<VirtualFile[]> chooseAsync(@Nullable VirtualFile toSelect) {
    return chooseAsync(null, new VirtualFile[]{toSelect});
  }

  @RequiredUIAccess

  @Override
  public CompletableFuture<VirtualFile[]> chooseAsync(@Nullable ComponentManager project, VirtualFile[] toSelect) {
    Window focusedWindow = Window.getActiveWindow();

    CompletableFuture<VirtualFile[]> result = new CompletableFuture<>();
    DirectoryDialog directoryDialog = new DirectoryDialog(TargetSWT.to(focusedWindow));
    UIAccess.current().give(() -> {
      String path = directoryDialog.open();
      if (path != null) {
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(path);
        if (vf != null) {
          result.complete(new VirtualFile[]{vf});
        }
        else {
          result.completeExceptionally(new CancellationException());
        }
      }
      else {
        result.completeExceptionally(new CancellationException());
      }
    }); return result;
  }
}
