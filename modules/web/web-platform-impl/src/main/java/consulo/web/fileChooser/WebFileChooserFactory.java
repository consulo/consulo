/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.fileChooser;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileChooser.FileTextField;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class WebFileChooserFactory extends FileChooserFactory {
  @Nonnull
  @Override
  public FileChooserDialog createFileChooser(@Nonnull FileChooserDescriptor descriptor, @javax.annotation.Nullable Project project, @javax.annotation.Nullable Component parent) {
    return null;
  }

  @Nonnull
  @Override
  public PathChooserDialog createPathChooser(@Nonnull FileChooserDescriptor descriptor, @javax.annotation.Nullable Project project, @javax.annotation.Nullable Component parent) {
    return new WebPathChooserDialog(project, descriptor);
  }

  @Nonnull
  @Override
  public FileSaverDialog createSaveFileDialog(@Nonnull FileSaverDescriptor descriptor, @javax.annotation.Nullable Project project) {
    return null;
  }

  @Nonnull
  @Override
  public FileSaverDialog createSaveFileDialog(@Nonnull FileSaverDescriptor descriptor, @Nonnull Component parent) {
    return null;
  }

  @Nonnull
  @Override
  public FileTextField createFileTextField(@Nonnull FileChooserDescriptor descriptor, boolean showHidden, @javax.annotation.Nullable Disposable parent) {
    return null;
  }

  @Override
  public void installFileCompletion(@Nonnull JTextField field, @Nonnull FileChooserDescriptor descriptor, boolean showHidden, @javax.annotation.Nullable Disposable parent) {

  }
}
