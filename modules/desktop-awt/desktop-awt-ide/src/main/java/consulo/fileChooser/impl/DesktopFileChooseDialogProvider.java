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
package consulo.fileChooser.impl;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.fileChooser.ex.FileChooserDialogImpl;
import com.intellij.openapi.project.Project;
import consulo.ui.fileOperateDialog.FileChooseDialogProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2018-06-28
 */
public class DesktopFileChooseDialogProvider implements FileChooseDialogProvider {
  @Nonnull
  @Override
  public String getId() {
    return APPLICATION_ID;
  }

  @Nonnull
  @Override
  public String getName() {
    return "application";
  }

  @Nonnull
  @Override
  public FileChooserDialog createFileChooser(@Nonnull FileChooserDescriptor descriptor, @Nullable Project project, @Nullable Component parent) {
    if (parent != null) {
      return new FileChooserDialogImpl(descriptor, parent, project);
    }
    else {
      return new FileChooserDialogImpl(descriptor, project);
    }
  }

  @Nonnull
  @Override
  public PathChooserDialog createPathChooser(@Nonnull FileChooserDescriptor descriptor, @Nullable Project project, @Nullable Component parent) {
    if (parent != null) {
      return new FileChooserDialogImpl(descriptor, parent, project);
    }
    else {
      return new FileChooserDialogImpl(descriptor, project);
    }
  }
}
