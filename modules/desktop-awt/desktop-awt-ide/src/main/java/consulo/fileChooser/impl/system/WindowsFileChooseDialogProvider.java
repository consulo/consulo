/*
 * Copyright 2013-2018 consulo.io
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
package consulo.fileChooser.impl.system;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.project.Project;
import com.intellij.ui.win.WinPathChooserDialog;
import consulo.fileChooser.impl.system.windows2.IFileDialogEarlyAccessProgramDescriptor;
import consulo.fileChooser.impl.system.windows2.WinPathChooserDialog2;
import consulo.ide.eap.EarlyAccessProgramManager;
import consulo.platform.Platform;
import consulo.ui.fileOperateDialog.FileChooseDialogProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2018-06-28
 */
public class WindowsFileChooseDialogProvider implements FileChooseDialogProvider {
  @Nonnull
  @Override
  public String getId() {
    return "windows-native";
  }

  @Nonnull
  @Override
  public String getName() {
    return "system";
  }

  @Override
  public boolean isAvailable() {
    return Platform.current().os().isWindows();
  }

  @Nonnull
  @Override
  public FileChooserDialog createFileChooser(@Nonnull FileChooserDescriptor descriptor, @Nullable Project project, @Nullable Component parent) {
    if(EarlyAccessProgramManager.is(IFileDialogEarlyAccessProgramDescriptor.class)) {
      return new WinPathChooserDialog2(descriptor, parent, project);
    }
    return new WinPathChooserDialog(descriptor, parent, project);
  }

  @Nonnull
  @Override
  public PathChooserDialog createPathChooser(@Nonnull FileChooserDescriptor descriptor, @Nullable Project project, @Nullable Component parent) {
    return new WinPathChooserDialog(descriptor, parent, project);
  }
}
